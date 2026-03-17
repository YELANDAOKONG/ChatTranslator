package xyz.dkos.gaming.mindustry.translator;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.mod.Mod;
import xyz.dkos.gaming.mindustry.translator.utils.BingTranslator;
import xyz.dkos.gaming.mindustry.translator.utils.GoogleTranslator;

public class ModMain extends Mod {

    private static final String PREF_ENABLED = "chat-translator-enabled";
    private static final String PREF_USE_GOOGLE = "chat-translator-use-google";

    public ModMain() {
        Log.info("Chat Translator Loaded.");
    }

    @Override
    public void init() {
        // Prevent initialization on headless servers
        if (Vars.headless) {
            return;
        }

        buildSettingsUI();
        registerChatListener();

        Log.info("Chat Translator Initialized.");
    }

    /**
     * Safely injects our settings into the Mindustry Settings Menu.
     * Uses custom rows to ensure labels render correctly without forcing bundle dependencies.
     */
    private void buildSettingsUI() {
        if (Vars.ui == null || Vars.ui.settings == null) {
            return;
        }

        Vars.ui.settings.addCategory("Translator", "chat", table -> {
            table.check("Enable Chat Translator", Core.settings.getBool(PREF_ENABLED, true),
                    b -> Core.settings.put(PREF_ENABLED, b)).left().row();

            table.check("Use Google Translate (Off = Bing)", Core.settings.getBool(PREF_USE_GOOGLE, false),
                    b -> Core.settings.put(PREF_USE_GOOGLE, b)).left().row();
        });
    }

    private void registerChatListener() {
        Events.on(PlayerChatEvent.class, event -> {
            // Early return if feature is disabled in settings
            if (!Core.settings.getBool(PREF_ENABLED, true)) {
                return;
            }

            // Ignore system messages or messages from ourselves
            if (event.player == null || event.player == Vars.player) {
                return;
            }

            if (event.message == null || event.message.trim().isEmpty()) {
                return;
            }

            boolean useGoogle = Core.settings.getBool(PREF_USE_GOOGLE, false);
            String targetLang = getClientLanguage(useGoogle);

            if (targetLang == null || targetLang.isEmpty()) {
                return;
            }

            // Shared callback logic for both engines
            arc.func.Cons<String> onSuccess = translated -> {
                if (!translated.equalsIgnoreCase(event.message.trim()) && Vars.ui != null && Vars.ui.chatfrag != null) {
                    Vars.ui.chatfrag.addMessage("[lightgray][TR] " + event.player.name + "[white]: " + translated);
                }
            };

            arc.func.Cons<Throwable> onFailure = error -> {
                Log.err("Chat Translator: Failed to process translation.", error);
            };

            // Route to correct translation engine
            if (useGoogle) {
                GoogleTranslator.translate(event.message, targetLang, onSuccess, onFailure);
            } else {
                BingTranslator.translate(event.message, targetLang, onSuccess, onFailure);
            }
        });
    }

    /**
     * Resolves the current client language code.
     * Maps Chinese locales correctly depending on the required format of the targeted API.
     */
    private String getClientLanguage(boolean isGoogleEngine) {
        String locale = Core.settings.getString("locale", "default");

        if (locale.equals("default")) {
            if (Core.bundle != null && Core.bundle.getLocale() != null) {
                locale = Core.bundle.getLocale().toString();
            } else {
                return "en";
            }
        }

        if (locale.contains("_")) {
            String[] parts = locale.split("_");
            if (parts[0].equalsIgnoreCase("zh")) {
                boolean isTraditional = locale.equalsIgnoreCase("zh_TW");

                // Google expects zh-CN / zh-TW, whereas Bing expects zh-Hans / zh-Hant
                if (isGoogleEngine) {
                    return isTraditional ? "zh-TW" : "zh-CN";
                } else {
                    return isTraditional ? "zh-Hant" : "zh-Hans";
                }
            }
            return parts[0];
        }

        return locale;
    }
}