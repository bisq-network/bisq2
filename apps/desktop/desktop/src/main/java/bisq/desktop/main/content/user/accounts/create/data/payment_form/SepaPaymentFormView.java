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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.AutoCompleteComboBox;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SepaPaymentFormView extends PaymentFormView<SepaPaymentFormModel, SepaPaymentFormController> {
    private final AutoCompleteComboBox<Country> country;
    private final MaterialTextField holderName, iban, bic;
    private final Label countryErrorLabel, acceptedCountriesErrorLabel;
    private final FlowPane acceptEuroCountriesFlowPane, acceptNonEuroCountriesFlowPane;
    private Subscription selectedCountryPin, requireValidationPin;

    public SepaPaymentFormView(SepaPaymentFormModel model, SepaPaymentFormController controller) {
        super(model, controller);

        country = new AutoCompleteComboBox<>(
                model.getAllSepaCountries(),
                Res.get("user.paymentAccounts.createAccount.accountData.countryOfBank"),
                Res.get("user.paymentAccounts.createAccount.accountData.country.prompt")
        );
        country.setPrefWidth(830 / 4d);

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
        countryErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(countryErrorLabel, new Insets(3.5, 0, 0, 16));
        VBox countryVBox = new VBox(country, countryErrorLabel);
        countryVBox.setAlignment(Pos.TOP_LEFT);

        holderName = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.sepa.holderName"),
                Res.get("user.paymentAccounts.createAccount.accountData.sepa.holderName.prompt"));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(holderName, Priority.ALWAYS);
        HBox countryAndHolderNameHBox = new HBox(10, countryVBox, holderName);

        iban = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.sepa.iban"),
                Res.get("user.paymentAccounts.createAccount.accountData.sepa.iban.prompt"));
        iban.setValidators(model.getSepaIbanValidator());
        iban.setMaxWidth(Double.MAX_VALUE);

        bic = new MaterialTextField(Res.get("user.paymentAccounts.createAccount.accountData.sepa.bic"),
                Res.get("user.paymentAccounts.createAccount.accountData.sepa.bic.prompt"));
        bic.setValidators(model.getSepaBicValidator());
        bic.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(iban, Priority.ALWAYS);
        HBox.setHgrow(bic, Priority.ALWAYS);
        HBox ibanBicHBox = new HBox(10, iban, bic);

        Label acceptAllEuroCountriesLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.sepa.acceptAllEuroCountries"));
        acceptAllEuroCountriesLabel.getStyleClass().add("bisq-text-1");
        Label acceptAllNonEuroCountriesLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.sepa.acceptNonEuroCountries"));
        acceptAllNonEuroCountriesLabel.getStyleClass().add("bisq-text-1");

        acceptEuroCountriesFlowPane = new FlowPane(5, 10);
        acceptNonEuroCountriesFlowPane = new FlowPane(5, 10);

        acceptedCountriesErrorLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.sepa.acceptCountries.error"));
        acceptedCountriesErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setVgrow(countryAndHolderNameHBox, Priority.ALWAYS);
        VBox.setVgrow(ibanBicHBox, Priority.ALWAYS);
        VBox.setMargin(acceptEuroCountriesFlowPane, new Insets(0, 0, 5, 0));
        root.getChildren().addAll(countryAndHolderNameHBox,
                ibanBicHBox,
                new HBox(acceptAllEuroCountriesLabel, Spacer.fillHBox()),
                acceptEuroCountriesFlowPane,
                new HBox(acceptAllNonEuroCountriesLabel, Spacer.fillHBox()),
                acceptNonEuroCountriesFlowPane,
                new HBox(acceptedCountriesErrorLabel, Spacer.fillHBox())
        );
    }

    @Override
    protected void onViewAttached() {
        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getIban().get())) {
            iban.setText(model.getIban().get());
            iban.validate();
        }
        if (StringUtils.isNotEmpty(model.getBic().get())) {
            bic.setText(model.getBic().get());
            bic.validate();
        }
        if (model.getSelectedCountryOfBank().get() != null) {
            country.getSelectionModel().select(model.getSelectedCountryOfBank().get());
        }

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());

        acceptedCountriesErrorLabel.visibleProperty().bind(model.getAcceptedCountriesErrorVisible());
        acceptedCountriesErrorLabel.managedProperty().bind(model.getAcceptedCountriesErrorVisible());

        holderName.textProperty().bindBidirectional(model.getHolderName());
        iban.textProperty().bindBidirectional(model.getIban());
        bic.textProperty().bindBidirectional(model.getBic());

        selectedCountryPin = EasyBind.subscribe(country.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                controller.onCountryOfBankSelected(selectedCountry);
            }
        });

        requireValidationPin = EasyBind.subscribe(model.getRequireValidation(), requireValidation -> {
            if (requireValidation) {
                holderName.validate();
                iban.validate();
                bic.validate();
                controller.onValidationDone();
            }
        });

        acceptEuroCountriesFlowPane.getChildren().addAll(getCountryEntries(model.getAllEuroCountries(),
                model.getAcceptedEuroCountries(),
                true));
        acceptNonEuroCountriesFlowPane.getChildren().addAll(getCountryEntries(model.getAllNonEuroCountries(),
                model.getAcceptedNonEuroCountries(),
                false));
    }

    @Override
    protected void onViewDetached() {
        holderName.resetValidation();
        iban.resetValidation();
        bic.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();
        acceptedCountriesErrorLabel.visibleProperty().unbind();
        acceptedCountriesErrorLabel.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        iban.textProperty().unbindBidirectional(model.getIban());
        bic.textProperty().unbindBidirectional(model.getBic());

        selectedCountryPin.unsubscribe();
        requireValidationPin.unsubscribe();

        acceptEuroCountriesFlowPane.getChildren().stream()
                .map(e -> (CheckBox) e)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });
        acceptNonEuroCountriesFlowPane.getChildren().stream()
                .map(e -> (CheckBox) e)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });

        acceptEuroCountriesFlowPane.getChildren().clear();
        acceptNonEuroCountriesFlowPane.getChildren().clear();
    }

    private Node[] getCountryEntries(List<Country> list, List<Country> selectedCountries, boolean isEuroCountry) {
        List<CheckBox> nodes = list.stream()
                .map(country -> getCountryEntry(country, selectedCountries.contains(country), isEuroCountry))
                .collect(Collectors.toList());
        return nodes.toArray(new Node[0]);
    }

    private CheckBox getCountryEntry(Country country, boolean isSelected, boolean isEuroCountry) {
        CheckBox checkBox = new CheckBox(country.getName());
        checkBox.setSelected(isSelected);
        checkBox.getStyleClass().add("small-checkbox");
        double width = 100;
        checkBox.setMinWidth(width);
        checkBox.setMaxWidth(width);
        checkBox.setOnAction(e -> controller.onSelectAcceptedCountry(country, checkBox.isSelected(), isEuroCountry));
        return checkBox;
    }
}