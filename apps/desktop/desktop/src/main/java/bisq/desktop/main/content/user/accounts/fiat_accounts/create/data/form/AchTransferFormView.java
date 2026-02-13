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

import bisq.account.accounts.fiat.BankAccountType;
import bisq.account.accounts.util.BankAccountUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;

@Slf4j
public class AchTransferFormView extends FormView<AchTransferFormModel, AchTransferFormController> {
    private static final String ACH_COUNTRY_CODE = "US";

    private final MaterialTextField holderName, bankName, bankId, accountNr;
    private final MaterialTextArea holderAddress;
    private final AutoCompleteComboBox<BankAccountType> bankAccountTypeComboBox;
    private final Label bankAccountTypeErrorLabel;
    private final VBox bankAccountTypeVBox;
    private Subscription runValidationPin, selectedAccountTypePin, holderAddressHeightPin;

    public AchTransferFormView(AchTransferFormModel model,
                               AchTransferFormController controller) {
        super(model, controller);

        VBox.setMargin(titleLabel, new Insets(30, 0, 10, 0));

        holderName = new MaterialTextField(
                Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.holderName")))
        );
        holderName.setValidators(model.getHolderNameValidator());

        accountNr = new MaterialTextField(
                model.getAccountNrDescription().get(),
                model.getAccountNrPrompt().get()
        );
        accountNr.setValidators(model.getAccountNrValidator());

        bankAccountTypeComboBox = new AutoCompleteComboBox<>(
                model.getBankAccountTypes(),
                Res.get("paymentAccounts.bank.bankAccountType"),
                Res.get("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt")
        );
        bankAccountTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BankAccountType type) {
                return Optional.ofNullable(type)
                        .map(BankAccountType::toDisplayString)
                        .orElse("");
            }

            @Override
            public BankAccountType fromString(String string) {
                return null;
            }
        });

        bankAccountTypeErrorLabel = new Label(
                Res.get("paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected")
        );
        bankAccountTypeErrorLabel.setMouseTransparent(true);
        bankAccountTypeErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(bankAccountTypeErrorLabel, new Insets(3.5, 0, 0, 16));

        bankAccountTypeVBox = new VBox(bankAccountTypeComboBox, bankAccountTypeErrorLabel);

        holderAddress = new MaterialTextArea(
                Res.get("paymentAccounts.holderAddress"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.holderAddress")))
        );
        holderAddress.setValidators(model.getHolderAddressValidator());

        bankName = new MaterialTextField(
                Res.get("paymentAccounts.bank.bankName"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.bank.bankName")))
        );
        bankName.setValidators(model.getBankNameValidator());

        String bankIdDescription = BankAccountUtils.getBankIdDescription(ACH_COUNTRY_CODE);
        bankId = new MaterialTextField(
                bankIdDescription,
                BankAccountUtils.getPrompt(ACH_COUNTRY_CODE, bankIdDescription)
        );
        bankId.setValidators(model.getBankIdValidator());

        // --- Grid Layout ---

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        // Two equal-width columns
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        // Left column: account data VBox
        VBox accountDataVBox = new VBox(10, holderName, accountNr, bankAccountTypeVBox);
        accountDataVBox.setMaxWidth(Double.MAX_VALUE);

        // Ensure children stretch
        holderName.setMaxWidth(Double.MAX_VALUE);
        accountNr.setMaxWidth(Double.MAX_VALUE);
        bankAccountTypeComboBox.setMaxWidth(Double.MAX_VALUE);

        // Right column: address
        holderAddress.setMaxWidth(Double.MAX_VALUE);

        // Row 0: Account data + Address
        grid.add(accountDataVBox, 0, 0);
        grid.add(holderAddress, 1, 0);

        GridPane.setHgrow(accountDataVBox, Priority.ALWAYS);
        GridPane.setHgrow(holderAddress, Priority.ALWAYS);

        // Row 1: Bank name + Bank ID
        bankName.setMaxWidth(Double.MAX_VALUE);
        bankId.setMaxWidth(Double.MAX_VALUE);

        grid.add(bankName, 0, 1);
        grid.add(bankId, 1, 1);

        GridPane.setHgrow(bankName, Priority.ALWAYS);
        GridPane.setHgrow(bankId, Priority.ALWAYS);

        // Add to content
        content.getChildren().add(grid);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        holderName.textProperty().bindBidirectional(model.getHolderName());
        holderAddress.textProperty().bindBidirectional(model.getHolderAddress());
        bankName.textProperty().bindBidirectional(model.getBankName());
        bankId.textProperty().bindBidirectional(model.getBankId());
        accountNr.textProperty().bindBidirectional(model.getAccountNr());

        bankAccountTypeComboBox.getSelectionModel().select(model.getSelectedBankAccountType().get());
        bankAccountTypeErrorLabel.visibleProperty().bind(model.getBankAccountTypeErrorVisible());
        bankAccountTypeErrorLabel.managedProperty().bind(model.getBankAccountTypeErrorVisible());

        selectedAccountTypePin = EasyBind.subscribe(bankAccountTypeComboBox.getSelectionModel().selectedItemProperty(),
                selectedAccountType -> {
                    if (selectedAccountType != null) {
                        controller.onSelectBankAccountType(selectedAccountType);
                    }
                });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                holderAddress.validate();
                bankName.validate();
                bankId.validate();
                accountNr.validate();
                controller.onValidationDone();
            }
        });

        holderAddressHeightPin = EasyBind.subscribe(EasyBind.combine(holderName.heightProperty(),
                        accountNr.heightProperty(),
                        bankAccountTypeVBox.heightProperty(),
                        (a, b, c) ->
                                20 + a.doubleValue() + b.doubleValue() + c.doubleValue()),
                holderAddress::setFixedHeight);
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        holderAddress.resetValidation();
        bankName.resetValidation();
        bankId.resetValidation();
        accountNr.resetValidation();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        holderAddress.textProperty().unbindBidirectional(model.getHolderAddress());
        bankName.textProperty().unbindBidirectional(model.getBankName());
        bankId.textProperty().unbindBidirectional(model.getBankId());
        accountNr.textProperty().unbindBidirectional(model.getAccountNr());

        bankAccountTypeErrorLabel.visibleProperty().unbind();
        bankAccountTypeErrorLabel.managedProperty().unbind();

        selectedAccountTypePin.unsubscribe();
        runValidationPin.unsubscribe();
        holderAddressHeightPin.unsubscribe();
    }
}
