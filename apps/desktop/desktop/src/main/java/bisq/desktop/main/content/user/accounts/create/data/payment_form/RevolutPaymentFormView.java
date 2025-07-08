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

import bisq.common.currency.FiatCurrency;
import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RevolutPaymentFormView extends PaymentFormView<RevolutPaymentFormModel, RevolutPaymentFormController> {
    private final MaterialTextField userName;
    private final Label selectedCountriesErrorLabel;
    private final FlowPane selectedCurrenciesFlowPane;
    private Subscription runValidationPin;

    public RevolutPaymentFormView(RevolutPaymentFormModel model, RevolutPaymentFormController controller) {
        super(model, controller);

        userName = new MaterialTextField(Res.get("user.paymentAccounts.userName"),
                Res.get("user.paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("user.paymentAccounts.userName"))));
        userName.setValidators(model.getUserNameValidator());
        userName.setMaxWidth(Double.MAX_VALUE);

        Label selectedCurrenciesLabel = new Label(Res.get("user.paymentAccounts.revolut.selectedCurrencies"));
        selectedCurrenciesLabel.getStyleClass().add("bisq-text-1");

        selectedCurrenciesFlowPane = new FlowPane(5, 10);

        selectedCountriesErrorLabel = new Label(Res.get("user.paymentAccounts.createAccount.accountData.revolut.selectedCurrencies.error"));
        selectedCountriesErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(userName, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(userName,
                new HBox(selectedCurrenciesLabel, Spacer.fillHBox()),
                selectedCurrenciesFlowPane,
                new HBox(selectedCountriesErrorLabel, Spacer.fillHBox())
        );
    }

    @Override
    protected void onViewAttached() {
        if (StringUtils.isNotEmpty(model.getUserName().get())) {
            userName.setText(model.getUserName().get());
            userName.validate();
        }

        selectedCountriesErrorLabel.visibleProperty().bind(model.getSelectedCurrenciesErrorVisible());
        selectedCountriesErrorLabel.managedProperty().bind(model.getSelectedCurrenciesErrorVisible());

        userName.textProperty().bindBidirectional(model.getUserName());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                userName.validate();
                controller.onValidationDone();
            }
        });

        selectedCurrenciesFlowPane.getChildren().addAll(getCountryEntries(model.getRevolutCurrencies(), model.getSelectedCurrencies()));
    }

    @Override
    protected void onViewDetached() {
        userName.resetValidation();

        selectedCountriesErrorLabel.visibleProperty().unbind();
        selectedCountriesErrorLabel.managedProperty().unbind();

        userName.textProperty().unbindBidirectional(model.getUserName());

        runValidationPin.unsubscribe();

        selectedCurrenciesFlowPane.getChildren().stream()
                .map(e -> (CheckBox) e)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });

        selectedCurrenciesFlowPane.getChildren().clear();
    }

    private Node[] getCountryEntries(List<FiatCurrency> list, List<FiatCurrency> selectedCurrencies) {
        List<CheckBox> nodes = list.stream()
                .map(currency -> getCountryEntry(currency, selectedCurrencies.contains(currency)))
                .collect(Collectors.toList());
        return nodes.toArray(new Node[0]);
    }

    private CheckBox getCountryEntry(FiatCurrency currency, boolean isSelected) {
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