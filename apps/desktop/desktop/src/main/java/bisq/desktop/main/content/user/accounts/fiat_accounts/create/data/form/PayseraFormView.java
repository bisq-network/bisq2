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
public class PayseraFormView extends FormView<PayseraFormModel, PayseraFormController> {
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final MaterialTextField email;
    private final Label currencyErrorLabel;
    private Subscription selectedCurrencyPin, runValidationPin;

    public PayseraFormView(PayseraFormModel model, PayseraFormController controller) {
        super(model, controller);

        currencyComboBox = new AutoCompleteComboBox<>(
                model.getCurrencies(),
                Res.get("paymentAccounts.currency"),
                Res.get("paymentAccounts.createAccount.accountData.currency.prompt")
        );
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

        email = new MaterialTextField(Res.get("paymentAccounts.email"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.email"))));
        email.setValidators(model.getEmailValidator());
        email.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(email, Priority.ALWAYS);
        HBox hBox = new HBox(10, currencyVBox, email);
        content.getChildren().add(hBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        currencyComboBox.getSelectionModel().select(model.getSelectedCurrency().get());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());

        if (StringUtils.isNotEmpty(model.getEmail().get())) {
            email.setText(model.getEmail().get());
            email.validate();
        }

        email.textProperty().bindBidirectional(model.getEmail());

        selectedCurrencyPin = EasyBind.subscribe(currencyComboBox.getSelectionModel().selectedItemProperty(), selectedCurrency -> {
            if (selectedCurrency != null) {
                model.getSelectedCurrency().set(selectedCurrency);
                controller.onSelectCurrency();
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                email.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        email.resetValidation();

        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();

        email.textProperty().unbindBidirectional(model.getEmail());

        selectedCurrencyPin.unsubscribe();
        runValidationPin.unsubscribe();
    }
}
