package xyz.dkos.gaming.mindustry.translator;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.mod.Mod;
import xyz.dkos.gaming.mindustry.translator.utils.BingTranslator;

public class ModMain extends Mod {

    public ModMain() {
        Log.info("Chat Translator Loaded.");
    }

    @Override
    public void init() {
        if (Vars.headless) {
            return;
        }

        Log.info("Chat Translator Initialized.");

        Events.on(PlayerChatEvent.class, event -> {
            // Ignore messages from ourselves or headless server inputs
            if (event.player == null || event.player == Vars.player) {
                return;
            }

            if (event.message == null || event.message.trim().isEmpty()) {
                return;
            }

            String targetLang = getClientLanguage();
            if (targetLang == null || targetLang.isEmpty()) {
                return;
            }

            BingTranslator.translate(
                    event.message,
                    targetLang,
                    translated -> {
                        // Avoid redundant chat spam if the language didn't change
                        if (!translated.equalsIgnoreCase(event.message.trim())) {
                            if (Vars.ui != null && Vars.ui.chatfrag != null) {
                                // [lightgray][TR] formatting distinguishes translations cleanly
                                Vars.ui.chatfrag.addMessage("[lightgray][TR] " + event.player.name + "[white]: " + translated);
                            }
                        }
                    },
                    error -> {
                        Log.err("Chat Translator: Failed to process translation.", error);
                    }
            );
        });
    }

    /**
     * Resolves the current client language code for the Translation API.
     */
    private String getClientLanguage() {
        String locale = Core.settings.getString("locale");

        // Handle unconfigured defaults
        if (locale == null || locale.isEmpty() || locale.equals("default")) {
            if (Core.bundle != null && Core.bundle.getLocale() != null) {
                return Core.bundle.getLocale().getLanguage();
            }
            return "en";
        }

        // Ensure accurate handling of Traditional and Simplified Chinese
        // that Microsoft Bing API requires ("zh-Hans" vs "zh-Hant")
        if (locale.contains("_")) {
            String[] parts = locale.split("_");
            if (parts[0].equalsIgnoreCase("zh")) {
                return locale.equalsIgnoreCase("zh_TW") ? "zh-Hant" : "zh-Hans";
            }
            return parts[0];
        }

        return locale;
    }
}