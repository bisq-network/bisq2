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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts.create;

import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StableCoinAccountSummaryController implements Controller {
    private final CreateStableCoinAccountModel parentModel;
    @Getter
    private final SummaryView view;

    StableCoinAccountSummaryController(CreateStableCoinAccountModel parentModel) {
        this.parentModel = parentModel;
        this.view = new SummaryView(parentModel);
    }

    @Override
    public void onActivate() {
        StableCoinPaymentRail rail = parentModel.getSelectedRail().get();
        if (rail != null) {
            view.networkValue.setText(rail.getStableCoin().getNetwork().getDisplayName());
            view.currencyValue.setText(rail.getStableCoin().getCode());
        }
        view.addressValue.setText(parentModel.getAddress().get());

        if (StringUtils.isEmpty(parentModel.getAccountName().get()) && rail != null) {
            String defaultName = rail.getStableCoin().getCode() + " (" +
                    rail.getStableCoin().getNetwork().getDisplayName() + ") " +
                    StringUtils.truncate(parentModel.getAddress().get(), 8);
            parentModel.getAccountName().set(defaultName);
        }
        view.accountNameField.setText(parentModel.getAccountName().get());
    }

    @Override
    public void onDeactivate() {
    }

    static class SummaryView extends View<VBox, CreateStableCoinAccountModel, StableCoinAccountSummaryController> {
        private final Label networkValue, addressValue, currencyValue;
        private final MaterialTextField accountNameField;
        private ChangeListener<String> accountNameListener;

        SummaryView(CreateStableCoinAccountModel model) {
            super(new VBox(15), model, null);

            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(20, 0, 0, 0));

            Label headline = new Label(Res.get("user.stableCoinAccounts.create.summary.headline"));
            headline.getStyleClass().add("large-thin-headline");

            networkValue = new Label();
            networkValue.getStyleClass().add("user-content-text");

            addressValue = new Label();
            addressValue.setWrapText(true);
            addressValue.getStyleClass().add("user-content-text");

            currencyValue = new Label();
            currencyValue.getStyleClass().add("user-content-text");

            accountNameField = new MaterialTextField(
                    Res.get("user.stableCoinAccounts.create.accountName"),
                    Res.get("user.stableCoinAccounts.create.accountName.prompt"));
            accountNameField.setPrefWidth(400);

            root.getChildren().addAll(
                    headline,
                    createRow(Res.get("user.stableCoinAccounts.create.summary.network"), networkValue),
                    createRow(Res.get("user.stableCoinAccounts.create.summary.currency"), currencyValue),
                    createRow(Res.get("user.stableCoinAccounts.create.summary.address"), addressValue),
                    accountNameField);
        }

        @Override
        protected void onViewAttached() {
            accountNameListener = (obs, old, newVal) -> model.getAccountName().set(newVal);
            accountNameField.textProperty().addListener(accountNameListener);
        }

        @Override
        protected void onViewDetached() {
            accountNameField.textProperty().removeListener(accountNameListener);
        }

        private javafx.scene.layout.HBox createRow(String labelText, Label valueLabel) {
            Label title = new Label(labelText + ":");
            title.getStyleClass().add("bisq-text-14");
            title.setMinWidth(100);
            return new javafx.scene.layout.HBox(10, title, valueLabel);
        }
    }
}
