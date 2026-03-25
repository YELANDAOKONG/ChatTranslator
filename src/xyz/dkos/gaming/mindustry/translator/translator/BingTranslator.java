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

import xyz.dkos.gaming.mindustry.translator.core.ITranslator;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

/**
 * Microsoft Bing Translator implementation using the Edge Translate API.
 * Requires periodic token refresh (valid for 9 minutes).
 */
public class BingTranslator implements ITranslator {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final String AUTH_URL = "https://edge.microsoft.com/translate/auth";
    private static final String API_URL = "https://api-edge.cognitive.microsofttranslator.com/translate?from=&to=%s&api-version=3.0&includeSentenceLength=true";
    private static final int TOKEN_LIFETIME_MS = 9 * 60 * 1000; // 9 minutes

    private String token = null;
    private long tokenExpiration = 0;

    @Override
    public void translate(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        if (isTokenExpired()) {
            fetchToken(
                    () -> executeTranslation(text, targetLang, onSuccess, onFailure),
                    onFailure
            );
        } else {
            executeTranslation(text, targetLang, onSuccess, onFailure);
        }
    }

    private boolean isTokenExpired() {
        return System.currentTimeMillis() > tokenExpiration || token == null;
    }

    private void fetchToken(Runnable onSuccess, Cons<Throwable> onFailure) {
        runAsync(() -> {
            try {
                DebugLogger.log("Bing: Fetching translation token...");

                URL url = new URL(AUTH_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status != 200) {
                    DebugLogger.log("Bing Token Fetch Failed. Status: " + status);
                    Core.app.post(() -> onFailure.get(
                            new RuntimeException("Failed to fetch translation token. Status: " + status)
                    ));
                    return;
                }

                String fetchedToken = readResponse(conn);
                updateToken(fetchedToken);

                DebugLogger.log("Bing: Token fetched successfully.");
                Core.app.post(onSuccess);

            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });
    }

    private void updateToken(String newToken) {
        this.token = newToken;
        this.tokenExpiration = System.currentTimeMillis() + TOKEN_LIFETIME_MS;
    }

    private void executeTranslation(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        runAsync(() -> {
            try {
                String urlString = String.format(API_URL, targetLang);
                String requestBody = buildRequestBody(text);

                DebugLogger.log("Bing Translate Request URL: " + urlString);
                DebugLogger.log("Bing Translate Payload: " + requestBody);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                configureConnection(conn);

                sendRequest(conn, requestBody);

                int status = conn.getResponseCode();
                DebugLogger.log("Bing Translate Response Status: " + status);

                if (status != 200) {
                    Core.app.post(() -> onFailure.get(
                            new RuntimeException("Translation API returned an error. Status: " + status)
                    ));
                    return;
                }

                String response = readResponse(conn);
                DebugLogger.log("Bing Translate Raw Response: " + response);

                String result = parseResponse(response);
                Core.app.post(() -> onSuccess.get(result));

            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });
    }

    private String buildRequestBody(String text) {
        Jval bodyArray = Jval.newArray();
        Jval textObj = Jval.newObject();
        textObj.put("Text", text);
        bodyArray.add(textObj);
        return bodyArray.toString(Jval.Jformat.plain);
    }

    private void configureConnection(HttpURLConnection conn) throws Exception {
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
    }

    private void sendRequest(HttpURLConnection conn, String requestBody) throws Exception {
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
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

        if (json.isArray() && json.asArray().size > 0) {
            Jval first = json.asArray().get(0);
            if (first.has("translations")) {
                Jval translations = first.get("translations");
                if (translations.isArray() && translations.asArray().size > 0) {
                    return translations.asArray().get(0).getString("text", "");
                }
            }
        }

        throw new RuntimeException("Invalid translation response structure");
    }

    private void runAsync(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}