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

package bisq.desktop.primary.main.content.wallet.receive;

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

public class WalletReceiveListCell extends ListCell<String> {
    private final HBox root;
    private final Label addressLabel;
    private final Button copyToClipboardButton;

    public WalletReceiveListCell() {
        root = new HBox();
        addressLabel = new Label();
        copyToClipboardButton = new Button(Res.get("wallet.receive.copy"));
        root.getChildren().addAll(addressLabel, copyToClipboardButton);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            addressLabel.setText(item);
            copyToClipboardButton.setOnMouseClicked(event -> ClipboardUtil.copyToClipboard(item));
            setGraphic(root);
        }
    }
}
