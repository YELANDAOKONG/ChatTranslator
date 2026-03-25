package xyz.dkos.gaming.mindustry.translator.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Dialog;
import arc.scene.ui.TextArea;
import arc.scene.ui.TextField;
import mindustry.Vars;
import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.service.TranslationService;
import xyz.dkos.gaming.mindustry.translator.utils.BundleHelper;

/**
 * Dialog enabling manual translation workflow directly from the chat box UI.
 */
public class OutgoingTranslatorDialog extends Dialog {

    private final TranslationService service;
    private final TextField chatField;
    private TextArea inputArea;

    public OutgoingTranslatorDialog(TranslationService service, TextField chatField) {
        super(BundleHelper.get("translator.ui.title"));
        this.service = service;
        this.chatField = chatField;

        closeOnBack(); // Allows closing via ESC key
        setup();
        addCloseButton(); // Adds standard Close button to the bottom 'buttons' table
    }

    private void setup() {
        cont.clear();

        // Show current target language info
        cont.add(BundleHelper.get("translator.settings.target-lang") + " [accent]" + TranslatorConfig.getTargetLanguageName() + "[]").left().row();

        cont.check(BundleHelper.get("translator.settings.auto-translate"),
                TranslatorConfig.isAutoTranslate(),
                TranslatorConfig::setAutoTranslate).left().row();

        cont.image().color(Color.gray).fillX().height(3f).pad(10f, 0, 10f, 0).row();

        cont.add(BundleHelper.get("translator.ui.manual-input")).left().row();

        inputArea = cont.area("", text -> {}).width(400f).height(120f).get();
        cont.row();

        cont.button(BundleHelper.get("translator.ui.translate-insert"), this::doManualTranslate)
                .width(400f).padTop(10f).row();
    }

    @Override
    public Dialog show() {
        Dialog result = super.show();
        // Request focus so players can type immediately upon opening
        Core.scene.setKeyboardFocus(inputArea);
        return result;
    }

    private void doManualTranslate() {
        String text = inputArea.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Vars.ui.loadfrag.show(BundleHelper.get("translator.message.translating"));

        service.translate(
                text,
                TranslatorConfig.getTargetLanguage(),
                translated -> {
                    Vars.ui.loadfrag.hide();
                    chatField.setText(translated);
                    hide();

                    // Re-focus original chat box
                    Core.app.post(() -> {
                        Core.scene.setKeyboardFocus(chatField);
                        chatField.setCursorPosition(translated.length());
                    });
                },
                error -> {
                    Vars.ui.loadfrag.hide();
                    Vars.ui.showErrorMessage(BundleHelper.get("translator.message.translation-error", error.getMessage()));
                }
        );
    }
}