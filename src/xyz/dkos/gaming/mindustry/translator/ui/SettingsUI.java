package xyz.dkos.gaming.mindustry.translator.ui;

import arc.scene.ui.*;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;

import xyz.dkos.gaming.mindustry.translator.config.TranslatorConfig;
import xyz.dkos.gaming.mindustry.translator.service.TranslationService;
import xyz.dkos.gaming.mindustry.translator.utils.BundleHelper;

import java.util.Locale;

public class SettingsUI {

    private final TranslationService translationService;

    // UI Component References
    private CheckBox enabledCheck;
    private CheckBox serverCheck;
    private CheckBox hideOriginalCheck;
    private CheckBox preserveColorCheck;

    private CheckBox showUiCheck;
    private CheckBox autoTranslateCheck;
    private CheckBox addPrefixCheck;
    private TextField prefixContentField;

    private CheckBox debugCheck;
    private CheckBox debugChatCheck;
    private TextField userAgentField;
    private TextField endpointField;
    private TextField modelField;
    private TextField keyField;
    private Slider tempSlider;
    private TextArea promptArea;

    public SettingsUI(TranslationService translationService) {
        this.translationService = translationService;
    }

    public void build() {
        if (Vars.ui == null || Vars.ui.settings == null) {
            return;
        }

        Vars.ui.settings.addCategory(BundleHelper.get("translator.settings.category"), Icon.settings, this::buildContent);
    }

    private void buildContent(Table table) {
        table.add("[cyan]" + BundleHelper.get("translator.settings.incoming-category")).left().row();
        buildIncomingSettings(table);
        buildDivider(table);

        table.add("[cyan]" + BundleHelper.get("translator.settings.outgoing-category")).left().row();
        buildOutgoingSettings(table);
        buildDivider(table);

        table.add("[cyan]" + BundleHelper.get("translator.settings.engine-category")).left().row();
        buildEngineSelector(table);
        buildUserAgentField(table);

        buildDebugSettings(table);
        buildDivider(table);

        buildOpenAISettings(table);
        buildDivider(table);

        buildDangerZone(table);
    }

    private void buildIncomingSettings(Table table) {
        enabledCheck = table.check(BundleHelper.get("translator.settings.enabled"),
                TranslatorConfig.isEnabled(),
                TranslatorConfig::setEnabled).left().get();
        table.row();

        serverCheck = table.check(BundleHelper.get("translator.settings.server"),
                TranslatorConfig.isTranslateServerEnabled(),
                TranslatorConfig::setTranslateServer).left().get();
        table.row();

        hideOriginalCheck = table.check(BundleHelper.get("translator.settings.hide-original"),
                TranslatorConfig.isHideOriginal(),
                TranslatorConfig::setHideOriginal).left().get();
        table.row();

        preserveColorCheck = table.check(BundleHelper.get("translator.settings.preserve-color"),
                TranslatorConfig.isPreserveColor(),
                TranslatorConfig::setPreserveColor).left().get();
        table.row();
    }

    private void buildOutgoingSettings(Table table) {
        showUiCheck = table.check(BundleHelper.get("translator.settings.show-ui"),
                TranslatorConfig.isShowUI(),
                TranslatorConfig::setShowUI).left().get();
        table.row();

        autoTranslateCheck = table.check(BundleHelper.get("translator.settings.auto-translate"),
                TranslatorConfig.isAutoTranslate(),
                TranslatorConfig::setAutoTranslate).left().get();
        table.row();

        addPrefixCheck = table.check(BundleHelper.get("translator.settings.add-prefix"),
                TranslatorConfig.isAddPrefix(),
                TranslatorConfig::setAddPrefix).left().get();
        table.row();

        prefixContentField = new TextField(TranslatorConfig.getPrefixContent());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.prefix-content")).left().padRight(5f);
            prefixContentField.changed(() -> TranslatorConfig.setPrefixContent(prefixContentField.getText()));
            t.add(prefixContentField).width(200f);

            t.button(BundleHelper.get("translator.settings.reset"), () -> {
                prefixContentField.setText(TranslatorConfig.DEFAULT_PREFIX_CONTENT);
                TranslatorConfig.setPrefixContent(TranslatorConfig.DEFAULT_PREFIX_CONTENT);
            }).width(80f).padLeft(10f);
        }).left().padTop(5f).row();
    }

    private void buildDebugSettings(Table table) {
        debugCheck = table.check(BundleHelper.get("translator.settings.debug"),
                TranslatorConfig.isDebugMode(),
                TranslatorConfig::setDebugMode).left().get();
        table.row();

        debugChatCheck = table.check(BundleHelper.get("translator.settings.debug-chat"),
                TranslatorConfig.isDebugInChat(),
                TranslatorConfig::setDebugInChat).left().get();
        table.row();
    }

    private void buildEngineSelector(Table table) {
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.engine")).left().padRight(15f);

            t.button(b -> b.label(() -> TranslatorConfig.getEngine()), () -> {
                String current = TranslatorConfig.getEngine();
                int currentIndex = findEngineIndex(current);
                int nextIndex = (currentIndex + 1) % TranslatorConfig.ENGINES.length;
                TranslatorConfig.setEngine(TranslatorConfig.ENGINES[nextIndex]);
            }).size(120f, 40f);
        }).left().padTop(5f).row();
    }

    private void buildUserAgentField(Table table) {
        userAgentField = new TextField(TranslatorConfig.getUserAgent());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.user-agent")).left().padRight(5f);
            userAgentField.changed(() -> TranslatorConfig.setUserAgent(userAgentField.getText()));
            t.add(userAgentField).width(300f);

            t.button(BundleHelper.get("translator.settings.reset"), () -> {
                userAgentField.setText(TranslatorConfig.DEFAULT_USER_AGENT);
                TranslatorConfig.setUserAgent(TranslatorConfig.DEFAULT_USER_AGENT);
            }).width(80f).padLeft(10f);
        }).left().padTop(5f).row();
    }

    private int findEngineIndex(String engine) {
        for (int i = 0; i < TranslatorConfig.ENGINES.length; i++) {
            if (TranslatorConfig.ENGINES[i].equalsIgnoreCase(engine)) {
                return i;
            }
        }
        return 0;
    }

    private void buildOpenAISettings(Table table) {
        table.add("[cyan]" + BundleHelper.get("translator.settings.openai-config")).left().row();

        buildEndpointField(table);
        buildModelField(table);
        buildKeyField(table);
        buildTemperatureSlider(table);
        buildPromptArea(table);
        buildTestButton(table);
    }

    private void buildEndpointField(Table table) {
        endpointField = new TextField(TranslatorConfig.getOpenAIEndpoint());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.endpoint")).left().padRight(5f);
            endpointField.changed(() -> TranslatorConfig.setOpenAIEndpoint(endpointField.getText()));
            t.add(endpointField).width(350f);
        }).left().padTop(5f).row();
    }

    private void buildModelField(Table table) {
        modelField = new TextField(TranslatorConfig.getOpenAIModel());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.model")).left().padRight(5f);
            modelField.changed(() -> TranslatorConfig.setOpenAIModel(modelField.getText()));
            t.add(modelField).width(350f);
        }).left().padTop(5f).row();
    }

    private void buildKeyField(Table table) {
        keyField = new TextField(TranslatorConfig.getOpenAIKey());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.apikey")).left().padRight(5f);
            keyField.setPasswordMode(true);
            keyField.setPasswordCharacter('*');
            keyField.changed(() -> TranslatorConfig.setOpenAIKey(keyField.getText()));
            t.add(keyField).width(350f);
        }).left().padTop(5f).row();
    }

    private void buildTemperatureSlider(Table table) {
        tempSlider = new Slider(0f, 2f, 0.1f, false);
        tempSlider.setValue(TranslatorConfig.getOpenAITemperature());

        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.temperature")).left().padRight(5f);
            tempSlider.changed(() -> TranslatorConfig.setOpenAITemperature(tempSlider.getValue()));
            t.add(tempSlider).width(150f);
            t.label(() -> String.format(Locale.US, "%.1f", tempSlider.getValue())).width(30f).padLeft(5f);
            t.button(BundleHelper.get("translator.settings.reset"), () -> {
                tempSlider.setValue(TranslatorConfig.DEFAULT_OPENAI_TEMP);
                TranslatorConfig.setOpenAITemperature(TranslatorConfig.DEFAULT_OPENAI_TEMP);
            }).width(80f).padLeft(10f);
        }).left().padTop(5f).row();
    }

    private void buildPromptArea(Table table) {
        promptArea = new TextArea(TranslatorConfig.getOpenAIPrompt());
        table.table(t -> {
            t.add(BundleHelper.get("translator.settings.prompt")).left().top().padRight(5f);
            promptArea.changed(() -> TranslatorConfig.setOpenAIPrompt(promptArea.getText()));
            t.add(promptArea).width(350f).height(180f);
            t.button(BundleHelper.get("translator.settings.reset"), () -> {
                promptArea.setText(TranslatorConfig.DEFAULT_PROMPT);
                TranslatorConfig.setOpenAIPrompt(TranslatorConfig.DEFAULT_PROMPT);
            }).width(80f).padLeft(10f).top();
        }).left().padTop(5f).row();
    }

    private void buildTestButton(Table table) {
        table.button("[cyan]" + BundleHelper.get("translator.settings.test-openai"), () -> {
            Vars.ui.loadfrag.show(BundleHelper.get("translator.message.testing"));

            translationService.testOpenAI(
                    result -> {
                        Vars.ui.loadfrag.hide();
                        Vars.ui.showInfo(BundleHelper.get("translator.message.test-success", result));
                    },
                    error -> {
                        Vars.ui.loadfrag.hide();
                        Vars.ui.showErrorMessage(BundleHelper.get("translator.message.test-failed", error.getMessage()));
                    }
            );
        }).width(250f).padTop(10f).left().row();
    }

    private void buildDangerZone(Table table) {
        table.add("[scarlet]" + BundleHelper.get("translator.settings.danger")).left().row();

        table.button("[scarlet]" + BundleHelper.get("translator.settings.reset-all"), () -> {
            Vars.ui.showConfirm(
                    BundleHelper.get("translator.message.reset-confirm-title"),
                    BundleHelper.get("translator.message.reset-confirm"),
                    this::resetAllSettings
            );
        }).width(250f).padTop(5f).left().row();
    }

    private void resetAllSettings() {
        TranslatorConfig.resetAll();

        // Reset UI components
        enabledCheck.setChecked(TranslatorConfig.DEFAULT_ENABLED);
        serverCheck.setChecked(TranslatorConfig.DEFAULT_TRANSLATE_SERVER);
        hideOriginalCheck.setChecked(TranslatorConfig.DEFAULT_HIDE_ORIGINAL);
        preserveColorCheck.setChecked(TranslatorConfig.DEFAULT_PRESERVE_COLOR);

        showUiCheck.setChecked(TranslatorConfig.DEFAULT_SHOW_UI);
        autoTranslateCheck.setChecked(TranslatorConfig.DEFAULT_AUTO_TRANSLATE);
        addPrefixCheck.setChecked(TranslatorConfig.DEFAULT_ADD_PREFIX);
        prefixContentField.setText(TranslatorConfig.DEFAULT_PREFIX_CONTENT);

        debugCheck.setChecked(TranslatorConfig.DEFAULT_DEBUG_MODE);
        debugChatCheck.setChecked(TranslatorConfig.DEFAULT_DEBUG_IN_CHAT);
        userAgentField.setText(TranslatorConfig.DEFAULT_USER_AGENT);
        endpointField.setText(TranslatorConfig.DEFAULT_OPENAI_ENDPOINT);
        modelField.setText(TranslatorConfig.DEFAULT_OPENAI_MODEL);
        keyField.setText(TranslatorConfig.DEFAULT_OPENAI_KEY);
        tempSlider.setValue(TranslatorConfig.DEFAULT_OPENAI_TEMP);
        promptArea.setText(TranslatorConfig.DEFAULT_PROMPT);

        Vars.ui.showInfo(BundleHelper.get("translator.message.reset-success"));
    }

    private void buildDivider(Table table) {
        table.image().color(arc.graphics.Color.gray).fillX().height(3f).pad(15f, 0, 15f, 0).row();
    }
}