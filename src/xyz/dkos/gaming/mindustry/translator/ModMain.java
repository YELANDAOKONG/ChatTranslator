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
    private static final String PREF_ENGINE = "chat-translator-engine";

    // Array of supported engines to cycle through
    private static final String[] ENGINES = { "Google", "Bing" };

    public ModMain() {
        Log.info("Chat Translator Loaded.");
    }

    @Override
    public void init() {
        if (Vars.headless) {
            return;
        }

        buildSettingsUI();
        registerChatListener();

        Log.info("Chat Translator Initialized.");
    }

    private void buildSettingsUI() {
        if (Vars.ui == null || Vars.ui.settings == null) {
            return;
        }

        Vars.ui.settings.addCategory("Translator", "chat", table -> {
            table.check("Enable Chat Translator", Core.settings.getBool(PREF_ENABLED, true),
                    b -> Core.settings.put(PREF_ENABLED, b)).left().row();

            // Mindustry alternative to Dropdown: A cycle button
            table.table(t -> {
                t.add("Translation Engine: ").left().padRight(15f);

                t.button(b -> {
                    // Dynamically update the label text based on current settings
                    b.label(() -> Core.settings.getString(PREF_ENGINE, "Bing"));
                }, () -> {
                    // Cycle to the next engine on click
                    String current = Core.settings.getString(PREF_ENGINE, "Bing");
                    int currentIndex = 0;

                    for (int i = 0; i < ENGINES.length; i++) {
                        if (ENGINES[i].equalsIgnoreCase(current)) {
                            currentIndex = i;
                            break;
                        }
                    }

                    int nextIndex = (currentIndex + 1) % ENGINES.length;
                    Core.settings.put(PREF_ENGINE, ENGINES[nextIndex]);
                }).size(120f, 40f);
            }).left().padTop(5f).row();
        });
    }

    private void registerChatListener() {
        Events.on(PlayerChatEvent.class, event -> {
            if (!Core.settings.getBool(PREF_ENABLED, true)) {
                return;
            }

            if (event.player == null || event.player == Vars.player) {
                return;
            }

            if (event.message == null || event.message.trim().isEmpty()) {
                return;
            }

            String engine = Core.settings.getString(PREF_ENGINE, "Bing");
            String targetLang = getClientLanguage(engine);

            if (targetLang == null || targetLang.isEmpty()) {
                return;
            }

            arc.func.Cons<String> onSuccess = translated -> {
                if (!translated.equalsIgnoreCase(event.message.trim()) && Vars.ui != null && Vars.ui.chatfrag != null) {
                    Vars.ui.chatfrag.addMessage("[lightgray][TR] " + event.player.name + "[white]: " + translated);
                }
            };

            arc.func.Cons<Throwable> onFailure = error -> {
                Log.err("Chat Translator: Failed to process translation.", error);
            };

            if ("Google".equalsIgnoreCase(engine)) {
                GoogleTranslator.translate(event.message, targetLang, onSuccess, onFailure);
            } else {
                BingTranslator.translate(event.message, targetLang, onSuccess, onFailure);
            }
        });
    }

    /**
     * Resolves the correct language code for the selected API.
     * Ensures support for ALL Mindustry locales (e.g., pt_BR, ru, es, zh_CN).
     */
    private String getClientLanguage(String engine) {
        String locale = Core.settings.getString("locale", "default");

        if ("default".equals(locale)) {
            if (Core.bundle != null && Core.bundle.getLocale() != null) {
                locale = Core.bundle.getLocale().toString();
            } else {
                return "en";
            }
        }

        // Convert Mindustry locale format (e.g., pt_BR) to web standard format (pt-BR)
        locale = locale.replace('_', '-');

        // Handle specific Chinese character sets required by different APIs
        if (locale.toLowerCase().startsWith("zh")) {
            boolean isTraditional = locale.equalsIgnoreCase("zh-TW") || locale.equalsIgnoreCase("zh-HK");

            if ("Google".equalsIgnoreCase(engine)) {
                return isTraditional ? "zh-TW" : "zh-CN";
            } else {
                return isTraditional ? "zh-Hant" : "zh-Hans";
            }
        }

        return locale;
    }
}