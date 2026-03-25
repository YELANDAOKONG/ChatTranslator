package xyz.dkos.gaming.mindustry.translator.service;

import arc.Core;
import arc.func.Cons;
import arc.util.Log;
import mindustry.Vars;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.core.ITranslator;
import xyz.dkos.gaming.mindustry.translator.translator.BingTranslator;
import xyz.dkos.gaming.mindustry.translator.translator.GoogleTranslator;
import xyz.dkos.gaming.mindustry.translator.translator.OpenAITranslator;
import xyz.dkos.gaming.mindustry.translator.utils.BundleHelper;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

/**
 * Service that coordinates translation requests across different engines.
 */
public class TranslationService {

    private final GoogleTranslator googleTranslator = new GoogleTranslator();
    private final BingTranslator bingTranslator = new BingTranslator();
    private final OpenAITranslator openAITranslator = new OpenAITranslator();

    /**
     * Translates text using the configured translation engine.
     *
     * @param text      Text to translate
     * @param onSuccess Callback with translated text
     * @param onFailure Callback with error if translation fails
     */
    public void translate(String text, Cons<String> onSuccess, Cons<Throwable> onFailure) {
        String engine = TranslatorConfig.getEngine();
        String targetLang = TranslatorConfig.getClientLanguage(engine);

        if (targetLang == null || targetLang.isEmpty()) {
            DebugLogger.log("Translation skipped - Invalid target language");
            return;
        }

        ITranslator translator = getTranslator(engine);
        translator.translate(text, targetLang, onSuccess, error -> {
            Log.err("Translation failed using engine: @", engine, error);
            handleTranslationError(error);
            onFailure.get(error);
        });
    }

    /**
     * Tests the OpenAI configuration with a sample text.
     *
     * @param onSuccess Callback with test result
     * @param onFailure Callback with error if test fails
     */
    public void testOpenAI(Cons<String> onSuccess, Cons<Throwable> onFailure) {
        String key = TranslatorConfig.getOpenAIKey();

        if (key.trim().isEmpty()) {
            Core.app.post(() -> onFailure.get(
                    new IllegalArgumentException(BundleHelper.get("translator.message.openai-key-missing"))
            ));
            return;
        }

        String targetLang = TranslatorConfig.getClientLanguage("openai");
        openAITranslator.translate("Hello, this is a test.", targetLang, onSuccess, onFailure);
    }

    private ITranslator getTranslator(String engine) {
        return switch (engine.toLowerCase()) {
            case "google" -> googleTranslator;
            case "bing" -> bingTranslator;
            case "openai" -> openAITranslator;
            default -> bingTranslator;
        };
    }

    private void handleTranslationError(Throwable error) {
        if (Vars.ui != null && Vars.ui.chatfrag != null) {
            String errorMessage = BundleHelper.get("translator.message.translation-error", error.getMessage());
            Vars.ui.chatfrag.addMessage(errorMessage);
        }
    }
}