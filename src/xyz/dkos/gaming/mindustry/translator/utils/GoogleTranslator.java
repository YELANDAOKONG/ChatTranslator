package xyz.dkos.gaming.mindustry.translator.utils;

import arc.Core;
import arc.func.Cons;
import arc.util.serialization.Jval;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GoogleTranslator {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * Translates text using the public Google Translate API.
     */
    public static void translate(String text, String toLang, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        Thread thread = new Thread(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
                String urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + toLang + "&dt=t&q=" + encodedText;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int status = conn.getResponseCode();
                if (status != 200) {
                    Core.app.post(() -> onFailure.get(new RuntimeException("Google API returned status: " + status)));
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Google returns a nested array: [[[ "Translated text", "Original text" ], ...]]
                    Jval json = Jval.read(response.toString());
                    Jval.JsonArray rootArray = json.asArray();

                    if (rootArray.size > 0 && rootArray.get(0).isArray()) {
                        StringBuilder result = new StringBuilder();
                        Jval sentences = rootArray.get(0);

                        for (Jval sentence : sentences.asArray()) {
                            if (sentence.isArray() && sentence.asArray().size > 0) {
                                result.append(sentence.asArray().get(0).asString());
                            }
                        }

                        Core.app.post(() -> onSuccess.get(result.toString()));
                        return;
                    }

                    Core.app.post(() -> onFailure.get(new RuntimeException("Invalid translation response structure.")));
                }
            } catch (Exception e) {
                Core.app.post(() -> onFailure.get(e));
            }
        });

        thread.setDaemon(true);
        thread.start();
    }
}