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
public class AmazonGiftCardFormView extends FormView<AmazonGiftCardFormModel, AmazonGiftCardFormController> {
    private final AutoCompleteComboBox<Country> countryComboBox;
    private final MaterialTextField emailOrMobileNr;
    private final Label countryErrorLabel;
    private Subscription selectedCountryPin, runValidationPin;

    public AmazonGiftCardFormView(AmazonGiftCardFormModel model, AmazonGiftCardFormController controller) {
        super(model, controller);

        countryComboBox = new AutoCompleteComboBox<>(
                model.getCountries(),
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
        VBox countryVBox = new VBox(countryComboBox, countryErrorLabel);
        countryVBox.setAlignment(Pos.TOP_LEFT);

        emailOrMobileNr = new MaterialTextField(Res.get("paymentAccounts.emailOrMobileNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.emailOrMobileNr"))));
        emailOrMobileNr.setValidators(model.getEmailOrMobileNrValidator());
        emailOrMobileNr.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(emailOrMobileNr, Priority.ALWAYS);
        HBox hBox = new HBox(10, countryVBox, emailOrMobileNr);
        content.getChildren().add(hBox);
        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.amazonGiftCard"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        countryComboBox.getSelectionModel().select(model.getSelectedCountry().get());
        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());

        if (StringUtils.isNotEmpty(model.getEmailOrMobileNr().get())) {
            emailOrMobileNr.setText(model.getEmailOrMobileNr().get());
            emailOrMobileNr.validate();
        }

        emailOrMobileNr.textProperty().bindBidirectional(model.getEmailOrMobileNr());

        selectedCountryPin = EasyBind.subscribe(countryComboBox.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                controller.onCountrySelected(selectedCountry);
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                emailOrMobileNr.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        emailOrMobileNr.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();

        emailOrMobileNr.textProperty().unbindBidirectional(model.getEmailOrMobileNr());

        selectedCountryPin.unsubscribe();
        runValidationPin.unsubscribe();
    }
}
