/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.settings.preferences;

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.settings.ChatNotificationType;
import bisq.user.reputation.ReputationScore;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PreferencesView extends View<VBox, PreferencesModel, PreferencesController> {
    private static final ValidatorBase REPUTATION_SCORE_VALIDATOR =
            new NumberValidator(Res.get("settings.preferences.trade.minReputationScore.invalid", ReputationScore.MAX_VALUE),
                    0, ReputationScore.MAX_VALUE);
    private static final ValidatorBase DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR =
            new NumberValidator(Res.get("settings.preferences.network.difficultyAdjustmentFactor.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    0, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT);

    private final Button resetDontShowAgain, clearNotifications, addLanguageButton;
    private final Switch useAnimations, preventStandbyMode, offersOnlySwitch, closeMyOfferWhenTaken, notifyForPreRelease,
            useTransientNotifications, ignoreDiffAdjustFromSecManagerSwitch,
            ignoreMinRequiredReputationScoreFromSecManagerSwitch;
    private final ToggleGroup notificationsToggleGroup = new ToggleGroup();
    private final RadioButton all, mention, off;
    private final ChangeListener<Toggle> notificationsToggleListener;
    private final AutoCompleteComboBox<String> languageSelection, supportedLanguagesComboBox;
    private final MaterialTextField minRequiredReputationScore, difficultyAdjustmentFactor;
    private Subscription selectedNotificationTypePin, getSelectedLSupportedLanguageCodePin;

    public PreferencesView(PreferencesModel model, PreferencesController controller) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        // Language
        Label languageSelectionHeadline = new Label(Res.get("settings.preferences.language.headline"));
        languageSelectionHeadline.getStyleClass().add("large-thin-headline");

        languageSelection = new AutoCompleteComboBox<>(model.getLanguageCodes(), Res.get("settings.preferences.language.select"));
        languageSelection.setPrefWidth(300);
        languageSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(String languageCode) {
                return languageCode != null ? controller.getDisplayLanguage(languageCode) : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });
        languageSelection.validateOnNoItemSelectedWithMessage(Res.get("settings.preferences.language.select.invalid"));

        // Supported languages
        Label supportedLanguagesHeadline = new Label(Res.get("settings.preferences.language.supported.headline"));
        supportedLanguagesHeadline.getStyleClass().add("large-thin-headline");

        GridPane supportedLanguageGridPane = new GridPane();
        supportedLanguageGridPane.setVgap(5);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        supportedLanguageGridPane.getColumnConstraints().addAll(col1, col2);
        int rowIndex = 0;

        Label selectSupportedLanguageHeadline = new Label(Res.get("settings.preferences.language.supported.subHeadLine"));
        selectSupportedLanguageHeadline.getStyleClass().add("settings-sub-headline");
        supportedLanguageGridPane.add(selectSupportedLanguageHeadline, 0, rowIndex);

        supportedLanguagesComboBox = new AutoCompleteComboBox<>(model.getSupportedLanguageCodeFilteredList(),
                Res.get("settings.preferences.language.supported.select"));
        supportedLanguagesComboBox.setMinWidth(150);
        supportedLanguagesComboBox.setMaxWidth(Double.MAX_VALUE); // Needed to force scale to available space
        supportedLanguagesComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String languageCode) {
                return languageCode != null ? controller.getDisplayLanguage(languageCode) : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });
        supportedLanguagesComboBox.validateOnNoItemSelectedWithMessage(Res.get("settings.preferences.language.supported.invalid"));

        addLanguageButton = BisqIconButton.createIconButton("arrow-right-sign",
                Res.get("settings.preferences.language.supported.addButton.tooltip"));

        HBox.setMargin(addLanguageButton, new Insets(12.5, 15, 0, 15));
        HBox.setHgrow(addLanguageButton, Priority.ALWAYS);
        HBox.setHgrow(supportedLanguagesComboBox, Priority.ALWAYS);
        HBox hBox = new HBox(0, supportedLanguagesComboBox, addLanguageButton);

        GridPane.setValignment(hBox, VPos.TOP);
        supportedLanguageGridPane.add(hBox, 0, ++rowIndex);

        Label supportedLanguageListViewSubHeadline = new Label(Res.get("settings.preferences.language.supported.list.subHeadLine"));
        supportedLanguageListViewSubHeadline.getStyleClass().add("settings-sub-headline");
        rowIndex = 0;
        supportedLanguageGridPane.add(supportedLanguageListViewSubHeadline, 1, rowIndex);

        ListView<String> supportedLanguageListView = new ListView<>(model.getSelectedSupportedLanguageCodes());
        supportedLanguageListView.setCellFactory(getSupportedLanguageCellFactory(controller));
        supportedLanguageListView.setMinWidth(150);
        supportedLanguageGridPane.setMaxHeight(150);
        supportedLanguageGridPane.add(supportedLanguageListView, 1, ++rowIndex);


        // Notifications
        Label notificationsHeadline = new Label(Res.get("settings.preferences.notification.options"));
        notificationsHeadline.getStyleClass().add("large-thin-headline");

        all = new RadioButton(Res.get("settings.preferences.notification.option.all"));
        all.setToggleGroup(notificationsToggleGroup);
        all.setUserData(ChatNotificationType.ALL);
        mention = new RadioButton(Res.get("settings.preferences.notification.option.mention"));
        mention.setToggleGroup(notificationsToggleGroup);
        mention.setUserData(ChatNotificationType.MENTION);
        off = new RadioButton(Res.get("settings.preferences.notification.option.off"));
        off.setToggleGroup(notificationsToggleGroup);
        off.setUserData(ChatNotificationType.OFF);

        notifyForPreRelease = new Switch(Res.get("settings.preferences.notification.notifyForPreRelease"));

        useTransientNotifications = new Switch(Res.get("settings.preferences.notification.useTransientNotifications"));

        clearNotifications = new Button(Res.get("settings.preferences.notification.clearNotifications"));
        clearNotifications.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(notifyForPreRelease, new Insets(10, 0, 0, 0));
        VBox.setMargin(clearNotifications, new Insets(10, 0, 0, 0));
        VBox notificationsVBox = new VBox(10, all, mention, off,
                notifyForPreRelease, useTransientNotifications, clearNotifications);


        // Display
        Label displayHeadline = new Label(Res.get("settings.preferences.display.headline"));
        displayHeadline.getStyleClass().add("large-thin-headline");

        useAnimations = new Switch(Res.get("settings.preferences.display.useAnimations"));
        preventStandbyMode = new Switch(Res.get("settings.preferences.display.preventStandbyMode"));
        resetDontShowAgain = new Button(Res.get("settings.preferences.display.resetDontShowAgain"));
        resetDontShowAgain.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(resetDontShowAgain, new Insets(10, 0, 0, 0));
        VBox displayVBox = new VBox(10, useAnimations, preventStandbyMode, resetDontShowAgain);


        // Trade
        Label tradeHeadline = new Label(Res.get("settings.preferences.trade.headline"));
        tradeHeadline.getStyleClass().add("large-thin-headline");

        offersOnlySwitch = new Switch(Res.get("bisqEasy.topPane.filter.offersOnly"));
        closeMyOfferWhenTaken = new Switch(Res.get("settings.preferences.trade.closeMyOfferWhenTaken"));
        minRequiredReputationScore = new MaterialTextField(Res.get("settings.preferences.trade.requiredTotalReputationScore"),
                null, Res.get("settings.preferences.trade.requiredTotalReputationScore.help"));
        minRequiredReputationScore.setMaxWidth(400);
        minRequiredReputationScore.setValidators(REPUTATION_SCORE_VALIDATOR);
        ignoreMinRequiredReputationScoreFromSecManagerSwitch = new Switch(Res.get("settings.preferences.network.minReputationScore.ignoreValueFromSecManager"));

        VBox tradeVBox = new VBox(10,
                closeMyOfferWhenTaken,
                offersOnlySwitch,
                minRequiredReputationScore, ignoreMinRequiredReputationScoreFromSecManagerSwitch
        );


        // Network
        Label networkHeadline = new Label(Res.get("settings.preferences.network.headline"));
        networkHeadline.getStyleClass().add("large-thin-headline");

        difficultyAdjustmentFactor = new MaterialTextField();
        difficultyAdjustmentFactor.setMaxWidth(400);
        difficultyAdjustmentFactor.setValidators(DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR);
        ignoreDiffAdjustFromSecManagerSwitch = new Switch(Res.get("settings.preferences.network.difficultyAdjustmentFactor.ignoreValueFromSecManager"));

        VBox networkVBox = new VBox(10, difficultyAdjustmentFactor, ignoreDiffAdjustFromSecManagerSwitch);


        Insets insets = new Insets(0, 5, 0, 5);
        VBox.setMargin(languageSelection, insets);
        VBox.setMargin(supportedLanguageGridPane, insets);
        VBox.setMargin(notificationsVBox, insets);
        VBox.setMargin(displayVBox, insets);
        VBox.setMargin(tradeVBox, insets);
        root.getChildren().addAll(languageSelectionHeadline, getLine(), languageSelection,
                supportedLanguagesHeadline, getLine(), supportedLanguageGridPane,
                notificationsHeadline, getLine(), notificationsVBox,
                displayHeadline, getLine(), displayVBox,
                tradeHeadline, getLine(), tradeVBox,
                networkHeadline, getLine(), networkVBox);

        notificationsToggleListener = (observable, oldValue, newValue) -> controller.onSetChatNotificationType((ChatNotificationType) newValue.getUserData());
    }

    @Override
    protected void onViewAttached() {
        notificationsToggleGroup.selectedToggleProperty().addListener(notificationsToggleListener);
        selectedNotificationTypePin = EasyBind.subscribe(model.getChatNotificationType(), selected -> applyChatNotificationType());

        notifyForPreRelease.selectedProperty().bindBidirectional(model.getNotifyForPreRelease());
        useTransientNotifications.selectedProperty().bindBidirectional(model.getUseTransientNotifications());
        useTransientNotifications.setVisible(model.isUseTransientNotificationsVisible());
        useTransientNotifications.setManaged(model.isUseTransientNotificationsVisible());
        useAnimations.selectedProperty().bindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().bindBidirectional(model.getPreventStandbyMode());
        offersOnlySwitch.selectedProperty().bindBidirectional(model.getOfferOnly());
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().bindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());
        ignoreMinRequiredReputationScoreFromSecManagerSwitch.selectedProperty().bindBidirectional(model.getIgnoreMinRequiredReputationScoreFromSecManager());
        closeMyOfferWhenTaken.selectedProperty().bindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.bindBidirectional(minRequiredReputationScore.textProperty(), model.getMinRequiredReputationScore(), new NumberStringConverter());
        minRequiredReputationScore.descriptionProperty().bind(model.getMinRequiredReputationScoreDescriptionText());
        minRequiredReputationScore.getTextInputControl().editableProperty().bind(model.getMinRequiredReputationScoreEditable());

        Bindings.bindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor(), new NumberStringConverter());
        difficultyAdjustmentFactor.descriptionProperty().bind(model.getDifficultyAdjustmentFactorDescriptionText());
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().bind(model.getDifficultyAdjustmentFactorEditable());

        languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
        languageSelection.setOnChangeConfirmed(e -> {
            if (languageSelection.getSelectionModel().getSelectedItem() == null) {
                languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
                return;
            }
            controller.onSelectLanguage(languageSelection.getSelectionModel().getSelectedItem());
        });

        supportedLanguagesComboBox.getSelectionModel().select(model.getSelectedLSupportedLanguageCode().get());
        supportedLanguagesComboBox.setOnChangeConfirmed(e -> {
            if (supportedLanguagesComboBox.getSelectionModel().getSelectedItem() == null) {
                supportedLanguagesComboBox.getSelectionModel().select(model.getSelectedLSupportedLanguageCode().get());
            }
            controller.onSelectSupportedLanguage(supportedLanguagesComboBox.getSelectionModel().getSelectedItem());
        });

        getSelectedLSupportedLanguageCodePin = EasyBind.subscribe(model.getSelectedLSupportedLanguageCode(),
                e -> supportedLanguagesComboBox.getSelectionModel().select(e));

        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain());
        clearNotifications.setOnAction(e -> controller.onClearNotifications());
        addLanguageButton.setOnAction(e -> {
            if (supportedLanguagesComboBox.validate()) {
                controller.onAddSupportedLanguage();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        notifyForPreRelease.selectedProperty().unbindBidirectional(model.getNotifyForPreRelease());
        useTransientNotifications.selectedProperty().unbindBidirectional(model.getUseTransientNotifications());
        useAnimations.selectedProperty().unbindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().unbindBidirectional(model.getPreventStandbyMode());
        offersOnlySwitch.selectedProperty().unbindBidirectional(model.getOfferOnly());
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().unbindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());
        ignoreMinRequiredReputationScoreFromSecManagerSwitch.selectedProperty().unbindBidirectional(model.getIgnoreMinRequiredReputationScoreFromSecManager());
        closeMyOfferWhenTaken.selectedProperty().unbindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.unbindBidirectional(minRequiredReputationScore.textProperty(), model.getMinRequiredReputationScore());
        minRequiredReputationScore.getTextInputControl().editableProperty().unbind();
        minRequiredReputationScore.descriptionProperty().unbind();

        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().unbind();
        difficultyAdjustmentFactor.descriptionProperty().unbind();

        notificationsToggleGroup.selectedToggleProperty().removeListener(notificationsToggleListener);
        selectedNotificationTypePin.unsubscribe();
        getSelectedLSupportedLanguageCodePin.unsubscribe();

        resetDontShowAgain.setOnAction(null);
        clearNotifications.setOnAction(null);
        addLanguageButton.setOnAction(null);
        languageSelection.setOnChangeConfirmed(null);
        supportedLanguagesComboBox.setOnChangeConfirmed(null);

        languageSelection.resetValidation();
        supportedLanguagesComboBox.resetValidation();
        minRequiredReputationScore.resetValidation();
        difficultyAdjustmentFactor.resetValidation();
    }

    private Region getLine() {
        Region line = Layout.hLine();
        VBox.setMargin(line, new Insets(-42.5, 0, -30, 0));
        return line;
    }

    private void applyChatNotificationType() {
        switch (model.getChatNotificationType().get()) {
            case ALL: {
                notificationsToggleGroup.selectToggle(all);
                break;
            }
            case MENTION: {
                notificationsToggleGroup.selectToggle(mention);
                break;
            }
            case OFF: {
                notificationsToggleGroup.selectToggle(off);
                break;
            }
        }
    }

    private Callback<ListView<String>, ListCell<String>> getSupportedLanguageCellFactory(PreferencesController controller) {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> list) {
                return new ListCell<>() {
                    private final Button button = new Button(Res.get("data.remove"));
                    private final Label label = new Label();
                    private final HBox hBox = new HBox(10, label, Spacer.fillHBox(), button);

                    {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    public void updateItem(String languageCode, boolean empty) {
                        super.updateItem(languageCode, empty);
                        if (languageCode != null && !empty) {
                            label.setText(controller.getDisplayLanguage(languageCode));
                            button.setOnAction(e -> controller.onRemoveSupportedLanguage(languageCode));
                            button.getStyleClass().add("grey-transparent-outlined-button");
                            setGraphic(hBox);
                        } else {
                            button.setOnAction(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }
}
