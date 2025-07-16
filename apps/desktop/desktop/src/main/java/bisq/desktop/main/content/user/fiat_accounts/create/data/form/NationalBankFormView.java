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

import bisq.account.accounts.fiat.BankAccountType;
import bisq.common.currency.FiatCurrency;
import bisq.common.currency.Asset;
import bisq.common.locale.Country;
import bisq.common.locale.CountryRepository;
import bisq.common.util.StringUtils;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class NationalBankFormView extends FormView<NationalBankFormModel, NationalBankFormController> {
    private static final double WITH = 840;
    private static final double MIN_WITH = 840 / 3d;

    private final MaterialTextField holderName, holderId,
            bankName, bankId, branchId,
            accountNr, nationalAccountId;
    private final AutoCompleteComboBox<Country> countryComboBox;
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final AutoCompleteComboBox<BankAccountType> bankAccountTypeComboBox;
    private final Label countryErrorLabel, currencyErrorLabel, bankAccountTypeErrorLabel;
    private final VBox bankAccountTypeVBox;
    private final HBox countryAndCurrencyBox, holderHBox, bankHBox, accountHBox;
    private Subscription selectedCountryPin, selectedCurrencyPin, selectedCurrencyFromModelPin,
            currencyCountryMismatchPin, selectedAccountTypePin, runValidationPin;

    public NationalBankFormView(NationalBankFormModel model,
                                NationalBankFormController controller) {
        super(model, controller);

        countryComboBox = new AutoCompleteComboBox<>(
                model.getAllCountries(),
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

        countryAndCurrencyBox = new HBox(10, countryVBox, currencyVBox);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));

        holderId = new MaterialTextField();

        HBox.setHgrow(holderName, Priority.ALWAYS);
        HBox.setHgrow(holderId, Priority.ALWAYS);
        holderHBox = new HBox(10, holderName, holderId);

        bankName = new MaterialTextField(Res.get("paymentAccounts.bank.bankName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.bank.bankName"))));
        bankId = new MaterialTextField();
        branchId = new MaterialTextField();

        bankHBox = new HBox(10, bankName, bankId, branchId);

        accountNr = new MaterialTextField();
        accountNr.setValidators(model.getAccountNrValidator());
        nationalAccountId = new MaterialTextField();

        bankAccountTypeComboBox = new AutoCompleteComboBox<>(
                model.getBankAccountTypes(),
                Res.get("paymentAccounts.bank.bankAccountType"),
                Res.get("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt")
        );
        bankAccountTypeComboBox.setMaxWidth(Double.MAX_VALUE);
        bankAccountTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BankAccountType bankAccountType) {
                return Optional.ofNullable(bankAccountType)
                        .map(BankAccountType::toString)
                        .orElse("");
            }

            @Override
            public BankAccountType fromString(String string) {
                return null;
            }
        });
        bankAccountTypeErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected"));
        bankAccountTypeErrorLabel.setMouseTransparent(true);
        bankAccountTypeErrorLabel.getStyleClass().add("material-text-field-error");
        bankAccountTypeErrorLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(bankAccountTypeErrorLabel, new Insets(3.5, 0, 0, 16));
        bankAccountTypeVBox = new VBox(bankAccountTypeComboBox, bankAccountTypeErrorLabel);
        bankAccountTypeVBox.setAlignment(Pos.TOP_LEFT);
        bankAccountTypeVBox.setFillWidth(true);

        HBox.setHgrow(accountNr, Priority.ALWAYS);
        HBox.setHgrow(nationalAccountId, Priority.ALWAYS);
        HBox.setHgrow(bankAccountTypeComboBox, Priority.ALWAYS);
        accountHBox = new HBox(10, accountNr, nationalAccountId, bankAccountTypeVBox);

        root.getChildren().addAll(countryAndCurrencyBox, holderHBox, bankHBox, accountHBox);
    }

    @Override
    protected void onViewAttached() {
        countryComboBox.getSelectionModel().select(model.getSelectedCountry().get());
        currencyComboBox.getSelectionModel().select(model.getSelectedCurrency().get());
        bankAccountTypeComboBox.getSelectionModel().select(model.getSelectedBankAccountType().get());

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());
        bankAccountTypeErrorLabel.visibleProperty().bind(model.getBankAccountTypeErrorVisible());
        bankAccountTypeErrorLabel.managedProperty().bind(model.getBankAccountTypeErrorVisible());

        bankAccountTypeVBox.visibleProperty().bind(model.getIsBankAccountTypesVisible());
        bankAccountTypeVBox.managedProperty().bind(model.getIsBankAccountTypesVisible());

        holderId.visibleProperty().bind(model.getIsHolderIdVisible());
        holderId.managedProperty().bind(model.getIsHolderIdVisible());
        bankName.visibleProperty().bind(model.getIsBankNameVisible());
        bankName.managedProperty().bind(model.getIsBankNameVisible());
        bankId.visibleProperty().bind(model.getIsBankIdVisible());
        bankId.managedProperty().bind(model.getIsBankIdVisible());
        branchId.visibleProperty().bind(model.getIsBranchIdVisible());
        branchId.managedProperty().bind(model.getIsBranchIdVisible());
        nationalAccountId.visibleProperty().bind(model.getIsNationalAccountIdVisible());
        nationalAccountId.managedProperty().bind(model.getIsNationalAccountIdVisible());

        holderName.textProperty().bindBidirectional(model.getHolderName());

        holderId.textProperty().bindBidirectional(model.getHolderId());
        holderId.descriptionProperty().bind(model.getHolderIdDescription());
        holderId.promptTextProperty().bind(model.getHolderIdPrompt());

        bankName.textProperty().bindBidirectional(model.getBankName());

        bankId.textProperty().bindBidirectional(model.getBankId());
        bankId.descriptionProperty().bind(model.getBankIdDescription());
        bankId.promptTextProperty().bind(model.getBankIdPrompt());

        branchId.textProperty().bindBidirectional(model.getBranchId());
        branchId.descriptionProperty().bind(model.getBranchIdDescription());
        branchId.promptTextProperty().bind(model.getBranchIdPrompt());

        accountNr.textProperty().bindBidirectional(model.getAccountNr());
        accountNr.descriptionProperty().bind(model.getAccountNrDescription());
        accountNr.promptTextProperty().bind(model.getAccountNrPrompt());

        nationalAccountId.textProperty().bindBidirectional(model.getNationalAccountId());
        nationalAccountId.descriptionProperty().bind(model.getNationalAccountIdDescription());
        nationalAccountId.promptTextProperty().bind(model.getNationalAccountIdPrompt());

        selectedCountryPin = EasyBind.subscribe(countryComboBox.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                controller.onSelectCountry(selectedCountry);

                holderName.clearValidators();
                holderId.clearValidators();
                bankName.clearValidators();
                bankId.clearValidators();
                branchId.clearValidators();
                accountNr.resetValidation();
                nationalAccountId.clearValidators();

                UIThread.runOnNextRenderFrame(this::layoutHBoxes);
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

        selectedAccountTypePin = EasyBind.subscribe(bankAccountTypeComboBox.getSelectionModel().selectedItemProperty(),
                selectedAccountType -> {
                    if (selectedAccountType != null) {
                        controller.onSelectBankAccountType(selectedAccountType);
                    }
                });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.clearValidators();
                holderId.clearValidators();
                bankName.clearValidators();
                bankId.clearValidators();
                branchId.clearValidators();
                nationalAccountId.clearValidators();

                accountNr.validate();

                if (model.getUseValidation().get()) {
                    holderName.setValidators(model.getHolderNameValidator());
                    holderId.setValidators(model.getHolderIdValidator());
                    bankName.setValidators(model.getBankNameValidator());
                    bankId.setValidators(model.getBankIdValidator());
                    branchId.setValidators(model.getBranchIdValidator());
                    nationalAccountId.setValidators(model.getNationalAccountIdValidator());
                    holderName.validate();
                    holderId.validate();
                    bankName.validate();
                    bankId.validate();
                    branchId.validate();
                    nationalAccountId.validate();
                }

                controller.onValidationDone();
            }
        });

        layoutHBoxes();
    }

    @Override
    protected void onViewDetached() {
        holderName.resetValidation();
        holderId.resetValidation();
        bankName.resetValidation();
        bankId.resetValidation();
        branchId.resetValidation();
        accountNr.resetValidation();
        nationalAccountId.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();
        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();
        bankAccountTypeErrorLabel.visibleProperty().unbind();
        bankAccountTypeErrorLabel.managedProperty().unbind();

        bankAccountTypeVBox.visibleProperty().unbind();
        bankAccountTypeVBox.managedProperty().unbind();

        holderId.visibleProperty().unbind();
        holderId.managedProperty().unbind();
        bankName.visibleProperty().unbind();
        bankName.managedProperty().unbind();
        bankId.visibleProperty().unbind();
        bankId.managedProperty().unbind();
        branchId.visibleProperty().unbind();
        branchId.managedProperty().unbind();
        nationalAccountId.visibleProperty().unbind();
        nationalAccountId.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());

        holderId.textProperty().unbindBidirectional(model.getHolderId());
        holderId.descriptionProperty().unbind();
        holderId.promptTextProperty().unbind();

        bankName.textProperty().unbindBidirectional(model.getBankName());

        bankId.textProperty().unbindBidirectional(model.getBankId());
        bankId.descriptionProperty().unbind();
        bankId.promptTextProperty().unbind();

        branchId.textProperty().unbindBidirectional(model.getBranchId());
        branchId.descriptionProperty().unbind();
        branchId.promptTextProperty().unbind();

        accountNr.textProperty().unbindBidirectional(model.getAccountNr());
        accountNr.descriptionProperty().unbind();
        accountNr.promptTextProperty().unbind();

        nationalAccountId.textProperty().unbindBidirectional(model.getNationalAccountId());
        nationalAccountId.descriptionProperty().unbind();
        nationalAccountId.promptTextProperty().unbind();

        selectedCountryPin.unsubscribe();
        selectedCurrencyPin.unsubscribe();
        selectedCurrencyFromModelPin.unsubscribe();
        currencyCountryMismatchPin.unsubscribe();
        selectedAccountTypePin.unsubscribe();
        runValidationPin.unsubscribe();
    }

    private void layoutHBoxes() {
        // Layout with equal spacing
        Stream.of(countryAndCurrencyBox, holderHBox, bankHBox, accountHBox).forEach(hBox -> {
            double numVisible = hBox.getChildren().stream().filter(Node::isVisible).count();
            double sumSpace = 10 * (numVisible - 1);
            double width = (WITH - sumSpace) / numVisible;
            hBox.getChildren().forEach(child -> {
                if (child instanceof Region region) {
                    if (child.isVisible()) {
                        region.setMinWidth(width);
                        region.setMaxWidth(width);
                    }
                }
            });
        });
    }
}