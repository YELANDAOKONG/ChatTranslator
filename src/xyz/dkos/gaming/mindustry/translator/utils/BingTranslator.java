package xyz.dkos.gaming.mindustry.translator.utils;

import arc.Core;
import arc.func.Cons;
import arc.util.async.Threads;
import arc.util.serialization.Jval;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BingTranslator {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";

    private static String token = null;
    private static long tokenExpiration = 0;

    /**
     * Translates a given text using Microsoft's Edge Translation API.
     */
    public static void translate(String text, String toLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        // Fetch new token if the current one has expired (validity is roughly 10 minutes)
        if (System.currentTimeMillis() > tokenExpiration || token == null) {
            fetchToken(
                    () -> doTranslate(text, toLang, onSuccess, onFailure),
                    onFailure
            );
        } else {
            doTranslate(text, toLang, onSuccess, onFailure);
        }
    }

    private static void fetchToken(Runnable onSuccess, Cons<Throwable> onFailure) {
        Threads.daemon(() -> {
            try {
                URL url = new URL("https://edge.microsoft.com/translate/auth");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        token = response.toString();
                        // Expire slightly before the true expiration time (9 minutes)
                        tokenExpiration = System.currentTimeMillis() + 9 * 60 * 1000;

                        Core.app.post(onSuccess);
                    }
                } else {
                    Core.app.post(() -> onFailure.get(new RuntimeException("Failed to fetch translation token. Status: " + status)));
                }
            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });
    }

    private static void doTranslate(String text, String toLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        Threads.daemon(() -> {
            try {
                String urlString = "https://api-edge.cognitive.microsofttranslator.com/translate?from=&to=" + toLang + "&api-version=3.0&includeSentenceLength=true";

                Jval.Jarray bodyArray = Jval.newArray();
                Jval textObj = Jval.newObject();
                textObj.put("Text", text);
                bodyArray.add(textObj);

                String bodyString = bodyArray.toString(Jval.Jformat.plain);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = bodyString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int status = conn.getResponseCode();
                if (status == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        Jval json = Jval.read(response.toString());
                        if (json.isArray() && json.asArray().size > 0) {
                            Jval first = json.asArray().get(0);
                            if (first.has("translations")) {
                                Jval translations = first.get("translations");
                                if (translations.isArray() && translations.asArray().size > 0) {
                                    String result = translations.asArray().get(0).getString("text", "");
                                    Core.app.post(() -> onSuccess.get(result));
                                    return;
                                }
                            }
                        }
                        Core.app.post(() -> onFailure.get(new RuntimeException("Invalid translation response structure.")));
                    }
                } else {
                    Core.app.post(() -> onFailure.get(new RuntimeException("Translation API returned an error. Status: " + status)));
                }
            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });
    }
}