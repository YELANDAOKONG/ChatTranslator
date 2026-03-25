package xyz.dkos.gaming.mindustry.translator.core;

import arc.func.Cons;

/**
 * Common interface for all translation engines.
 */
public interface ITranslator {
    /**
     * Translates text to the target language.
     *
     * @param text       Text to translate
     * @param targetLang Target language code
     * @param onSuccess  Callback with translated result
     * @param onFailure  Callback with error if translation fails
     */
    void translate(String text, String targetLang, Cons<String> onSuccess, Cons<Throwable> onFailure);
}