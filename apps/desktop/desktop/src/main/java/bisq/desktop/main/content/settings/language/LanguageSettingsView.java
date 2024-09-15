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

package bisq.desktop.main.content.settings.language;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class LanguageSettingsView extends View<VBox, LanguageSettingsModel, LanguageSettingsController> {
    private final Button addLanguageButton;
    private final AutoCompleteComboBox<String> languageSelection, supportedLanguagesComboBox;
    private Subscription getSelectedSupportedLanguageCodePin;

    public LanguageSettingsView(LanguageSettingsModel model, LanguageSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // Language
        Label languageSelectionHeadline = SettingsViewUtils.getHeadline(Res.get("settings.language.headline"));

        languageSelection = new AutoCompleteComboBox<>(model.getLanguageCodes(), Res.get("settings.language.select"));
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
        languageSelection.validateOnNoItemSelectedWithMessage(Res.get("settings.language.select.invalid"));

        // Supported languages
        Label supportedLanguagesHeadline = SettingsViewUtils.getHeadline(Res.get("settings.language.supported.headline"));

        GridPane supportedLanguageGridPane = new GridPane();
        supportedLanguageGridPane.setVgap(5);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        supportedLanguageGridPane.getColumnConstraints().addAll(col1, col2);
        int rowIndex = 0;

        Label selectSupportedLanguageHeadline = new Label(Res.get("settings.language.supported.subHeadLine"));
        selectSupportedLanguageHeadline.getStyleClass().add("settings-sub-headline");
        supportedLanguageGridPane.add(selectSupportedLanguageHeadline, 0, rowIndex);

        supportedLanguagesComboBox = new AutoCompleteComboBox<>(model.getSupportedLanguageCodeFilteredList(),
                Res.get("settings.language.supported.select"));
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
        supportedLanguagesComboBox.validateOnNoItemSelectedWithMessage(Res.get("settings.language.supported.invalid"));

        addLanguageButton = BisqIconButton.createIconButton("arrow-right-sign",
                Res.get("settings.language.supported.addButton.tooltip"));

        HBox.setMargin(addLanguageButton, new Insets(12.5, 15, 0, 15));
        HBox.setHgrow(addLanguageButton, Priority.ALWAYS);
        HBox.setHgrow(supportedLanguagesComboBox, Priority.ALWAYS);
        HBox hBox = new HBox(0, supportedLanguagesComboBox, addLanguageButton);

        GridPane.setValignment(hBox, VPos.TOP);
        supportedLanguageGridPane.add(hBox, 0, ++rowIndex);

        Label supportedLanguageListViewSubHeadline = new Label(Res.get("settings.language.supported.list.subHeadLine"));
        supportedLanguageListViewSubHeadline.getStyleClass().add("settings-sub-headline");
        rowIndex = 0;
        supportedLanguageGridPane.add(supportedLanguageListViewSubHeadline, 1, rowIndex);

        ListView<String> supportedLanguageListView = new ListView<>(model.getSelectedSupportedLanguageCodes());
        supportedLanguageListView.setCellFactory(getSupportedLanguageCellFactory(controller));
        supportedLanguageListView.setMinWidth(150);
        supportedLanguageGridPane.setMaxHeight(150);
        supportedLanguageGridPane.add(supportedLanguageListView, 1, ++rowIndex);

        Insets insets = new Insets(0, 5, 0, 5);
        VBox.setMargin(languageSelection, insets);
        VBox.setMargin(supportedLanguageGridPane, insets);
        VBox contentBox = new VBox(50);
        contentBox.getChildren().addAll(languageSelectionHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), languageSelection,
                supportedLanguagesHeadline, SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()), supportedLanguageGridPane);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
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

        getSelectedSupportedLanguageCodePin = EasyBind.subscribe(model.getSelectedLSupportedLanguageCode(),
                e -> supportedLanguagesComboBox.getSelectionModel().select(e));

        addLanguageButton.setOnAction(e -> {
            if (supportedLanguagesComboBox.validate()) {
                controller.onAddSupportedLanguage();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        getSelectedSupportedLanguageCodePin.unsubscribe();
        addLanguageButton.setOnAction(null);
        languageSelection.setOnChangeConfirmed(null);
        supportedLanguagesComboBox.setOnChangeConfirmed(null);

        languageSelection.resetValidation();
        supportedLanguagesComboBox.resetValidation();
    }

    private Callback<ListView<String>, ListCell<String>> getSupportedLanguageCellFactory(LanguageSettingsController controller) {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> list) {
                return new ListCell<>() {
                    private final Button button = new Button(Res.get("data.remove"));
                    private final Label label = new Label();
                    private final HBox hBox = new HBox(10, label, Spacer.fillHBox(), button);

                    {
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        button.getStyleClass().add("grey-transparent-outlined-button");
                    }

                    @Override
                    protected void updateItem(String languageCode, boolean empty) {
                        super.updateItem(languageCode, empty);

                        if (languageCode != null && !empty) {
                            label.setText(controller.getDisplayLanguage(languageCode));
                            button.setOnAction(e -> controller.onRemoveSupportedLanguage(languageCode));
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
