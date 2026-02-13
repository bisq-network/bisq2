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
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class CashByMailFormView extends FormView<CashByMailFormModel, CashByMailFormController> {
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final Label currencyErrorLabel;
    private final MaterialTextArea postalAddress;
    private final MaterialTextField contact;
    private final MaterialTextArea extraInfo;
    private Subscription runValidationPin, selectedCurrencyPin;

    public CashByMailFormView(CashByMailFormModel model, CashByMailFormController controller) {
        super(model, controller);

        VBox.setMargin(titleLabel, new Insets(10, 0, 5, 0));

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
        currencyComboBox.setMaxWidth(Double.MAX_VALUE);

        currencyErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.currency.error.noneSelected"));
        currencyErrorLabel.setMouseTransparent(true);
        currencyErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(currencyErrorLabel, new Insets(3.5, 0, 0, 16));
        VBox currencyVBox = new VBox(currencyComboBox, currencyErrorLabel);
        currencyVBox.setAlignment(Pos.TOP_LEFT);

        contact = new MaterialTextField(Res.get("paymentAccounts.cashByMail.contact"),
                Res.get("paymentAccounts.cashByMail.contact.prompt"));
        contact.setValidators(model.getContactValidator());
        contact.setMaxWidth(Double.MAX_VALUE);

        postalAddress = new MaterialTextArea(Res.get("paymentAccounts.postalAddress"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.postalAddress"))));
        postalAddress.setValidators(model.getPostalAddressValidator());
        postalAddress.setFixedHeight(120);

        extraInfo = new MaterialTextArea(Res.get("paymentAccounts.cashByMail.extraInfo"),
                Res.get("paymentAccounts.cashByMail.extraInfo.prompt"));
        extraInfo.setValidators(model.getExtraInfoValidator());
        extraInfo.setFixedHeight(120);

        content.getChildren().addAll(currencyVBox, contact, postalAddress, extraInfo);
        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.cashByMail.headline"),
                Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.cashByMail"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (StringUtils.isNotEmpty(model.getPostalAddress().get())) {
            postalAddress.setText(model.getPostalAddress().get());
            postalAddress.validate();
        }
        if (StringUtils.isNotEmpty(model.getContact().get())) {
            contact.setText(model.getContact().get());
            contact.validate();
        }
        if (StringUtils.isNotEmpty(model.getExtraInfo().get())) {
            extraInfo.setText(model.getExtraInfo().get());
            extraInfo.validate();
        }

        currencyComboBox.getSelectionModel().select(model.getSelectedCurrency().get());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());

        postalAddress.textProperty().bindBidirectional(model.getPostalAddress());
        contact.textProperty().bindBidirectional(model.getContact());
        extraInfo.textProperty().bindBidirectional(model.getExtraInfo());

        selectedCurrencyPin = EasyBind.subscribe(currencyComboBox.getSelectionModel().selectedItemProperty(), selectedCurrency -> {
            if (selectedCurrency != null) {
                model.getSelectedCurrency().set(selectedCurrency);
                controller.onSelectCurrency();
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                postalAddress.validate();
                contact.validate();
                extraInfo.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        postalAddress.resetValidation();
        contact.resetValidation();
        extraInfo.resetValidation();

        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();

        postalAddress.textProperty().unbindBidirectional(model.getPostalAddress());
        contact.textProperty().unbindBidirectional(model.getContact());
        extraInfo.textProperty().unbindBidirectional(model.getExtraInfo());

        selectedCurrencyPin.unsubscribe();
        runValidationPin.unsubscribe();
    }
}
