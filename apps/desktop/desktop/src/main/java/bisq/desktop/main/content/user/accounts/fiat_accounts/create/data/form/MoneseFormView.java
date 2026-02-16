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
public class MoneseFormView extends FormView<MoneseFormModel, MoneseFormController> {
    private final MaterialTextField holderName;
    private final MaterialTextField mobileNr;
    private final FlowPane selectedCurrenciesFlowPane;
    private final Label selectedCountriesErrorLabel;
    private Subscription runValidationPin;

    public MoneseFormView(MoneseFormModel model, MoneseFormController controller) {
        super(model, controller);


        holderName = new MaterialTextField(Res.get("paymentAccounts.holderName"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.holderName"))));
        holderName.setValidators(model.getHolderNameValidator());
        holderName.setMaxWidth(Double.MAX_VALUE);

        mobileNr = new MaterialTextField(Res.get("paymentAccounts.mobileNr"),
                Res.get("paymentAccounts.createAccount.prompt", StringUtils.unCapitalize(Res.get("paymentAccounts.mobileNr"))));
        mobileNr.setValidators(model.getMobileNrValidator());
        mobileNr.setMaxWidth(Double.MAX_VALUE);


        Label selectedCurrenciesLabel = new Label(Res.get("paymentAccounts.revolut.selectedCurrencies"));
        selectedCurrenciesLabel.getStyleClass().add("bisq-text-1");

        selectedCurrenciesFlowPane = new FlowPane(5, 10);

        selectedCountriesErrorLabel = new Label(Res.get("paymentAccounts.createAccount.accountData.revolut.selectedCurrencies.error"));
        selectedCountriesErrorLabel.getStyleClass().add("material-text-field-error");

        VBox.setMargin(holderName, new Insets(0, 0, 10, 0));
        content.getChildren().addAll(holderName,
                mobileNr,
                new HBox(selectedCurrenciesLabel, Spacer.fillHBox()),
                selectedCurrenciesFlowPane,
                new HBox(selectedCountriesErrorLabel, Spacer.fillHBox())
        );
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        if (StringUtils.isNotEmpty(model.getHolderName().get())) {
            holderName.setText(model.getHolderName().get());
            holderName.validate();
        }

        if (StringUtils.isNotEmpty(model.getMobileNr().get())) {
            mobileNr.setText(model.getMobileNr().get());
            mobileNr.validate();
        }

        selectedCountriesErrorLabel.visibleProperty().bind(model.getSelectedCurrenciesErrorVisible());
        selectedCountriesErrorLabel.managedProperty().bind(model.getSelectedCurrenciesErrorVisible());

        holderName.textProperty().bindBidirectional(model.getHolderName());
        mobileNr.textProperty().bindBidirectional(model.getMobileNr());

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                holderName.validate();
                mobileNr.validate();
                controller.onValidationDone();
            }
        });

        selectedCurrenciesFlowPane.getChildren().addAll(getCountryEntries(model.getCurrencies(), model.getSelectedCurrencies()));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        holderName.resetValidation();
        mobileNr.resetValidation();

        selectedCountriesErrorLabel.visibleProperty().unbind();
        selectedCountriesErrorLabel.managedProperty().unbind();

        holderName.textProperty().unbindBidirectional(model.getHolderName());
        mobileNr.textProperty().unbindBidirectional(model.getMobileNr());

        runValidationPin.unsubscribe();

        selectedCurrenciesFlowPane.getChildren().stream()
                .map(CheckBox.class::cast)
                .forEach(checkBox -> {
                    checkBox.setTooltip(null);
                    checkBox.setOnAction(null);
                });

        selectedCurrenciesFlowPane.getChildren().clear();
    }

    private Node[] getCountryEntries(List<FiatCurrency> list, List<FiatCurrency> selectedCurrencies) {
        List<CheckBox> nodes = list.stream()
                .map(currency -> getCountryEntry(currency, selectedCurrencies.contains(currency)))
                .toList();
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
