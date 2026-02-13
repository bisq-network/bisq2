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

@Slf4j
public class WiseFormView extends FormView<WiseFormModel, WiseFormController> {
    private final MaterialTextField holderName, email;
    private final Label  selectedCurrenciesErrorLabel;
    private final FlowPane selectedCurrenciesFlowPane;
    private Subscription runValidationPin;

    public WiseFormView(WiseFormModel model, WiseFormController controller) {
        super(model, controller);

        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        email = new MaterialTextField(Res.get("paymentAccounts.email"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.email"))));
        email.setValidators(model.getEmailValidator());
        email.setMaxWidth(Double.MAX_VALUE);

        Label selectedCurrenciesLabel = new Label(Res.get("paymentAccounts.wise.selectedCurrencies"));
        selectedCurrenciesLabel.getStyleClass().add("bisq-text-1");

        selectedCurrenciesFlowPane = new FlowPane(5, 10);

        selectedCurrenciesErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.wise.selectedCurrencies.error"));
        selectedCurrenciesErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(holderName, new Insets(0, 0, 10, 0));
        VBox.setMargin(email, new Insets(0, 0, 10, 0));

        content.getChildren().addAll(holderName, email,
                new HBox(selectedCurrenciesLabel, Spacer.fillHBox()),
                selectedCurrenciesFlowPane,
                new HBox(selectedCurrenciesErrorLabel, Spacer.fillHBox())
        );
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        selectedCurrenciesErrorLabel.visibleProperty().bind(model.getSelectedCurrenciesErrorVisible());
        selectedCurrenciesErrorLabel.managedProperty().bind(model.getSelectedCurrenciesErrorVisible());

        holderName.textProperty().bindBidirectional(model.getHolderName());
        email.textProperty().bindBidirectional(model.getEmail());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                email.validate();
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

        selectedCurrenciesErrorLabel.visibleProperty().unbind();
        selectedCurrenciesErrorLabel.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        email.textProperty().unbindBidirectional(model.getEmail());

        runValidationPin.unsubscribe();

        selectedCurrenciesFlowPane.getChildren().stream()
                .map(CheckBox.class::cast)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });
        selectedCurrenciesFlowPane.getChildren().clear();
    }

    private Node[] getCurrencyEntries(List<FiatCurrency> list, List<FiatCurrency> selectedCurrencies) {
        List<CheckBox> nodes = list.stream()
                .map(currency -> getCurrencyEntry(currency, selectedCurrencies.contains(currency)))
                .toList();
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
