package xyz.dkos.gaming.mindustry.translator.translator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Jval;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.core.ITranslator;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

/**
 * OpenAI GPT-based translator using the Chat Completions API.
 * Supports custom prompts, temperature control, and multiple models.
 */
public class OpenAITranslator implements ITranslator {
    private static final String API_PATH = "/chat/completions";

    @Override
    public void translate(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        String endpoint = TranslatorConfig.getOpenAIEndpoint();
        String model = TranslatorConfig.getOpenAIModel();
        String apiKey = TranslatorConfig.getOpenAIKey();
        double temperature = TranslatorConfig.getOpenAITemperature();
        String promptTemplate = TranslatorConfig.getOpenAIPrompt();

        translate(text, targetLang, endpoint, model, apiKey, temperature, promptTemplate, onSuccess, onFailure);
    }

    /**
     * Translates text using OpenAI API with custom configuration.
     * This method is public to support testing from the settings UI.
     *
     * @param text           Text to translate
     * @param targetLang     Target language
     * @param endpoint       OpenAI API endpoint
     * @param model          Model name (e.g., "gpt-3.5-turbo")
     * @param apiKey         OpenAI API key
     * @param temperature    Temperature for generation (0.0 - 2.0)
     * @param promptTemplate Custom prompt template
     * @param onSuccess      Success callback
     * @param onFailure      Failure callback
     */
    public void translate(String text, String targetLang,
                          String endpoint, String model, String apiKey,
                          double temperature, String promptTemplate,
                          Cons<String> onSuccess, Cons<Throwable> onFailure) {

        Thread thread = new Thread(() ->
                executeTranslation(text, targetLang, endpoint, model, apiKey,
                        temperature, promptTemplate, onSuccess, onFailure)
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void executeTranslation(String text, String targetLang,
                                    String endpoint, String model, String apiKey,
                                    double temperature, String promptTemplate,
                                    Cons<String> onSuccess, Cons<Throwable> onFailure) {
        try {
            String apiUrl = buildApiUrl(endpoint);
            String finalPrompt = buildPrompt(promptTemplate, targetLang, text);
            String requestBody = buildRequestBody(model, temperature, finalPrompt);

            DebugLogger.log("OpenAI Request URL: " + apiUrl);
            DebugLogger.log("OpenAI Request Payload: " + requestBody);

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            configureConnection(conn, apiKey);

            sendRequest(conn, requestBody);

            int status = conn.getResponseCode();
            DebugLogger.log("OpenAI Response Status: " + status);

            if (status != 200) {
                handleErrorResponse(conn, status, onFailure);
                return;
            }

            String response = readResponse(conn);
            DebugLogger.log("OpenAI Raw Response: " + response);

            String result = parseResponse(response);
            Core.app.post(() -> onSuccess.get(result));

        } catch (Exception e) {
            Core.app.post(() -> onFailure.get(e));
        }
    }

    private String buildApiUrl(String endpoint) {
        String apiUrl = endpoint.trim();
        if (!apiUrl.endsWith(API_PATH)) {
            apiUrl = apiUrl.endsWith("/")
                    ? apiUrl + "chat/completions"
                    : apiUrl + "/chat/completions";
        }
        return apiUrl;
    }

    private String buildPrompt(String template, String targetLang, String text) {
        return template
                .replace("{{target_language}}", targetLang)
                .replace("{{text}}", text);
    }

    private String buildRequestBody(String model, double temperature, String prompt) {
        Jval requestBody = Jval.newObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);

        Jval messageArray = Jval.newArray();
        Jval messageObj = Jval.newObject();
        messageObj.put("role", "user");
        messageObj.put("content", prompt);
        messageArray.add(messageObj);

        requestBody.put("messages", messageArray);
        return requestBody.toString(Jval.Jformat.plain);
    }

    private void configureConnection(HttpURLConnection conn, String apiKey) throws Exception {
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", TranslatorConfig.getUserAgent());
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);
    }

    private void sendRequest(HttpURLConnection conn, String requestBody) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }

    private void handleErrorResponse(HttpURLConnection conn, int status, Cons<Throwable> onFailure) {
        try (var errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder errResponse = new StringBuilder();
            String line;
            while ((line = errReader.readLine()) != null) {
                errResponse.append(line);
            }

            DebugLogger.log("OpenAI Error Response: " + errResponse);
            Core.app.post(() -> onFailure.get(
                    new RuntimeException("OpenAI API returned status: " + status + " Details: " + errResponse)
            ));
        } catch (Exception e) {
            Core.app.post(() -> onFailure.get(e));
        }
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private String parseResponse(String response) throws Exception {
        Jval json = Jval.read(response);

        if (json.has("choices") && json.get("choices").isArray()) {
            Jval.JsonArray choices = json.get("choices").asArray();
            if (choices.size > 0) {
                Jval firstChoice = choices.get(0);
                if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                    return firstChoice.get("message").getString("content", "").trim();
                }
            }
        }

        throw new RuntimeException("Invalid OpenAI response structure");
    }
}