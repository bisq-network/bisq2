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
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class LanguageSettingsView extends View<VBox, LanguageSettingsModel, LanguageSettingsController> {
    private final Button addLanguageButton;
    private final AutoCompleteComboBox<String> languageSelection, countrySelection, currencySelection,
            supportedLanguagesComboBox;
    private Subscription getSelectedSupportedLanguageCodePin, selectedLanguageTagPin;

    public LanguageSettingsView(LanguageSettingsModel model, LanguageSettingsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        // --- MAIN HEADLINE ---
        Label mainHeadline = SettingsViewUtils.getHeadline(Res.get("settings.language.headline"));

        // 1. Language Selector
        languageSelection = new AutoCompleteComboBox<>(model.getLanguageTags(), Res.get("settings.language.select"));
        int comboBoxMinWidth = 250;
        languageSelection.setMinWidth(comboBoxMinWidth);
        languageSelection.setConverter(model.getLanguageStringConverter());
        languageSelection.validateOnNoItemSelectedWithMessage(Res.get("settings.language.select.invalid"));
        languageSelection.setMaxWidth(Double.MAX_VALUE);

        // 2. Country Selector
        countrySelection = new AutoCompleteComboBox<>(model.getCountryCodes(), Res.get("settings.language.region.country"));
        countrySelection.setMinWidth(comboBoxMinWidth);
        countrySelection.setConverter(model.getCountryStringConverter());
        countrySelection.setMaxWidth(Double.MAX_VALUE);

        // 3. Currency Selector
        currencySelection = new AutoCompleteComboBox<>(model.getCurrencyCodes(), Res.get("settings.language.region.currency"));
        currencySelection.setMinWidth(comboBoxMinWidth);
        currencySelection.setConverter(model.getCurrencyStringConverter());
        currencySelection.setMaxWidth(Double.MAX_VALUE);

        // --- SELECTORS ROW ---
        HBox.setHgrow(languageSelection, Priority.ALWAYS);
        HBox.setHgrow(countrySelection, Priority.ALWAYS);
        HBox.setHgrow(currencySelection, Priority.ALWAYS);
        HBox selectorsRow = new HBox(20, languageSelection, countrySelection, currencySelection);
        // selectorsRow.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(selectorsRow, new Insets(0, 5, 0, 5));

        // --- SUPPORTED LANGUAGES SECTION ---
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

        supportedLanguagesComboBox = new AutoCompleteComboBox<>(model.getSupportedLanguageTagsFilteredList(),
                Res.get("settings.language.supported.select"));
        supportedLanguagesComboBox.setMinWidth(150);
        supportedLanguagesComboBox.setMaxWidth(Double.MAX_VALUE);
        supportedLanguagesComboBox.setConverter(model.getLanguageStringConverter());
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

        ListView<String> supportedLanguageListView = new ListView<>(model.getSelectedSupportedLanguageTags());
        supportedLanguageListView.setCellFactory(getSupportedLanguageCellFactory(controller));
        supportedLanguageListView.setMinWidth(150);
        supportedLanguageGridPane.setMaxHeight(150);
        supportedLanguageGridPane.add(supportedLanguageListView, 1, ++rowIndex);

        VBox.setMargin(supportedLanguageGridPane, new Insets(0, 5, 0, 5));

        // --- FINAL UI ASSEMBLY ---
        VBox contentBox = new VBox(50);
        contentBox.getChildren().addAll(
                mainHeadline,
                SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()),
                selectorsRow,
                supportedLanguagesHeadline,
                SettingsViewUtils.getLineAfterHeadline(contentBox.getSpacing()),
                supportedLanguageGridPane
        );

        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        languageSelection.setOnChangeConfirmed(e -> {
            if (languageSelection.getSelectionModel().getSelectedItem() == null) {
                languageSelection.getSelectionModel().select(model.getSelectedLanguageTag().get());
                return;
            }
            controller.onSelectLanguageTag(languageSelection.getSelectionModel().getSelectedItem());
        });
        selectedLanguageTagPin = EasyBind.subscribe(model.getSelectedLanguageTag(), selectedLanguageTag -> {
            if (selectedLanguageTag != null) {
                languageSelection.getSelectionModel().select(selectedLanguageTag);
            }
        });

        countrySelection.getSelectionModel().select(model.getSelectedCountryCode());
        countrySelection.setOnChangeConfirmed(e -> {
            if (countrySelection.getSelectionModel().getSelectedItem() == null) {
                countrySelection.getSelectionModel().select(model.getSelectedCountryCode());
                return;
            }
            controller.onSelectCountryCode(countrySelection.getSelectionModel().getSelectedItem());
            currencySelection.getSelectionModel().select(model.getSelectedCurrencyCode());
        });

        currencySelection.getSelectionModel().select(model.getSelectedCurrencyCode());
        currencySelection.setOnChangeConfirmed(e -> {
            if (currencySelection.getSelectionModel().getSelectedItem() == null) {
                currencySelection.getSelectionModel().select(model.getSelectedCurrencyCode());
                return;
            }
            controller.onSelectCurrencyCode(currencySelection.getSelectionModel().getSelectedItem());
        });

        supportedLanguagesComboBox.getSelectionModel().select(model.getSelectedLSupportedLanguageTag().get());
        supportedLanguagesComboBox.setOnChangeConfirmed(e -> {
            if (supportedLanguagesComboBox.getSelectionModel().getSelectedItem() == null) {
                supportedLanguagesComboBox.getSelectionModel().select(model.getSelectedLSupportedLanguageTag().get());
                return;
            }
            controller.onSelectSupportedLanguage(supportedLanguagesComboBox.getSelectionModel().getSelectedItem());
        });

        getSelectedSupportedLanguageCodePin = EasyBind.subscribe(model.getSelectedLSupportedLanguageTag(),
                languageTag -> {
                    if (languageTag != null) {
                        supportedLanguagesComboBox.getSelectionModel().select(languageTag);
                    }
                });

        addLanguageButton.setOnAction(e -> {
            if (supportedLanguagesComboBox.validate()) {
                controller.onAddSupportedLanguage();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        getSelectedSupportedLanguageCodePin.unsubscribe();
        selectedLanguageTagPin.unsubscribe();
        addLanguageButton.setOnAction(null);
        languageSelection.setOnChangeConfirmed(null);
        countrySelection.setOnChangeConfirmed(null);
        currencySelection.setOnChangeConfirmed(null);
        supportedLanguagesComboBox.setOnChangeConfirmed(null);

        languageSelection.resetValidation();
        countrySelection.resetValidation();
        currencySelection.resetValidation();
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
                            label.setText(model.getLanguageStringConverter().toString(languageCode));
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
