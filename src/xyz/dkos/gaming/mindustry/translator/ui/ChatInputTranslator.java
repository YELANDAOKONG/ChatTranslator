package xyz.dkos.gaming.mindustry.translator.ui;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Reflect;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Call;
import mindustry.gen.Icon;
import mindustry.ui.Styles;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.service.TranslationService;
import xyz.dkos.gaming.mindustry.translator.utils.DebugLogger;

/**
 * Handles injection of translation features into the client's outgoing chat system.
 */
public class ChatInputTranslator {

    private final TranslationService translationService;
    private TextField chatField;
    private Table uiContainer;

    public ChatInputTranslator(TranslationService translationService) {
        this.translationService = translationService;
        Events.on(ClientLoadEvent.class, e -> init());
    }

    private void init() {
        findChatField();
        if (chatField == null) {
            DebugLogger.log("Could not find chat text field.");
            return;
        }

        setupInterceptor();
        setupUI();
    }

    private void findChatField() {
        if (Vars.ui == null || Vars.ui.chatfrag == null) {
            return;
        }
        try {
            // Use Arc's Reflect utility to directly fetch the private 'chatfield' variable
            chatField = Reflect.get(Vars.ui.chatfrag, "chatfield");
        } catch (Exception e) {
            DebugLogger.log("Failed to get chat field via reflection: " + e.getMessage());
        }
    }

    private void setupInterceptor() {
        // Intercepts enter key to provide auto-translation
        chatField.getListeners().insert(0, new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                if (keycode == KeyCode.enter) {
                    String text = chatField.getText();

                    // Ignore empty inputs or commands
                    if (text == null || text.trim().isEmpty() || text.startsWith("/")) {
                        return false;
                    }

                    if (TranslatorConfig.isAutoTranslate()) {
                        event.cancel(); // Prevent default send

                        chatField.setText("");
                        Core.scene.setKeyboardFocus(null); // Deselect to close typing UI

                        doAutoTranslate(text);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void doAutoTranslate(String originalText) {
        String targetLang = TranslatorConfig.getTargetLanguage();

        translationService.translate(
                originalText,
                targetLang,
                translated -> {
                    String prefix = TranslatorConfig.isAddPrefix() ? TranslatorConfig.getPrefixContent() : "";
                    Call.sendChatMessage(prefix + translated);
                },
                error -> {
                    if (Vars.ui != null && Vars.ui.chatfrag != null) {
                        Vars.ui.chatfrag.addMessage("[crimson][TR] Failed to auto-translate outgoing message: " + error.getMessage());
                    }
                }
        );
    }

    private void setupUI() {
        uiContainer = new Table();
        uiContainer.name = "chat-translator-input-ui";
        Core.scene.add(uiContainer);

        uiContainer.button(Icon.book, Styles.flati, () -> {
            new OutgoingTranslatorDialog(translationService, chatField).show();
        }).size(36f);

        // Keep the UI strictly aligned with the chat text box when it is active
        Events.run(Trigger.update, () -> {
            if (chatField == null || !TranslatorConfig.isShowUI()) {
                uiContainer.visible = false;
                return;
            }

            // The UI should only show when the player is actively typing in the chat field
            boolean chatVisible = chatField.parent != null && chatField.parent.visible && Core.scene.getKeyboardFocus() == chatField;
            uiContainer.visible = chatVisible;

            if (chatVisible) {
                Vec2 pos = chatField.localToStageCoordinates(Tmp.v1.set(chatField.getWidth(), chatField.getHeight()));
                // Positions exactly aligned with the right side of the chat field, just slightly above it
                uiContainer.setPosition(pos.x - uiContainer.getPrefWidth(), pos.y + 2f);
            }
        });
    }
}