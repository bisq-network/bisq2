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

package bisq.desktop.main.content.user.accounts.create.data;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentDataView extends View<VBox, PaymentDataModel, PaymentDataController> {
    private final Label titleLabel;

    public PaymentDataView(PaymentDataModel model, PaymentDataController controller) {
        super(new VBox(10), model, controller);

        root.setPadding(new Insets(25, 15, 15, 15));
        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("create-account-data-view");

        titleLabel = new Label();
        titleLabel.getStyleClass().add("bisq-text-headline-2");
    }

    @Override
    protected void onViewAttached() {
        root.getChildren().addAll(titleLabel, model.getPaymentForm(), Spacer.fillVBox());
        titleLabel.setText(Res.get("user.paymentAccounts.createAccount.accountData.title",
                model.getPaymentMethod().getDisplayString()));
    }

    @Override
    protected void onViewDetached() {
        root.getChildren().clear();
    }
}