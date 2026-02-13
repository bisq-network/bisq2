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

package bisq.desktop.main.content.user.accounts.fiat_accounts.create.data;

import bisq.desktop.common.view.View;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountDataView extends View<VBox, AccountDataModel, AccountDataController> {

    public AccountDataView(AccountDataModel model, AccountDataController controller) {
        super(new VBox(15), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.getStyleClass().add("create-account-data-view");
    }

    @Override
    protected void onViewAttached() {
        root.getChildren().setAll(model.getPaymentForm());
    }

    @Override
    protected void onViewDetached() {
        root.getChildren().clear();
    }
}