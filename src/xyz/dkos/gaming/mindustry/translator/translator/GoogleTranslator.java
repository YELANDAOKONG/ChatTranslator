package xyz.dkos.gaming.mindustry.translator.translator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Jval;

import xyz.dkos.gaming.mindustry.translator.core.ITranslator;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

public class GoogleTranslator implements ITranslator {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final String API_URL = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=%s&dt=t&q=%s";

    @Override
    public void translate(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        Thread thread = new Thread(() -> executeTranslation(text, targetLang, onSuccess, onFailure));
        thread.setDaemon(true);
        thread.start();
    }

    private void executeTranslation(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            String urlString = String.format(API_URL, targetLang, encodedText);

            DebugLogger.log("Google Translate Request URL: " + urlString);

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            DebugLogger.log("Google Translate Response Status: " + status);

            if (status != 200) {
                Core.app.post(() -> onFailure.get(
                        new RuntimeException("Google API returned status: " + status)
                ));
                return;
            }

            String response = readResponse(conn);
            DebugLogger.log("Google Translate Raw Response: " + response);

            String result = parseResponse(response);
            Core.app.post(() -> onSuccess.get(result));

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
        Jval.JsonArray rootArray = json.asArray();

        if (rootArray.size > 0 && rootArray.get(0).isArray()) {
            StringBuilder result = new StringBuilder();
            Jval sentences = rootArray.get(0);

            for (Jval sentence : sentences.asArray()) {
                if (sentence.isArray() && sentence.asArray().size > 0) {
                    result.append(sentence.asArray().get(0).asString());
                }
            }
            return result.toString();
        }

        throw new RuntimeException("Invalid translation response structure");
    }
}