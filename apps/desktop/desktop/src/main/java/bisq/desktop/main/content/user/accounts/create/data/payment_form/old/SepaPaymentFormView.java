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

package bisq.desktop.main.content.user.accounts.create.data.payment_form.old;

import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SepaPaymentFormView extends PaymentFormView {

    private MaterialTextField holderNameField;
    private MaterialTextField ibanField;
    private MaterialTextField bicField;
    private AutoCompleteComboBox<Country> countryBox;

    private Label acceptedCountriesLabel;
    private CheckBox acceptAllCountriesCheckBox;
    private FlowPane specificCountriesContainer;
    private List<CheckBox> countryCheckBoxes;
    private Label countrySelectionHint;

    private Label holderNameErrorLabel;
    private Label ibanErrorLabel;
    private Label bicErrorLabel;
    private Label countryErrorLabel;
    private Label acceptedCountriesErrorLabel;

    private List<Country> sepaCountries;

    private ChangeListener<String> holderNameListener;
    private ChangeListener<String> ibanListener;
    private ChangeListener<String> bicListener;
    private ChangeListener<Boolean> acceptAllListener;
    private ChangeListener<Boolean> countryListener;

    private boolean updating = false;

    public SepaPaymentFormView(PaymentFormModel model, SepaPaymentFormController controller) {
        super(model, controller);
    }

    public void setHolderNameText(Optional<String> text) {
        setMaterialFieldText(holderNameField, text, holderNameListener);
    }

    public void setIbanText(Optional<String> text) {
        setMaterialFieldText(ibanField, text, ibanListener);
    }

    public void setBicText(Optional<String> text) {
        setMaterialFieldText(bicField, text, bicListener);
    }

    public void setSelectedCountry(Optional<Country> country) {
        Optional.ofNullable(countryBox)
                .filter(box -> country.map(c ->
                        !c.equals(box.getSelectionModel().getSelectedItem())).orElse(true))
                .ifPresent(box -> box.getSelectionModel().select(country.orElse(null)));
    }

    public void setAcceptedCountryCodes(List<String> acceptedCountryCodes) {
        Optional.ofNullable(acceptedCountryCodes)
                .filter(codes -> countryCheckBoxes != null && !countryCheckBoxes.isEmpty())
                .ifPresent(codes -> {
                    updating = true;
                    try {
                        List<String> allSepaCountries = FiatPaymentRailUtil.getSepaEuroCountries();
                        boolean acceptingAllCountries = new HashSet<>(codes).containsAll(allSepaCountries) &&
                                new HashSet<>(allSepaCountries).containsAll(codes);

                        Optional.ofNullable(acceptAllListener)
                                .ifPresent(listener ->
                                        acceptAllCountriesCheckBox.selectedProperty().removeListener(listener));

                        acceptAllCountriesCheckBox.setSelected(acceptingAllCountries);

                        Optional.ofNullable(acceptAllListener)
                                .ifPresent(listener ->
                                        acceptAllCountriesCheckBox.selectedProperty()
                                                .addListener(createWeakListener(listener, "acceptAll")));

                        for (CheckBox checkBox : countryCheckBoxes) {
                            Optional.ofNullable(countryListener)
                                    .ifPresent(listener ->
                                            checkBox.selectedProperty().removeListener(listener));

                            String countryCode = (String) checkBox.getUserData();
                            checkBox.setSelected(codes.contains(countryCode));

                            Optional.ofNullable(countryListener)
                                    .ifPresent(listener ->
                                            checkBox.selectedProperty()
                                                    .addListener(createWeakListener(listener, "country_" + countryCode)));
                        }

                        specificCountriesContainer.setVisible(!acceptingAllCountries);
                        specificCountriesContainer.setManaged(!acceptingAllCountries);
                        countrySelectionHint.setVisible(!acceptingAllCountries);
                        countrySelectionHint.setManaged(!acceptingAllCountries);
                    } finally {
                        updating = false;
                    }
                });
    }

    @Override
    protected Map<String, Runnable> getFieldErrorActions() {
        return Map.of(
                "acceptedCountries", () -> {
                    specificCountriesContainer.getStyleClass().remove("sepa-form-countries-container");
                    specificCountriesContainer.getStyleClass().add("sepa-form-countries-container-error");
                }
        );
    }

    @Override
    protected void setupForm() {
        initializeSepaCountryData();
        createFormFields();
        createErrorLabels();
        layoutFormComponents();
    }

    @Override
    protected void onViewAttached() {
        createAndAttachDataChangeListeners();
        attachCountrySelectionHandler();
        attachAcceptedCountriesListeners();
        getController().restoreViewFromFormData();
    }

    @Override
    protected void onViewDetached() {
        Optional.ofNullable(holderNameListener)
                .ifPresent(listener -> holderNameField.textProperty().removeListener(listener));

        Optional.ofNullable(ibanListener)
                .ifPresent(listener -> ibanField.textProperty().removeListener(listener));

        Optional.ofNullable(bicListener)
                .ifPresent(listener -> bicField.textProperty().removeListener(listener));

        Optional.ofNullable(acceptAllListener)
                .ifPresent(listener ->
                        acceptAllCountriesCheckBox.selectedProperty().removeListener(listener));

        Optional.ofNullable(countryListener)
                .ifPresent(listener -> countryCheckBoxes.forEach(cb ->
                        cb.selectedProperty().removeListener(listener)));

        countryBox.setOnChangeConfirmed(null);
        cleanupListeners();
    }

    private void initializeSepaCountryData() {
        List<String> sepaCountryCodes = FiatPaymentRailUtil.getSepaEuroCountries();
        sepaCountries = CountryRepository.getCountriesFromCodes(sepaCountryCodes);
    }

    private void createFormFields() {
        createCountrySelection();
        createHolderNameField();
        createIbanField();
        createBicField();
        createAcceptedCountriesSelection();
    }

    private void createCountrySelection() {
        countryBox = new AutoCompleteComboBox<>(
                FXCollections.observableArrayList(sepaCountries),
                Res.get("user.paymentAccounts.sepa.country")
        );

        countryBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Country country) {
                return Optional.ofNullable(country)
                        .map(Country::getName)
                        .orElse("");
            }

            @Override
            public Country fromString(String string) {
                return sepaCountries.stream()
                        .filter(country -> country.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
    }

    private void createHolderNameField() {
        holderNameField = new MaterialTextField(Res.get("user.paymentAccounts.sepa.holderName"));
    }

    private void createIbanField() {
        ibanField = new MaterialTextField(Res.get("user.paymentAccounts.sepa.iban"));
    }

    private void createBicField() {
        bicField = new MaterialTextField(Res.get("user.paymentAccounts.sepa.bic"));
    }

    private void createAcceptedCountriesSelection() {
        acceptedCountriesLabel = new Label(Res.get("user.paymentAccounts.sepa.acceptedCountries"));
        acceptedCountriesLabel.getStyleClass().addAll("bisq-text-2", "sepa-form-section-header");
        acceptAllCountriesCheckBox = new CheckBox(Res.get("user.paymentAccounts.sepa.acceptAllSepaCountries"));
        acceptAllCountriesCheckBox.setSelected(true);
        acceptAllCountriesCheckBox.getStyleClass().addAll("bisq-text-2", "sepa-form-accept-all-checkbox");

        countrySelectionHint = new Label(Res.get("user.paymentAccounts.sepa.acceptedCountriesHint"));
        countrySelectionHint.getStyleClass().addAll("bisq-text-3", "sepa-form-country-hint");
        countrySelectionHint.setWrapText(true);

        createSpecificCountriesContainer();
    }

    private void createSpecificCountriesContainer() {
        specificCountriesContainer = new FlowPane(4, 4);
        specificCountriesContainer.setPadding(new Insets(8));
        specificCountriesContainer.getStyleClass().add("sepa-form-countries-container");

        countryCheckBoxes = new ArrayList<>();

        for (Country country : sepaCountries) {
            CheckBox countryCheckBox = new CheckBox(country.getCode());
            countryCheckBox.setUserData(country.getCode());
            countryCheckBox.setSelected(true);

            countryCheckBox.getStyleClass().addAll("bisq-text-4", "sepa-form-country-checkbox");

            countryCheckBoxes.add(countryCheckBox);
            specificCountriesContainer.getChildren().add(countryCheckBox);
        }

        specificCountriesContainer.setVisible(false);
        specificCountriesContainer.setManaged(false);
        countrySelectionHint.setVisible(false);
        countrySelectionHint.setManaged(false);
    }

    private void createErrorLabels() {
        holderNameErrorLabel = createErrorLabel();
        ibanErrorLabel = createErrorLabel();
        bicErrorLabel = createErrorLabel();
        countryErrorLabel = createErrorLabel();
        acceptedCountriesErrorLabel = createErrorLabel();

        errorLabels.put("holderName", holderNameErrorLabel);
        errorLabels.put("iban", ibanErrorLabel);
        errorLabels.put("bic", bicErrorLabel);
        errorLabels.put("country", countryErrorLabel);
        errorLabels.put("acceptedCountries", acceptedCountriesErrorLabel);
    }

    private void layoutFormComponents() {
        HBox countryAndHolderFields = createCountryAndHolderFieldsLayout();
        HBox countryAndHolderErrors = createCountryAndHolderErrorsLayout();

        HBox ibanAndBicFields = createIbanAndBicFieldsLayout();
        HBox ibanAndBicErrors = createIbanAndBicErrorsLayout();

        VBox countryAndHolderCompleteRow = new VBox(1);
        countryAndHolderCompleteRow.getChildren().addAll(countryAndHolderFields, countryAndHolderErrors);

        VBox ibanAndBicCompleteRow = new VBox(1);
        ibanAndBicCompleteRow.getChildren().addAll(ibanAndBicFields, ibanAndBicErrors);

        VBox acceptedCountriesSection = createAcceptedCountriesSection();

        root.setSpacing(5);
        root.getChildren().addAll(
                countryAndHolderCompleteRow,
                ibanAndBicCompleteRow,
                acceptedCountriesSection
        );
    }

    private HBox createCountryAndHolderFieldsLayout() {
        HBox countryAndHolderFields = new HBox(8);
        countryAndHolderFields.setAlignment(Pos.CENTER_LEFT);

        countryBox.setPrefWidth(160);
        holderNameField.setPrefWidth(300);
        HBox.setHgrow(holderNameField, Priority.ALWAYS);

        countryAndHolderFields.getChildren().addAll(countryBox, holderNameField);
        return countryAndHolderFields;
    }

    private HBox createCountryAndHolderErrorsLayout() {
        HBox countryAndHolderErrors = new HBox(8);
        countryAndHolderErrors.setAlignment(Pos.CENTER_LEFT);

        VBox countryErrorContainer = new VBox();
        countryErrorContainer.setPrefWidth(160);
        countryErrorContainer.getChildren().add(countryErrorLabel);

        VBox holderNameErrorContainer = new VBox();
        HBox.setHgrow(holderNameErrorContainer, Priority.ALWAYS);
        holderNameErrorContainer.getChildren().add(holderNameErrorLabel);

        countryAndHolderErrors.getChildren().addAll(countryErrorContainer, holderNameErrorContainer);
        return countryAndHolderErrors;
    }

    private HBox createIbanAndBicFieldsLayout() {
        HBox ibanAndBicFields = new HBox(8);
        ibanAndBicFields.setAlignment(Pos.CENTER_LEFT);

        ibanField.setPrefWidth(300);
        bicField.setPrefWidth(280);
        HBox.setHgrow(ibanField, Priority.ALWAYS);

        ibanAndBicFields.getChildren().addAll(ibanField, bicField);
        return ibanAndBicFields;
    }

    private HBox createIbanAndBicErrorsLayout() {
        HBox ibanAndBicErrors = new HBox(8);
        ibanAndBicErrors.setAlignment(Pos.CENTER_LEFT);

        VBox ibanErrorContainer = new VBox();
        ibanErrorContainer.setPrefWidth(300);
        HBox.setHgrow(ibanErrorContainer, Priority.ALWAYS);
        ibanErrorContainer.getChildren().add(ibanErrorLabel);

        VBox bicErrorContainer = new VBox();
        bicErrorContainer.setPrefWidth(280);
        bicErrorContainer.getChildren().add(bicErrorLabel);

        ibanAndBicErrors.getChildren().addAll(ibanErrorContainer, bicErrorContainer);
        return ibanAndBicErrors;
    }

    private VBox createAcceptedCountriesSection() {
        VBox acceptedCountriesSection = new VBox(4);
        acceptedCountriesSection.getChildren().addAll(
                acceptedCountriesLabel,
                acceptAllCountriesCheckBox,
                countrySelectionHint,
                specificCountriesContainer,
                acceptedCountriesErrorLabel
        );
        acceptedCountriesSection.getStyleClass().add("sepa-form-accepted-countries-section");
        return acceptedCountriesSection;
    }

    private void createAndAttachDataChangeListeners() {
        holderNameListener = (obs, oldValue, newValue) -> {
            getController().onFieldChanged("holderName", newValue);
            clearFieldError("holderName");
        };

        ibanListener = (obs, oldValue, newValue) -> {
            getController().onFieldChanged("iban", newValue);
            clearFieldError("iban");
        };

        bicListener = (obs, oldValue, newValue) -> {
            getController().onFieldChanged("bic", newValue);
            clearFieldError("bic");
        };

        holderNameField.textProperty().addListener(createWeakListener(holderNameListener, "holderName"));
        ibanField.textProperty().addListener(createWeakListener(ibanListener, "iban"));
        bicField.textProperty().addListener(createWeakListener(bicListener, "bic"));
    }

    private void attachCountrySelectionHandler() {
        countryBox.setOnChangeConfirmed(event -> {
            Country selectedCountry = countryBox.getSelectionModel().getSelectedItem();
            getController().onFieldChanged("country", selectedCountry);
            clearFieldError("country");
            clearFieldError("iban");

            Optional.ofNullable(selectedCountry)
                    .ifPresent(country -> Platform.runLater(() -> holderNameField.requestFocusWithCursor()));
        });
    }

    private void attachAcceptedCountriesListeners() {
        acceptAllListener = (obs, oldValue, newValue) -> {
            if (updating) {
                return;
            }

            updating = true;
            try {
                specificCountriesContainer.setVisible(!newValue);
                specificCountriesContainer.setManaged(!newValue);
                countrySelectionHint.setVisible(!newValue);
                countrySelectionHint.setManaged(!newValue);

                if (newValue) {
                    countryCheckBoxes.forEach(cb -> cb.setSelected(true));
                }

                updateAcceptedCountriesData();
                clearFieldError("acceptedCountries");
            } finally {
                updating = false;
            }
        };

        countryListener = (obs, oldValue, newValue) -> {
            if (updating) {
                return;
            }

            updating = true;
            try {
                updateAcceptedCountriesData();
                clearFieldError("acceptedCountries");

                boolean allSelected = countryCheckBoxes.stream().allMatch(CheckBox::isSelected);
                if (allSelected && !acceptAllCountriesCheckBox.isSelected()) {
                    acceptAllCountriesCheckBox.setSelected(true);
                    specificCountriesContainer.setVisible(false);
                    specificCountriesContainer.setManaged(false);
                    countrySelectionHint.setVisible(false);
                    countrySelectionHint.setManaged(false);
                } else if (!allSelected && acceptAllCountriesCheckBox.isSelected()) {
                    acceptAllCountriesCheckBox.setSelected(false);
                    specificCountriesContainer.setVisible(true);
                    specificCountriesContainer.setManaged(true);
                    countrySelectionHint.setVisible(true);
                    countrySelectionHint.setManaged(true);
                }
            } finally {
                updating = false;
            }
        };

        acceptAllCountriesCheckBox.selectedProperty().addListener(
                createWeakListener(acceptAllListener, "acceptAll"));

        for (int i = 0; i < countryCheckBoxes.size(); i++) {
            CheckBox checkBox = countryCheckBoxes.get(i);
            String key = "country_" + i;
            checkBox.selectedProperty().addListener(
                    createWeakListener(countryListener, key));
        }

        updateAcceptedCountriesData();
    }

    private void updateAcceptedCountriesData() {
        List<String> acceptedCodes;

        if (acceptAllCountriesCheckBox.isSelected()) {
            acceptedCodes = FiatPaymentRailUtil.getSepaEuroCountries();
        } else {
            acceptedCodes = countryCheckBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(cb -> (String) cb.getUserData())
                    .collect(Collectors.toList());
        }

        getController().onFieldChanged("acceptedCountryCodes", acceptedCodes);
    }

    private SepaPaymentFormController getController() {
        return (SepaPaymentFormController) controller;
    }
}