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

package bisq.desktop.main.content.user.fiat_accounts.create.data.form;

import bisq.common.asset.FiatCurrency;
import bisq.common.asset.Asset;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
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
public class F2FFormView extends FormView<F2FFormModel, F2FFormController> {
    private final MaterialTextField city, contact;
    private final MaterialTextArea extraInfo;
    private final AutoCompleteComboBox<Country> countryComboBox;
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final Label countryErrorLabel, currencyErrorLabel;
    private final VBox countryVBox, currencyVBox;
    private Subscription selectedCountryPin, selectedCurrencyPin, selectedCurrencyFromModelPin,
            currencyCountryMismatchPin, runValidationPin;

    public F2FFormView(F2FFormModel model, F2FFormController controller) {
        super(model, controller);

        countryComboBox = new AutoCompleteComboBox<>(
                model.getAllCountries(),
                Res.get("paymentAccounts.country"),
                Res.get("paymentAccounts.createAccount.accountData.country.prompt")
        );
        countryComboBox.setPrefWidth(830 / 4d);

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
        countryVBox = new VBox(countryComboBox, countryErrorLabel);
        countryVBox.setAlignment(Pos.TOP_LEFT);


        currencyComboBox = new AutoCompleteComboBox<>(
                model.getCurrencies(),
                Res.get("paymentAccounts.currency"),
                Res.get("paymentAccounts.createAccount.accountData.currency.prompt")
        );
        currencyComboBox.setPrefWidth(830 / 4d);

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
        currencyVBox = new VBox(currencyComboBox, currencyErrorLabel);
        currencyVBox.setAlignment(Pos.TOP_LEFT);


        city = new MaterialTextField(Res.get("paymentAccounts.f2f.city"),
                Res.get("paymentAccounts.createAccount.accountData.f2f.city.prompt"));
        city.setMaxWidth(Double.MAX_VALUE);
        city.setValidators(model.getCityValidator());

        HBox.setHgrow(city, Priority.ALWAYS);
        HBox hBox = new HBox(10, countryVBox, currencyVBox, city);
        hBox.setAlignment(Pos.TOP_LEFT);

        contact = new MaterialTextField(Res.get("paymentAccounts.f2f.contact"),
                Res.get("paymentAccounts.createAccount.accountData.f2f.contact.prompt"));
        contact.setValidators(model.getContactValidator());

        extraInfo = new MaterialTextArea(Res.get("paymentAccounts.f2f.extraInfo"),
                Res.get("paymentAccounts.createAccount.accountData.f2f.extraInfo.prompt"));
        extraInfo.setFixedHeight(140);
        extraInfo.setValidators(model.getExtraInfoValidator());

        root.getChildren().addAll(hBox, contact, extraInfo);
    }

    @Override
    protected void onViewAttached() {
        if (StringUtils.isNotEmpty(model.getCity().get())) {
            city.setText(model.getCity().get());
            city.validate();
        }
        if (StringUtils.isNotEmpty(model.getContact().get())) {
            contact.setText(model.getContact().get());
            contact.validate();
        }
        if (StringUtils.isNotEmpty(model.getExtraInfo().get())) {
            extraInfo.setText(model.getExtraInfo().get());
            extraInfo.validate();
        }
        if (model.getSelectedCountry().get() != null) {
            countryComboBox.getSelectionModel().select(model.getSelectedCountry().get());
        }

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());
        countryVBox.prefHeightProperty().bindBidirectional(city.prefHeightProperty());
        currencyVBox.prefHeightProperty().bindBidirectional(city.prefHeightProperty());
        city.textProperty().bindBidirectional(model.getCity());
        contact.textProperty().bindBidirectional(model.getContact());
        extraInfo.textProperty().bindBidirectional(model.getExtraInfo());

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
        selectedCurrencyFromModelPin = EasyBind.subscribe(model.getSelectedCurrency(), selectedCurrency -> {
            if (selectedCurrency != null) {
                currencyComboBox.getSelectionModel().select(selectedCurrency);
            }
        });
        currencyCountryMismatchPin = EasyBind.subscribe(model.getCurrencyCountryMismatch(), currencyCountryMismatch -> {
            if (currencyCountryMismatch) {
                new Popup().owner(root)
                        .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                        .warning(Res.get("paymentAccounts.createAccount.accountData.currency.warn.currencyCountryMismatch"))
                        .closeButtonText(Res.get("confirmation.yes"))
                        .onClose(() -> controller.onCurrencyCountryMisMatchPopupClosed(false))
                        .actionButtonText(Res.get("confirmation.no"))
                        .onAction(() -> controller.onCurrencyCountryMisMatchPopupClosed(true))
                        .show();
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                city.validate();
                contact.validate();
                extraInfo.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        city.resetValidation();
        contact.resetValidation();
        extraInfo.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();
        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();
        countryVBox.prefHeightProperty().unbindBidirectional(city.prefHeightProperty());
        currencyVBox.prefHeightProperty().unbindBidirectional(city.prefHeightProperty());
        city.textProperty().unbindBidirectional(model.getCity());
        contact.textProperty().unbindBidirectional(model.getContact());
        extraInfo.textProperty().unbindBidirectional(model.getExtraInfo());

        selectedCountryPin.unsubscribe();
        selectedCurrencyPin.unsubscribe();
        selectedCurrencyFromModelPin.unsubscribe();
        currencyCountryMismatchPin.unsubscribe();
        runValidationPin.unsubscribe();
    }
}