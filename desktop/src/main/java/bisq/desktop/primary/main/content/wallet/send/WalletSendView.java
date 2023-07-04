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

package bisq.desktop.primary.main.content.wallet.send;

import bisq.desktop.common.utils.validation.MonetaryValidator;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletSendView extends View<VBox, WalletSendModel, WalletSendController> {
    private final MaterialTextField address, amount;
    private final MaterialPasswordField password;
    private final Button sendButton;

    public WalletSendView(WalletSendModel model, WalletSendController controller, MonetaryValidator amountValidator) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(40, 0, 0, 0));

        address = new MaterialTextField(Res.get("wallet.send.address"), null, null);
        amount = new MaterialTextField(Res.get("wallet.send.amount"), null, null);
        amount.setValidator(amountValidator);
        password = new MaterialPasswordField(Res.get("wallet.send.password"), null, null);
        sendButton = new Button(Res.get("wallet.send.sendBtc"));
        sendButton.setDefaultButton(true);

        root.getChildren().addAll(address, amount, password, sendButton);
    }

    @Override
    protected void onViewAttached() {
        address.textProperty().bindBidirectional(model.getAddress());
        amount.textProperty().bindBidirectional(model.getAmount());
        password.textProperty().bindBidirectional(model.getPassword());
        password.visibleProperty().bind(model.getIsPasswordVisible());
        password.managedProperty().bind(model.getIsPasswordVisible());
        sendButton.setOnAction(e -> controller.onSend());
    }

    @Override
    protected void onViewDetached() {
        address.textProperty().unbindBidirectional(model.getAddress());
        amount.textProperty().unbindBidirectional(model.getAmount());
        password.textProperty().unbindBidirectional(model.getPassword());
        password.visibleProperty().unbind();
        password.managedProperty().unbind();
        sendButton.setOnAction(null);
    }
}
