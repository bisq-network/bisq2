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
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class SwiftFormView extends FormView<SwiftFormModel, SwiftFormController> {
    private final AutoCompleteComboBox<Country> bankCountryComboBox, intermediaryBankCountryComboBox;
    private final AutoCompleteComboBox<FiatCurrency> currencyComboBox;
    private final Label bankCountryErrorLabel, currencyErrorLabel, intermediaryBankCountryErrorLabel;
    private final VBox bankCountryVBox, currencyVBox, intermediaryBankCountryVBox;

    private final MaterialTextField beneficiaryName, beneficiaryAccountNr, beneficiaryPhone;
    private final MaterialTextArea beneficiaryAddress, additionalInstructions;

    private final MaterialTextField bankSwiftCode, bankName, bankBranch;
    private final MaterialTextArea bankAddress;

    private final CheckBox useIntermediaryBank;

    private final MaterialTextField intermediaryBankSwiftCode, intermediaryBankName, intermediaryBankBranch;
    private final MaterialTextArea intermediaryBankAddress;

    private final List<javafx.scene.Node> intermediaryNodes;
    private final Set<Subscription> subscriptions = new HashSet<>();

    private boolean currencyCountryMismatchPopupShown = false;

    public SwiftFormView(SwiftFormModel model, SwiftFormController controller) {
        super(model, controller);

        bankCountryComboBox = new AutoCompleteComboBox<>(
                model.getBankCountries(),
                Res.get("paymentAccounts.swift.bankCountry"),
                Res.get("paymentAccounts.createAccount.accountData.country.prompt")
        );
        bankCountryComboBox.setPrefWidth(830 / 2d);
        bankCountryComboBox.setMaxWidth(Double.MAX_VALUE);
        bankCountryComboBox.setConverter(new StringConverter<>() {
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

        bankCountryErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.country.error"));
        bankCountryErrorLabel.setMouseTransparent(true);
        bankCountryErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(bankCountryErrorLabel, new Insets(3.5, 0, 0, 16));
        bankCountryVBox = new VBox(bankCountryComboBox, bankCountryErrorLabel);
        bankCountryVBox.setAlignment(Pos.TOP_LEFT);

        currencyComboBox = new AutoCompleteComboBox<>(
                model.getCurrencies(),
                Res.get("paymentAccounts.currency"),
                Res.get("paymentAccounts.createAccount.accountData.currency.prompt")
        );
        currencyComboBox.setPrefWidth(830 / 2d);
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
        currencyVBox = new VBox(currencyComboBox, currencyErrorLabel);
        currencyVBox.setAlignment(Pos.TOP_LEFT);

        beneficiaryName = new MaterialTextField(Res.get("paymentAccounts.swift.beneficiaryName"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.beneficiaryName"))));
        beneficiaryName.setValidators(model.getBeneficiaryNameValidator());

        beneficiaryAccountNr = new MaterialTextField(Res.get("paymentAccounts.swift.beneficiaryAccountNr"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.beneficiaryAccountNr"))));
        beneficiaryAccountNr.setValidators(model.getBeneficiaryAccountNrValidator());

        beneficiaryPhone = new MaterialTextField(Res.get("paymentAccounts.swift.beneficiaryPhone"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.beneficiaryPhone"))));
        beneficiaryPhone.setValidators(model.getBeneficiaryPhoneValidator());

        beneficiaryAddress = new MaterialTextArea(Res.get("paymentAccounts.swift.beneficiaryAddress"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.beneficiaryAddress"))));
        beneficiaryAddress.setFixedHeight(90);
        beneficiaryAddress.setValidators(model.getBeneficiaryAddressValidator());

        bankSwiftCode = new MaterialTextField(Res.get("paymentAccounts.swift.bankSwiftCode"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.bankSwiftCode"))));
        bankSwiftCode.setValidators(model.getBankSwiftCodeValidator());

        bankName = new MaterialTextField(Res.get("paymentAccounts.swift.bankName"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.bankName"))));
        bankName.setValidators(model.getBankNameValidator());

        bankBranch = new MaterialTextField(Res.get("paymentAccounts.swift.bankBranch"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.bankBranch"))));
        bankBranch.setValidators(model.getBankBranchValidator());

        bankAddress = new MaterialTextArea(Res.get("paymentAccounts.swift.bankAddress"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.bankAddress"))));
        bankAddress.setFixedHeight(90);
        bankAddress.setValidators(model.getBankAddressValidator());

        useIntermediaryBank = new CheckBox(Res.get("paymentAccounts.swift.useIntermediaryBank"));
        useIntermediaryBank.getStyleClass().add("small-checkbox");

        intermediaryBankCountryComboBox = new AutoCompleteComboBox<>(
                model.getBankCountries(),
                Res.get("paymentAccounts.swift.intermediaryBankCountry"),
                Res.get("paymentAccounts.createAccount.accountData.country.prompt")
        );
        intermediaryBankCountryComboBox.setPrefWidth(830 / 4d);
        intermediaryBankCountryComboBox.setConverter(new StringConverter<>() {
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

        intermediaryBankCountryErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.country.error"));
        intermediaryBankCountryErrorLabel.setMouseTransparent(true);
        intermediaryBankCountryErrorLabel.getStyleClass().add("material-text-field-error");
        VBox.setMargin(intermediaryBankCountryErrorLabel, new Insets(3.5, 0, 0, 16));
        intermediaryBankCountryVBox = new VBox(intermediaryBankCountryComboBox, intermediaryBankCountryErrorLabel);
        intermediaryBankCountryVBox.setAlignment(Pos.TOP_LEFT);

        intermediaryBankSwiftCode = new MaterialTextField(Res.get("paymentAccounts.swift.intermediaryBankSwiftCode"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.intermediaryBankSwiftCode"))));
        intermediaryBankSwiftCode.setValidators(model.getIntermediaryBankSwiftCodeValidator());

        intermediaryBankName = new MaterialTextField(Res.get("paymentAccounts.swift.intermediaryBankName"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.intermediaryBankName"))));
        intermediaryBankName.setValidators(model.getIntermediaryBankNameValidator());

        intermediaryBankBranch = new MaterialTextField(Res.get("paymentAccounts.swift.intermediaryBankBranch"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.intermediaryBankBranch"))));
        intermediaryBankBranch.setValidators(model.getIntermediaryBankBranchValidator());

        intermediaryBankAddress = new MaterialTextArea(Res.get("paymentAccounts.swift.intermediaryBankAddress"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.intermediaryBankAddress"))));
        intermediaryBankAddress.setFixedHeight(90);
        intermediaryBankAddress.setValidators(model.getIntermediaryBankAddressValidator());

        additionalInstructions = new MaterialTextArea(Res.get("paymentAccounts.swift.additionalInstructions"),
                Res.get("paymentAccounts.createAccount.prompt",
                        StringUtils.unCapitalize(Res.get("paymentAccounts.swift.additionalInstructions"))));
        additionalInstructions.setFixedHeight(90);
        additionalInstructions.setValidators(model.getAdditionalInstructionsValidator());

        HBox beneficiaryRow = new HBox(10, beneficiaryName, beneficiaryAccountNr, beneficiaryPhone);
        beneficiaryRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(beneficiaryName, Priority.ALWAYS);
        HBox.setHgrow(beneficiaryAccountNr, Priority.ALWAYS);
        HBox.setHgrow(beneficiaryPhone, Priority.ALWAYS);

        HBox bankCountryRow = new HBox(10, bankCountryVBox, currencyVBox);
        bankCountryRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(bankCountryVBox, Priority.ALWAYS);
        HBox.setHgrow(currencyVBox, Priority.ALWAYS);

        HBox bankInfoRow = new HBox(10, bankSwiftCode, bankName, bankBranch);
        bankInfoRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(bankSwiftCode, Priority.ALWAYS);
        HBox.setHgrow(bankName, Priority.ALWAYS);
        HBox.setHgrow(bankBranch, Priority.ALWAYS);

        HBox useIntermediaryBankRow = new HBox(useIntermediaryBank);
        useIntermediaryBankRow.setAlignment(Pos.CENTER_LEFT);

        HBox intermediaryCountryRow = new HBox(10, intermediaryBankCountryVBox);
        intermediaryCountryRow.setAlignment(Pos.TOP_LEFT);

        HBox intermediaryInfoRow = new HBox(10, intermediaryBankSwiftCode, intermediaryBankName, intermediaryBankBranch);
        intermediaryInfoRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(intermediaryBankSwiftCode, Priority.ALWAYS);
        HBox.setHgrow(intermediaryBankName, Priority.ALWAYS);
        HBox.setHgrow(intermediaryBankBranch, Priority.ALWAYS);

        content.getChildren().addAll(
                beneficiaryRow,
                beneficiaryAddress,
                bankCountryRow,
                bankInfoRow,
                bankAddress,
                useIntermediaryBankRow,
                intermediaryCountryRow,
                intermediaryInfoRow,
                intermediaryBankAddress,
                additionalInstructions
        );

        intermediaryNodes = List.of(
                intermediaryCountryRow,
                intermediaryInfoRow,
                intermediaryBankAddress,
                intermediaryBankSwiftCode,
                intermediaryBankName,
                intermediaryBankBranch
        );

        configOverlay(Res.get("paymentAccounts.createAccount.accountData.backgroundOverlay.swift"));
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        if (model.getSelectedBankCountry().get() != null) {
            bankCountryComboBox.getSelectionModel().select(model.getSelectedBankCountry().get());
        }
        if (model.getSelectedCurrency().get() != null) {
            currencyComboBox.getSelectionModel().select(model.getSelectedCurrency().get());
        }
        if (model.getIntermediaryBankCountry().get() != null) {
            intermediaryBankCountryComboBox.getSelectionModel().select(model.getIntermediaryBankCountry().get());
        }

        subscriptions.add(EasyBind.subscribe(model.getUseIntermediaryBank(), useIntermediaryBank -> {
            intermediaryNodes.forEach(node -> {
                node.setVisible(useIntermediaryBank);
                node.setManaged(useIntermediaryBank);
            });
            intermediaryBankCountryVBox.setVisible(useIntermediaryBank);
            intermediaryBankCountryVBox.setManaged(useIntermediaryBank);
        }));

        bankCountryErrorLabel.visibleProperty().bind(model.getBankCountryErrorVisible());
        bankCountryErrorLabel.managedProperty().bind(model.getBankCountryErrorVisible());
        currencyErrorLabel.visibleProperty().bind(model.getCurrencyErrorVisible());
        currencyErrorLabel.managedProperty().bind(model.getCurrencyErrorVisible());
        intermediaryBankCountryErrorLabel.visibleProperty().bind(model.getIntermediaryBankCountryErrorVisible());
        intermediaryBankCountryErrorLabel.managedProperty().bind(model.getIntermediaryBankCountryErrorVisible());

        beneficiaryName.textProperty().bindBidirectional(model.getBeneficiaryName());
        beneficiaryAccountNr.textProperty().bindBidirectional(model.getBeneficiaryAccountNr());
        beneficiaryPhone.textProperty().bindBidirectional(model.getBeneficiaryPhone());
        beneficiaryAddress.textProperty().bindBidirectional(model.getBeneficiaryAddress());

        bankSwiftCode.textProperty().bindBidirectional(model.getBankSwiftCode());
        bankName.textProperty().bindBidirectional(model.getBankName());
        bankBranch.textProperty().bindBidirectional(model.getBankBranch());
        bankAddress.textProperty().bindBidirectional(model.getBankAddress());

        useIntermediaryBank.selectedProperty().bindBidirectional(model.getUseIntermediaryBank());

        intermediaryBankSwiftCode.textProperty().bindBidirectional(model.getIntermediaryBankSwiftCode());
        intermediaryBankName.textProperty().bindBidirectional(model.getIntermediaryBankName());
        intermediaryBankBranch.textProperty().bindBidirectional(model.getIntermediaryBankBranch());
        intermediaryBankAddress.textProperty().bindBidirectional(model.getIntermediaryBankAddress());

        additionalInstructions.textProperty().bindBidirectional(model.getAdditionalInstructions());

        subscriptions.add(EasyBind.subscribe(bankCountryComboBox.getSelectionModel().selectedItemProperty(),
                selectedCountry -> {
                    if (selectedCountry != null) {
                        controller.onSelectBankCountry(selectedCountry);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(currencyComboBox.getSelectionModel().selectedItemProperty(),
                selectedCurrency -> {
                    if (selectedCurrency != null) {
                        controller.onSelectCurrency(selectedCurrency);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(model.getSelectedCurrency(),
                selectedCurrency -> {
                    if (selectedCurrency != null) {
                        currencyComboBox.getSelectionModel().select(selectedCurrency);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(model.getCurrencyCountryMismatch(),
                currencyCountryMismatch -> {
                    if (currencyCountryMismatch && !currencyCountryMismatchPopupShown) {
                        currencyCountryMismatchPopupShown = true;
                        new Popup().owner(root)
                                .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                                .warning(Res.get("paymentAccounts.createAccount.accountData.currency.warn.currencyCountryMismatch"))
                                .closeButtonText(Res.get("confirmation.yes"))
                                .onClose(() -> controller.onCurrencyCountryMisMatchPopupClosed(false))
                                .actionButtonText(Res.get("confirmation.no"))
                                .onAction(() -> controller.onCurrencyCountryMisMatchPopupClosed(true))
                                .show();
                    }
                }));

        subscriptions.add(EasyBind.subscribe(intermediaryBankCountryComboBox.getSelectionModel().selectedItemProperty(),
                selectedCountry -> {
                    if (selectedCountry != null) {
                        controller.onSelectIntermediaryBankCountry(selectedCountry);
                    }
                }));

        subscriptions.add(EasyBind.subscribe(useIntermediaryBank.selectedProperty(), controller::onToggleUseIntermediaryBank));

        subscriptions.add(EasyBind.subscribe(model.getRunValidation(),
                runValidation -> {
                    if (runValidation) {
                        beneficiaryName.validate();
                        beneficiaryAccountNr.validate();
                        beneficiaryAddress.validate();
                        if (StringUtils.isNotEmpty(beneficiaryPhone.getText())) {
                            beneficiaryPhone.validate();
                        } else {
                            beneficiaryPhone.resetValidation();
                        }
                        bankSwiftCode.validate();
                        bankName.validate();
                        bankAddress.validate();
                        if (StringUtils.isNotEmpty(bankBranch.getText())) {
                            bankBranch.validate();
                        } else {
                            bankBranch.resetValidation();
                        }
                        if (StringUtils.isNotEmpty(additionalInstructions.getText())) {
                            additionalInstructions.validate();
                        } else {
                            additionalInstructions.resetValidation();
                        }
                        if (model.getUseIntermediaryBank().get()) {
                            intermediaryBankSwiftCode.validate();
                            intermediaryBankName.validate();
                            intermediaryBankAddress.validate();
                            if (StringUtils.isNotEmpty(intermediaryBankBranch.getText())) {
                                intermediaryBankBranch.validate();
                            } else {
                                intermediaryBankBranch.resetValidation();
                            }
                        }
                        controller.onValidationDone();
                    }
                }));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        beneficiaryName.resetValidation();
        beneficiaryAccountNr.resetValidation();
        beneficiaryAddress.resetValidation();
        beneficiaryPhone.resetValidation();
        bankSwiftCode.resetValidation();
        bankName.resetValidation();
        bankAddress.resetValidation();
        bankBranch.resetValidation();
        intermediaryBankSwiftCode.resetValidation();
        intermediaryBankName.resetValidation();
        intermediaryBankAddress.resetValidation();
        intermediaryBankBranch.resetValidation();
        additionalInstructions.resetValidation();

        bankCountryErrorLabel.visibleProperty().unbind();
        bankCountryErrorLabel.managedProperty().unbind();
        currencyErrorLabel.visibleProperty().unbind();
        currencyErrorLabel.managedProperty().unbind();
        intermediaryBankCountryErrorLabel.visibleProperty().unbind();
        intermediaryBankCountryErrorLabel.managedProperty().unbind();

        beneficiaryName.textProperty().unbindBidirectional(model.getBeneficiaryName());
        beneficiaryAccountNr.textProperty().unbindBidirectional(model.getBeneficiaryAccountNr());
        beneficiaryPhone.textProperty().unbindBidirectional(model.getBeneficiaryPhone());
        beneficiaryAddress.textProperty().unbindBidirectional(model.getBeneficiaryAddress());
        bankSwiftCode.textProperty().unbindBidirectional(model.getBankSwiftCode());
        bankName.textProperty().unbindBidirectional(model.getBankName());
        bankBranch.textProperty().unbindBidirectional(model.getBankBranch());
        bankAddress.textProperty().unbindBidirectional(model.getBankAddress());
        intermediaryBankSwiftCode.textProperty().unbindBidirectional(model.getIntermediaryBankSwiftCode());
        intermediaryBankName.textProperty().unbindBidirectional(model.getIntermediaryBankName());
        intermediaryBankBranch.textProperty().unbindBidirectional(model.getIntermediaryBankBranch());
        intermediaryBankAddress.textProperty().unbindBidirectional(model.getIntermediaryBankAddress());
        additionalInstructions.textProperty().unbindBidirectional(model.getAdditionalInstructions());

        useIntermediaryBank.selectedProperty().unbindBidirectional(model.getUseIntermediaryBank());

        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
    }
}
