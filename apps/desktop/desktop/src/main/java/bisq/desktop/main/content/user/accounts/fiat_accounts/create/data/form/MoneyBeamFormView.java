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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data.form;

import bisq.common.asset.Asset;
import bisq.common.asset.FiatCurrency;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class MoneyBeamFormView extends FormView<MoneyBeamFormModel, MoneyBeamFormController> {
    private final AutoCompleteComboBox<Country> countryComboBox;
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final MaterialTextField holderName, emailOrMobileNr;
    private final Label countryErrorLabel, currencyErrorLabel;
    private Subscription selectedCountryPin, selectedCurrencyPin, runValidationPin;

    public MoneyBeamFormView(MoneyBeamFormModel model, MoneyBeamFormController controller) {
        super(model, controller);

        countryComboBox = new AutoCompleteComboBox<>(
                model.getCountries(),
                Res.get("paymentAccounts.country"),
                Res.get("paymentAccounts.createAccount.accountData.country.prompt")
        );
        countryComboBox.setMaxWidth(Double.MAX_VALUE);
        countryComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Country country) {
                return Optional.ofNullable(country)
                        .map(Country::getName)
                        .orElse("");
            }

            @Override
            public Country fromString(String string) {
                return CountryRepository.getAllCountries().stream()
                        .filter(country -> country.getName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });

        countryErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.country.error"));
        countryErrorLabel.setMouseTransparent(true);
        countryErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(countryErrorLabel, new Insets(3.5, 0, 0, 16));
        VBox countryVBox = new VBox(countryComboBox, countryErrorLabel);
        countryVBox.setAlignment(Pos.TOP_LEFT);

        currencyComboBox = new AutoCompleteComboBox<>(
                model.getCurrencies(),
                Res.get("paymentAccounts.currency"),
                Res.get("paymentAccounts.createAccount.accountData.currency.prompt")
        );
        currencyComboBox.setMaxWidth(Double.MAX_VALUE);
        currencyComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FiatCurrency currency) {
                return Optional.ofNullable(currency)
                        .map(Asset::getDisplayNameAndCode)
                        .orElse("");
            }

            @Override
            public FiatCurrency fromString(String string) {
                return null;
            }
        });

        currencyErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.currency.error.noneSelected"));
        currencyErrorLabel.setMouseTransparent(true);
        currencyErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(currencyErrorLabel, new Insets(3.5, 0, 0, 16));
        VBox currencyVBox = new VBox(currencyComboBox, currencyErrorLabel);
        currencyVBox.setAlignment(Pos.TOP_LEFT);

        HBox countryCurrencyBox = new HBox(10, countryVBox, currencyVBox);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        emailOrMobileNr = new MaterialTextField(Res.get("paymentAccounts.emailOrMobileNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.emailOrMobileNr"))));
        emailOrMobileNr.setValidators(model.getEmailOrMobileNrValidator());
        emailOrMobileNr.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(countryComboBox, Priority.ALWAYS);
        HBox.setHgrow(currencyComboBox, Priority.ALWAYS);
        HBox.setHgrow(holderName, Priority.ALWAYS);
        HBox.setHgrow(emailOrMobileNr, Priority.ALWAYS);

        VBox.setMargin(countryCurrencyBox, new Insets(0, 0, 10, 0));
        VBox.setMargin(holderName, new Insets(0, 0, 10, 0));
        content.getChildren().addAll(countryCurrencyBox, holderName, emailOrMobileNr);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        countryComboBox.getSelectionModel().select(model.getSelectedCountry().get());
        currencyComboBox.getSelectionModel().select(model.getSelectedCurrency().get());

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());

        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getEmailOrMobileNr().get())) {
            emailOrMobileNr.setText(model.getEmailOrMobileNr().get());
            emailOrMobileNr.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        emailOrMobileNr.textProperty().bindBidirectional(model.getEmailOrMobileNr());

        selectedCountryPin = EasyBind.subscribe(countryComboBox.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                controller.onSelectCountry(selectedCountry);
            }
        });

        selectedCurrencyPin = EasyBind.subscribe(currencyComboBox.getSelectionModel().selectedItemProperty(), selectedCurrency -> {
            if (selectedCurrency != null) {
                controller.onSelectCurrency(selectedCurrency);
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                emailOrMobileNr.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        emailOrMobileNr.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();
        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        emailOrMobileNr.textProperty().unbindBidirectional(model.getEmailOrMobileNr());

        selectedCountryPin.unsubscribe();
        selectedCurrencyPin.unsubscribe();
        runValidationPin.unsubscribe();
    }
}
