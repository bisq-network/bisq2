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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.data;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountDataView extends View<VBox, AccountDataModel, AccountDataController> {
    private final Label titleLabel;

    public AccountDataView(AccountDataModel model, AccountDataController controller) {
        super(new VBox(15), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("create-account-data-view");

        titleLabel = new Label();
        titleLabel.getStyleClass().add("bisq-text-headline-2");
    }

    @Override
    protected void onViewAttached() {
        titleLabel.setText(Res.get("paymentAccounts.crypto.address.title", model.getPaymentMethod().getName()));
        root.getChildren().setAll(Spacer.fillVBox(),
                titleLabel,
                model.getPaymentForm(),
                Spacer.fillVBox());
    }

    @Override
    protected void onViewDetached() {
        root.getChildren().clear();
    }
}