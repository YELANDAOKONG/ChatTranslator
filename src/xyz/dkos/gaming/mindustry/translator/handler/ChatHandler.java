package xyz.dkos.gaming.mindustry.translator.handler;

import arc.Core;
import mindustry.Vars;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.gen.Player;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.service.TranslationService;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

/**
 * Handles incoming chat messages and determines if translation is needed.
 */
public class ChatHandler {

    private final TranslationService translationService;

    public ChatHandler(TranslationService translationService) {
        this.translationService = translationService;
    }

    /**
     * Processes a chat event and translates if necessary.
     *
     * @param event The player chat event
     */
    public void handleChatEvent(PlayerChatEvent event) {
        logEventDetails(event);

        if (!shouldTranslate(event)) {
            return;
        }

        String senderName = formatSenderName(event.player);
        translateAndDisplay(event.message, senderName);
    }

    private boolean shouldTranslate(PlayerChatEvent event) {
        if (!TranslatorConfig.isEnabled()) {
            DebugLogger.log("Translation skipped - Disabled");
            return false;
        }

        if (event.message == null || event.message.trim().isEmpty()) {
            DebugLogger.log("Translation skipped - Empty message");
            return false;
        }

        boolean isServerMessage = (event.player == null);
        boolean isOwnMessage = !isServerMessage && (event.player == Vars.player);

        if (isOwnMessage) {
            DebugLogger.log("Translation skipped - Own message");
            return false;
        }

        if (isServerMessage && !TranslatorConfig.isTranslateServerEnabled()) {
            DebugLogger.log("Translation skipped - Server message and translate server disabled");
            return false;
        }

        return true;
    }

    private String formatSenderName(Player player) {
        if (player == null) {
            return "[Server]";
        }

        return TranslatorConfig.isPreserveColor()
                ? player.coloredName()
                : player.name;
    }

    private void translateAndDisplay(String message, String senderName) {
        DebugLogger.log("Translating message: " + message);

        translationService.translate(
                message,
                translated -> displayTranslation(translated, message, senderName),
                error -> DebugLogger.log("Translation failed: " + error.getMessage())
        );
    }

    private void displayTranslation(String translated, String original, String senderName) {
        if (translated.equalsIgnoreCase(original.trim())) {
            DebugLogger.log("Translation skipped - Same as original");
            return;
        }

        if (Vars.ui == null || Vars.ui.chatfrag == null) {
            return;
        }

        String displayMessage = formatDisplayMessage(translated, senderName);
        Core.app.post(() -> Vars.ui.chatfrag.addMessage(displayMessage));

        DebugLogger.log("Translation displayed: " + translated);
    }

    private String formatDisplayMessage(String translated, String senderName) {
        if (TranslatorConfig.isHideOriginal()) {
            return senderName + "[white]: " + translated;
        } else {
            return "[lightgray][TR] " + senderName + "[white]: " + translated;
        }
    }

    private void logEventDetails(PlayerChatEvent event) {
        if (!TranslatorConfig.isDebugMode()) {
            return;
        }

        DebugLogger.log("=== Chat Event Details ===");
        DebugLogger.log("Player object: " + event.player);
        DebugLogger.log("Player null?: " + (event.player == null));

        if (event.player != null) {
            DebugLogger.log("Player name: " + event.player.name);
            DebugLogger.log("Player coloredName: " + event.player.coloredName());
            DebugLogger.log("Player isLocal: " + event.player.isLocal());
            DebugLogger.log("Player isAdmin: " + event.player.admin);
            DebugLogger.log("Player team: " + event.player.team());
        }

        DebugLogger.log("Message: " + event.message);
        DebugLogger.log("=========================");
    }
}