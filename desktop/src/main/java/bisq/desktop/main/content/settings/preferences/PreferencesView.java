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

import bisq.desktop.common.Icons;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import bisq.settings.ChatNotificationType;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PreferencesView extends View<VBox, PreferencesModel, PreferencesController> {
    private final Button resetDontShowAgain, addSupportedLanguageButton;
    private final Switch useAnimations, preventStandbyMode, offersOnlySwitch, closeMyOfferWhenTaken, notifyForPreRelease;
    private final ToggleGroup notificationsToggleGroup = new ToggleGroup();
    private final RadioButton all, mention, off;
    private final ChangeListener<Toggle> notificationsToggleListener;
    private final AutoCompleteComboBox<String> languageSelection, supportedLanguageSelection;
    private final MaterialTextField requiredTotalReputationScore;
    private Subscription selectedNotificationTypePin;
    private Subscription getSelectedLSupportedLanguageCodePin;

    public PreferencesView(PreferencesModel model, PreferencesController controller) {
        super(new VBox(20), model, controller);

        // Language
        Label languageSelectionHeadline = new Label(Res.get("settings.preferences.language.headline"));
        languageSelectionHeadline.getStyleClass().addAll("settings-headline");

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

        Label supportedLanguageSelectionHeadline = new Label(Res.get("settings.preferences.language.supported.headline"));
        supportedLanguageSelectionHeadline.getStyleClass().addAll("settings-headline");

        Label supportedLanguageSelectionSubHeadline = new Label(Res.get("settings.preferences.language.supported.subHeadLine"));
        supportedLanguageSelectionSubHeadline.getStyleClass().addAll("settings-sub-headLine");

        supportedLanguageSelection = new AutoCompleteComboBox<>(model.getSupportedLanguageCodeFilteredList(), Res.get("settings.preferences.language.supported.select"));
        supportedLanguageSelection.setMinWidth(300);
        supportedLanguageSelection.setConverter(new StringConverter<>() {
            @Override
            public String toString(String languageCode) {
                return languageCode != null ? controller.getDisplayLanguage(languageCode) : "";
            }

            @Override
            public String fromString(String string) {
                return null;
            }
        });

        Label icon = Icons.getIcon(AwesomeIcon.CIRCLE_ARROW_RIGHT);
        addSupportedLanguageButton = new Button(Res.get("settings.preferences.language.supported.add"), icon);
        addSupportedLanguageButton.setGraphicTextGap(5);
        addSupportedLanguageButton.setDefaultButton(true);
        addSupportedLanguageButton.setMinWidth(240);
        HBox selectionAndButtonHBox = new HBox(20, supportedLanguageSelection, addSupportedLanguageButton);
        selectionAndButtonHBox.setAlignment(Pos.CENTER_LEFT);
        VBox addSupportedLanguageVBox = new VBox(10, supportedLanguageSelectionSubHeadline, selectionAndButtonHBox);
        addSupportedLanguageVBox.setMaxWidth(300);

        Label supportedLanguageListViewSubHeadline = new Label(Res.get("settings.preferences.language.supported.list.subHeadLine"));
        supportedLanguageListViewSubHeadline.getStyleClass().addAll("settings-sub-headLine");

        ListView<String> supportedLanguageListView = new ListView<>(model.getSelectedSupportedLanguageCodes());
        supportedLanguageListView.setCellFactory(getSupportedLanguageCellFactory(controller));
        supportedLanguageListView.setMinWidth(450);
        supportedLanguageListView.setMaxHeight(150);
        VBox supportedLanguageListViewVBox = new VBox(10, supportedLanguageListViewSubHeadline, supportedLanguageListView);

        HBox.setHgrow(supportedLanguageListViewVBox, Priority.ALWAYS);
        HBox addSupportedLanguageHBox = new HBox(20, addSupportedLanguageVBox, supportedLanguageListViewVBox);
        addSupportedLanguageHBox.getStyleClass().add("settings-box-bg");

        // Notifications
        Label notificationsHeadline = new Label(Res.get("settings.preferences.notification.options"));
        notificationsHeadline.getStyleClass().addAll("settings-headline");

        all = new RadioButton(Res.get("settings.preferences.notification.option.all"));
        mention = new RadioButton(Res.get("settings.preferences.notification.option.mention"));
        off = new RadioButton(Res.get("settings.preferences.notification.option.off"));

        all.setToggleGroup(notificationsToggleGroup);
        mention.setToggleGroup(notificationsToggleGroup);
        off.setToggleGroup(notificationsToggleGroup);

        all.setUserData(ChatNotificationType.ALL);
        mention.setUserData(ChatNotificationType.MENTION);
        off.setUserData(ChatNotificationType.OFF);

        notifyForPreRelease = new Switch(Res.get("settings.preferences.notification.notifyForPreRelease"));

        VBox.setMargin(notifyForPreRelease, new Insets(10, 0, 0, 0));
        VBox notificationsVBox = new VBox(10, all, mention, off, notifyForPreRelease);
        notificationsVBox.setPadding(new Insets(10));
        notificationsVBox.getStyleClass().add("settings-box-bg");

        // Display
        Label displayHeadline = new Label(Res.get("settings.preferences.display.headline"));
        displayHeadline.getStyleClass().addAll("settings-headline");

        useAnimations = new Switch(Res.get("settings.preferences.display.useAnimations"));
        preventStandbyMode = new Switch(Res.get("settings.preferences.display.preventStandbyMode"));
        resetDontShowAgain = new Button(Res.get("settings.preferences.display.resetDontShowAgain"));
        resetDontShowAgain.getStyleClass().add("outlined-button");

        VBox.setMargin(resetDontShowAgain, new Insets(10, 0, 0, 0));
        VBox displayVBox = new VBox(10, useAnimations, preventStandbyMode, resetDontShowAgain);
        displayVBox.setPadding(new Insets(10));
        displayVBox.getStyleClass().add("settings-box-bg");

        // Trade
        Label tradeHeadline = new Label(Res.get("settings.preferences.trade.headline"));
        tradeHeadline.getStyleClass().addAll("settings-headline");
        offersOnlySwitch = new Switch(Res.get("bisqEasy.topPane.filter.offersOnly"));
        closeMyOfferWhenTaken = new Switch(Res.get("settings.preferences.trade.closeMyOfferWhenTaken"));
        requiredTotalReputationScore = new MaterialTextField(Res.get("settings.preferences.trade.requiredTotalReputationScore"),
                null, Res.get("settings.preferences.trade.requiredTotalReputationScore.help"));
        requiredTotalReputationScore.setMaxWidth(300);

        VBox tradeVBox = new VBox(10, requiredTotalReputationScore, offersOnlySwitch, closeMyOfferWhenTaken);
        tradeVBox.setPadding(new Insets(10));
        tradeVBox.getStyleClass().add("settings-box-bg");

        VBox.setMargin(languageSelectionHeadline, new Insets(20, 0, -10, 0));
        VBox.setMargin(supportedLanguageSelectionHeadline, new Insets(10, 0, -10, 0));
        VBox.setMargin(notificationsHeadline, new Insets(10, 0, -10, 0));
        VBox.setMargin(displayHeadline, new Insets(10, 0, -10, 0));
        VBox.setMargin(tradeHeadline, new Insets(10, 0, -10, 0));
        root.getChildren().addAll(languageSelectionHeadline, languageSelection,
                supportedLanguageSelectionHeadline, addSupportedLanguageHBox,
                notificationsHeadline, notificationsVBox,
                displayHeadline, displayVBox,
                tradeHeadline, tradeVBox);

        notificationsToggleListener = (observable, oldValue, newValue) -> controller.onSetChatNotificationType((ChatNotificationType) newValue.getUserData());
    }

    @Override
    protected void onViewAttached() {
        notificationsToggleGroup.selectedToggleProperty().addListener(notificationsToggleListener);
        selectedNotificationTypePin = EasyBind.subscribe(model.getChatNotificationType(), selected -> applyChatNotificationType());

        notifyForPreRelease.selectedProperty().bindBidirectional(model.getNotifyForPreRelease());
        useAnimations.selectedProperty().bindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().bindBidirectional(model.getPreventStandbyMode());
        offersOnlySwitch.selectedProperty().bindBidirectional(model.getOfferOnly());
        closeMyOfferWhenTaken.selectedProperty().bindBidirectional(model.getCloseMyOfferWhenTaken());
        addSupportedLanguageButton.disableProperty().bind(model.getAddSupportedLanguageButtonDisabled());

        Bindings.bindBidirectional(requiredTotalReputationScore.textProperty(), model.getRequiredTotalReputationScore(), new NumberStringConverter());

        languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
        languageSelection.setOnChangeConfirmed(e -> {
            if (languageSelection.getSelectionModel().getSelectedItem() == null) {
                languageSelection.getSelectionModel().select(model.getSelectedLanguageCode());
                return;
            }
            controller.onSelectLanguage(languageSelection.getSelectionModel().getSelectedItem());
        });

        supportedLanguageSelection.getSelectionModel().select(model.getSelectedLSupportedLanguageCode().get());
        supportedLanguageSelection.setOnChangeConfirmed(e -> {
            if (supportedLanguageSelection.getSelectionModel().getSelectedItem() == null) {
                supportedLanguageSelection.getSelectionModel().select(model.getSelectedLSupportedLanguageCode().get());
                return;
            }
            controller.onSelectSupportedLanguage(supportedLanguageSelection.getSelectionModel().getSelectedItem());
        });

        getSelectedLSupportedLanguageCodePin = EasyBind.subscribe(model.getSelectedLSupportedLanguageCode(),
                e -> supportedLanguageSelection.getSelectionModel().select(e));

        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain());
        addSupportedLanguageButton.setOnAction(e -> controller.onAddSupportedLanguage());
    }

    @Override
    protected void onViewDetached() {
        notificationsToggleGroup.selectedToggleProperty().removeListener(notificationsToggleListener);
        selectedNotificationTypePin.unsubscribe();
        getSelectedLSupportedLanguageCodePin.unsubscribe();

        notifyForPreRelease.selectedProperty().unbindBidirectional(model.getNotifyForPreRelease());
        useAnimations.selectedProperty().unbindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().unbindBidirectional(model.getPreventStandbyMode());

        offersOnlySwitch.selectedProperty().unbindBidirectional(model.getOfferOnly());
        closeMyOfferWhenTaken.selectedProperty().unbindBidirectional(model.getCloseMyOfferWhenTaken());

        Bindings.unbindBidirectional(requiredTotalReputationScore.textProperty(), model.getRequiredTotalReputationScore());

        languageSelection.setOnChangeConfirmed(null);
        supportedLanguageSelection.setOnChangeConfirmed(null);

        resetDontShowAgain.setOnAction(null);
        addSupportedLanguageButton.setOnAction(null);
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
                    final Button button = new Button(Res.get("settings.preferences.language.supported.list.remove"));
                    final Label label = new Label();
                    final HBox hBox = new HBox(10, label, Spacer.fillHBox(), button);

                    {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        hBox.setFillHeight(true);
                        hBox.setPadding(new Insets(10, 10, 0, 0));
                    }

                    @Override
                    public void updateItem(String languageCode, boolean empty) {
                        super.updateItem(languageCode, empty);
                        if (languageCode != null && !empty) {
                            label.setText(controller.getDisplayLanguage(languageCode));
                            button.setOnAction(e -> controller.onRemoveSupportedLanguage(languageCode));
                            setGraphic(hBox);
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }
}
