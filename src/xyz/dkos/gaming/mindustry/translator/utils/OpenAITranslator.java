package xyz.dkos.gaming.mindustry.translator.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Jval;

import xyz.dkos.gaming.mindustry.translator.ModMain;

public class OpenAITranslator {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public static void translate(String text, String toLang,
                                 String endpoint, String model, String apiKey,
                                 double temperature, String promptTemplate,
                                 Cons<String> onSuccess, Cons<Throwable> onFailure) {

        Thread thread = new Thread(() -> {
            try {
                String apiUrl = endpoint.trim();
                if (!apiUrl.endsWith("/chat/completions")) {
                    apiUrl = apiUrl.endsWith("/") ? apiUrl + "chat/completions" : apiUrl + "/chat/completions";
                }

                String finalPrompt = promptTemplate
                        .replace("{{target_language}}", toLang)
                        .replace("{{text}}", text);

                Jval requestBody = Jval.newObject();
                requestBody.put("model", model);
                requestBody.put("temperature", temperature);

                Jval messageArray = Jval.newArray();
                Jval messageObj = Jval.newObject();
                messageObj.put("role", "user");
                messageObj.put("content", finalPrompt);
                messageArray.add(messageObj);

                requestBody.put("messages", messageArray);

                String bodyString = requestBody.toString(Jval.Jformat.plain);

                ModMain.debugLog("OpenAI Request URL: " + apiUrl);
                ModMain.debugLog("OpenAI Request Payload: " + bodyString);

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = bodyString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                ModMain.debugLog("OpenAI Response Status: " + status);

                if (status != 200) {
                    try (var errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errResponse = new StringBuilder();
                        String line;
                        while ((line = errReader.readLine()) != null) errResponse.append(line);

                        ModMain.debugLog("OpenAI Error Response: " + errResponse);
                        Core.app.post(() -> onFailure.get(new RuntimeException("OpenAI API returned status: " + status + " Details: " + errResponse)));
                    }
                    return;
                }

                try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    ModMain.debugLog("OpenAI Raw Response: " + response);

                    Jval json = Jval.read(response.toString());
                    if (json.has("choices") && json.get("choices").isArray() && json.get("choices").asArray().size > 0) {
                        Jval firstChoice = json.get("choices").asArray().get(0);
                        if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                            String result = firstChoice.get("message").getString("content", "").trim();
                            Core.app.post(() -> onSuccess.get(result));
                            return;
                        }
                    }
                    Core.app.post(() -> onFailure.get(new RuntimeException("Invalid OpenAI response structure.")));
                }
            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}