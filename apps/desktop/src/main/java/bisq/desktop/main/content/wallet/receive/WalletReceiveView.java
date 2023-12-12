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

package bisq.desktop.main.content.wallet.receive;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletReceiveView extends View<VBox, WalletReceiveModel, WalletReceiveController> {

    private final MaterialTextField address;
    private final Button copyButton;

    public WalletReceiveView(WalletReceiveModel model, WalletReceiveController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(40, 0, 0, 0));

        address = new MaterialTextField(Res.get("wallet.receive.address"));
        address.setEditable(false);
        address.showCopyIcon();
        copyButton = new Button(Res.get("wallet.receive.copy"));
        copyButton.setDefaultButton(true);

        root.getChildren().addAll(address, copyButton);
    }

    @Override
    protected void onViewAttached() {
        address.textProperty().bind(model.getReceiveAddress());
        copyButton.setOnAction(e -> controller.onCopyToClipboard());
        address.getIconButton().setOnAction(e -> controller.onCopyToClipboard());
    }

    @Override
    protected void onViewDetached() {
        address.textProperty().unbind();
        copyButton.setOnAction(null);
    }
}
