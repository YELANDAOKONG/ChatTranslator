package xyz.dkos.gaming.mindustry.translator.utils;

import arc.Core;
import arc.util.Log;
import mindustry.Vars;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;

/**
 * Centralized debug logging utility.
 */
public class DebugLogger {

    /**
     * Logs debug messages to console and optionally to chat.
     * Debug messages are NOT localized.
     *
     * @param message Debug message
     */
    public static void log(String message) {
        if (!TranslatorConfig.isDebugMode()) {
            return;
        }

        Log.info("[TR] (DEBUG) @", message);

        if (TranslatorConfig.isDebugInChat()) {
            Core.app.post(() -> {
                if (Vars.ui != null && Vars.ui.chatfrag != null) {
                    Vars.ui.chatfrag.addMessage("[gray][TR] (DEBUG) " + message);
                }
            });
        }
    }
}