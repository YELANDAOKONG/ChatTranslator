package xyz.dkos.gaming.mindustry.translator;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.mod.Mod;

import xyz.dkos.gaming.mindustry.translator.handler.ChatHandler;
import xyz.dkos.gaming.mindustry.translator.service.TranslationService;
import xyz.dkos.gaming.mindustry.translator.ui.SettingsUI;

/**
 * Main entry point for the Chat Translator mod.
 */
public class ModMain extends Mod {

    private TranslationService translationService;
    private ChatHandler chatHandler;
    private SettingsUI settingsUI;

    public ModMain() {
        Log.info("Chat Translator Loaded.");
    }

    @Override
    public void init() {
        if (Vars.headless) {
            Log.info("Chat Translator: Headless mode detected, skipping initialization.");
            return;
        }

        initializeServices();
        registerEventListeners();
        buildUI();

        Log.info("Chat Translator Initialized.");
    }

    private void initializeServices() {
        translationService = new TranslationService();
        chatHandler = new ChatHandler(translationService);
        settingsUI = new SettingsUI(translationService);
    }

    private void registerEventListeners() {
        Events.on(PlayerChatEvent.class, chatHandler::handleChatEvent);
    }

    private void buildUI() {
        settingsUI.build();
    }
}