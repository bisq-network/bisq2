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

import bisq.common.asset.FiatCurrency;
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
public class MoneyGramFormView extends FormView<MoneyGramFormModel, MoneyGramFormController> {
    private final AutoCompleteComboBox<Country> countryComboBox;
    private final MaterialTextField holderName, email, state;
    private final Label countryErrorLabel, selectedCurrenciesErrorLabel;
    private final FlowPane selectedCurrenciesFlowPane;
    private Subscription runValidationPin, selectedCountryPin;

    public MoneyGramFormView(MoneyGramFormModel model, MoneyGramFormController controller) {
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

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        email = new MaterialTextField(Res.get("paymentAccounts.email"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.email"))));
        email.setValidators(model.getEmailValidator());
        email.setMaxWidth(Double.MAX_VALUE);

        state = new MaterialTextField(Res.get("paymentAccounts.moneyGram.state"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.moneyGram.state"))));
        state.setValidators(model.getStateValidator());
        state.setMaxWidth(Double.MAX_VALUE);

        Label selectedCurrenciesLabel = new Label(Res.get("paymentAccounts.moneyGram.selectedCurrencies"));
        selectedCurrenciesLabel.getStyleClass().add("bisq-text-1");

        selectedCurrenciesFlowPane = new FlowPane(5, 10);

        selectedCurrenciesErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.moneyGram.selectedCurrencies.error"));
        selectedCurrenciesErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(countryVBox, new Insets(0, 0, 10, 0));
        VBox.setMargin(holderName, new Insets(0, 0, 10, 0));
        VBox.setMargin(email, new Insets(0, 0, 10, 0));
        VBox.setMargin(state, new Insets(0, 0, 10, 0));

        HBox.setHgrow(holderName, Priority.ALWAYS);
        HBox.setHgrow(email, Priority.ALWAYS);
        HBox.setHgrow(state, Priority.ALWAYS);

        content.getChildren().addAll(countryVBox, holderName, email, state,
                new HBox(selectedCurrenciesLabel, Spacer.fillHBox()),
                selectedCurrenciesFlowPane,
                new HBox(selectedCurrenciesErrorLabel, Spacer.fillHBox())
        );
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        countryComboBox.getSelectionModel().select(model.getSelectedCountry().get());

        countryErrorLabel.visibleProperty().bind(model.getCountryErrorVisible());
        countryErrorLabel.managedProperty().bind(model.getCountryErrorVisible());
        selectedCurrenciesErrorLabel.visibleProperty().bind(model.getSelectedCurrenciesErrorVisible());
        selectedCurrenciesErrorLabel.managedProperty().bind(model.getSelectedCurrenciesErrorVisible());
        state.visibleProperty().bind(model.getStateVisible());
        state.managedProperty().bind(model.getStateVisible());

        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }
        if (StringUtils.isNotEmpty(model.getEmail().get())) {
            email.setText(model.getEmail().get());
            email.validate();
        }
        if (StringUtils.isNotEmpty(model.getState().get())) {
            state.setText(model.getState().get());
            state.validate();
        }

        holderName.textProperty().bindBidirectional(model.getHolderName());
        email.textProperty().bindBidirectional(model.getEmail());
        state.textProperty().bindBidirectional(model.getState());

        selectedCountryPin = EasyBind.subscribe(countryComboBox.getSelectionModel().selectedItemProperty(), selectedCountry -> {
            if (selectedCountry != null) {
                controller.onSelectCountry(selectedCountry);
            }
        });

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                email.validate();
                state.validate();
                controller.onValidationDone();
            }
        });

        selectedCurrenciesFlowPane.getChildren().addAll(getCurrencyEntries(model.getCurrencies(), model.getSelectedCurrencies()));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        email.resetValidation();
        state.resetValidation();

        countryErrorLabel.visibleProperty().unbind();
        countryErrorLabel.managedProperty().unbind();
        selectedCurrenciesErrorLabel.visibleProperty().unbind();
        selectedCurrenciesErrorLabel.managedProperty().unbind();
        state.visibleProperty().unbind();
        state.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        email.textProperty().unbindBidirectional(model.getEmail());
        state.textProperty().unbindBidirectional(model.getState());

        selectedCountryPin.unsubscribe();
        runValidationPin.unsubscribe();

        selectedCurrenciesFlowPane.getChildren().stream()
                .map(e -> (CheckBox) e)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });
        selectedCurrenciesFlowPane.getChildren().clear();
    }

    private Node[] getCurrencyEntries(List<FiatCurrency> list, List<FiatCurrency> selectedCurrencies) {
        List<CheckBox> nodes = list.stream()
                .map(currency -> getCurrencyEntry(currency, selectedCurrencies.contains(currency)))
                .collect(Collectors.toList());
        return nodes.toArray(new Node[0]);
    }

    private CheckBox getCurrencyEntry(FiatCurrency currency, boolean isSelected) {
        CheckBox checkBox = new CheckBox(currency.getName());
        checkBox.setSelected(isSelected);
        checkBox.getStyleClass().add("small-checkbox");
        double width = 136;
        checkBox.setMinWidth(width);
        checkBox.setMaxWidth(width);
        checkBox.setOnAction(e -> controller.onSelectCurrency(currency, checkBox.isSelected()));
        return checkBox;
    }
}
