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

import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class F2FPaymentFormView extends PaymentFormView {
    private MaterialTextField cityField;
    private MaterialTextField contactField;
    private MaterialTextArea extraInfoField;
    private AutoCompleteComboBox<Country> countryBox;

    private ChangeListener<String> cityListener;
    private ChangeListener<String> contactListener;
    private ChangeListener<String> extraInfoListener;

    public F2FPaymentFormView(PaymentFormModel model, F2FPaymentFormController controller) {
        super(model, controller);
    }

    public void setCityText(Optional<String> text) {
        setMaterialFieldText(cityField, text, cityListener);
    }

    public void setContactText(Optional<String> text) {
        setMaterialFieldText(contactField, text, contactListener);
    }

    public void setExtraInfoText(Optional<String> text) {
        setMaterialFieldText(extraInfoField, text, extraInfoListener);
    }

    public void setSelectedCountry(Optional<Country> country) {
        Optional.ofNullable(countryBox)
                .filter(box -> country.map(c ->
                        !c.equals(box.getSelectionModel().getSelectedItem())).orElse(true))
                .ifPresent(box ->
                        box.getSelectionModel().select(country.orElse(null)));
    }

    @Override
    protected void setupForm() {
        countryBox = new AutoCompleteComboBox<>(
                FXCollections.observableArrayList(CountryRepository.getCountries()),
                Res.get("user.paymentAccounts.createAccount.accountData.country")
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
                return CountryRepository.getCountries().stream()
                        .filter(country -> country.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        cityField = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.f2f.city"));
        cityField.setPromptText(Res.get("user.paymentAccounts.createAccount.accountData.f2f.city.prompt"));

        HBox countryAndCityBox = new HBox(10);
        countryAndCityBox.setAlignment(Pos.CENTER_LEFT);

        countryBox.setPrefWidth(240);
        cityField.setPrefWidth(400);
        HBox.setHgrow(cityField, Priority.ALWAYS);
        countryAndCityBox.getChildren().addAll(countryBox, cityField);

        contactField = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.f2f.contact"));
        contactField.setPromptText(Res.get("user.paymentAccounts.createAccount.accountData.f2f.contact.prompt"));

        extraInfoField = new MaterialTextArea(Res.get("user.paymentAccounts.createAccount.accountData.f2f.extraInfo"));
        extraInfoField.setPromptText(Res.get("user.paymentAccounts.createAccount.accountData.f2f.extraInfo.prompt"));
        extraInfoField.setFixedHeight(100);

        Label cityErrorLabel = createErrorLabel();
        Label contactErrorLabel = createErrorLabel();
        Label extraInfoErrorLabel = createErrorLabel();
        Label countryErrorLabel = createErrorLabel();

        errorLabels.put("city", cityErrorLabel);
        errorLabels.put("contact", contactErrorLabel);
        errorLabels.put("extraInfo", extraInfoErrorLabel);
        errorLabels.put("country", countryErrorLabel);

        VBox countryErrorContainer = new VBox(2);
        countryErrorContainer.getChildren().add(countryErrorLabel);

        VBox cityErrorContainer = new VBox(2);
        cityErrorContainer.getChildren().add(cityErrorLabel);

        HBox countryAndCityErrors = new HBox(10);
        countryErrorContainer.setPrefWidth(240);
        cityErrorContainer.setPrefWidth(400);
        HBox.setHgrow(cityErrorContainer, Priority.ALWAYS);
        countryAndCityErrors.getChildren().addAll(countryErrorContainer, cityErrorContainer);

        VBox countryAndCityCompleteRow = new VBox(1);
        countryAndCityCompleteRow.getChildren().addAll(countryAndCityBox, countryAndCityErrors);

        VBox contactWithError = new VBox(2);
        contactWithError.getChildren().addAll(contactField, contactErrorLabel);

        VBox extraInfoWithError = new VBox(2);
        extraInfoWithError.getChildren().addAll(extraInfoField, extraInfoErrorLabel);

        root.getChildren().addAll(countryAndCityCompleteRow, contactWithError, extraInfoWithError);
    }

    @Override
    protected void onViewAttached() {
        cityListener = (obs, old, newValue) -> {
            controller.onFieldChanged("city", newValue);
            clearFieldError("city");
        };

        contactListener = (obs, old, newValue) -> {
            controller.onFieldChanged("contact", newValue);
            clearFieldError("contact");
        };

        extraInfoListener = (obs, old, newValue) -> {
            controller.onFieldChanged("extraInfo", newValue);
            clearFieldError("extraInfo");
        };

        cityField.textProperty().addListener(createWeakListener(cityListener, "city"));
        contactField.textProperty().addListener(createWeakListener(contactListener, "contact"));
        extraInfoField.textProperty().addListener(createWeakListener(extraInfoListener, "extraInfo"));

        countryBox.setOnChangeConfirmed(event -> {
            Country selectedCountry = countryBox.getSelectionModel().getSelectedItem();
            controller.onFieldChanged("country", selectedCountry);
            clearFieldError("country");
        });

        controller.restoreViewFromFormData();
        clearAllErrors();
    }

    @Override
    protected void onViewDetached() {
        Optional.ofNullable(cityListener)
                .ifPresent(listener -> cityField.textProperty().removeListener(listener));

        Optional.ofNullable(contactListener)
                .ifPresent(listener -> contactField.textProperty().removeListener(listener));

        Optional.ofNullable(extraInfoListener)
                .ifPresent(listener -> extraInfoField.textProperty().removeListener(listener));

        countryBox.setOnChangeConfirmed(null);
        cleanupListeners();
    }
}