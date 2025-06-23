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

package bisq.desktop.main.content.user.accounts.create.data.payment_form;

import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
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
public class F2FPaymentFormView extends PaymentFormView<F2FPaymentFormModel, F2FPaymentFormController> {
    private final MaterialTextField city, contact;
    private final MaterialTextArea extraInfo;
    private final AutoCompleteComboBox<Country> country;
    private final Label countryErrorLabel;
    private final VBox countryVBox;
    private Subscription selectedCountryPin, requireValidationPin;

    public F2FPaymentFormView(F2FPaymentFormModel model, F2FPaymentFormController controller) {
        super(model, controller);

        country = new AutoCompleteComboBox<>(
                model.getCountries(),
                Res.get("user.paymentAccounts.createAccount.accountData.country"),
                Res.get("user.paymentAccounts.createAccount.accountData.country.prompt")
        );
        country.setPrefWidth(240);
        country.setConverter(new StringConverter<>() {
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

        countryErrorLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.country.error"));
        countryErrorLabel.setMouseTransparent(true);
        countryErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(countryErrorLabel, new Insets(3.5, 0, 0, 16));
        countryVBox = new VBox(country, countryErrorLabel);
        countryVBox.setAlignment(Pos.TOP_LEFT);

        city = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.f2f.city"),
                Res.get("user.paymentAccounts.createAccount.accountData.f2f.city.prompt"));
        city.setPrefWidth(400);
        city.setValidators(model.getCityValidator());

        HBox.setHgrow(city, Priority.ALWAYS);
        HBox countryAndCityBox = new HBox(10, countryVBox, city);
        countryAndCityBox.setAlignment(Pos.TOP_LEFT);

        contact = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.f2f.contact"),
                Res.get("user.paymentAccounts.createAccount.accountData.f2f.contact.prompt"));
        contact.setValidators(model.getContactValidator());

        extraInfo = new MaterialTextArea(Res.get("user.paymentAccounts.createAccount.accountData.f2f.extraInfo"),
                Res.get("user.paymentAccounts.createAccount.accountData.f2f.extraInfo.prompt"));
        extraInfo.setFixedHeight(140);
        extraInfo.setValidators(model.getExtraInfoValidator());

        root.getChildren().addAll(countryAndCityBox, contact, extraInfo);
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
        if (model.getCountry().get() != null) {
            country.getSelectionModel().select(model.getCountry().get());
        }

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());

        countryVBox.prefHeightProperty().bindBidirectional(city.prefHeightProperty());

        city.textProperty().bindBidirectional(model.getCity());
        contact.textProperty().bindBidirectional(model.getContact());
        extraInfo.textProperty().bindBidirectional(model.getExtraInfo());

        selectedCountryPin = EasyBind.subscribe(country.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                model.getCountry().set(selectedCountry);
                model.getCountryErrorVisible().set(false);
            }
        });

        requireValidationPin = EasyBind.subscribe(model.getRequireValidation(), requireValidation -> {
            if (requireValidation) {
                city.validate();
                contact.validate();
                extraInfo.validate();
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

        countryVBox.prefHeightProperty().unbindBidirectional(city.prefHeightProperty());

        city.textProperty().unbindBidirectional(model.getCity());
        contact.textProperty().unbindBidirectional(model.getContact());
        extraInfo.textProperty().unbindBidirectional(model.getExtraInfo());

        selectedCountryPin.unsubscribe();
        requireValidationPin.unsubscribe();
    }
}