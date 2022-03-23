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

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.BisqGridPane;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class WalletReceiveView extends View<VBox, WalletReceiveModel, WalletReceiveController> {
    public WalletReceiveView(WalletReceiveModel model, WalletReceiveController controller) {
        super(new VBox(), model, controller);

        Button generateNewAddressButton = buildGenerateAddressButton(controller);
        Button copyButton = buildCopyButton(controller);
        TextField textField = buildAddressTextField(model);

        var gridPane = new BisqGridPane();
        gridPane.addColumn(0, textField);
        gridPane.addColumn(1, copyButton);

        root.getChildren().addAll(generateNewAddressButton, gridPane);
    }

    private TextField buildAddressTextField(WalletReceiveModel model) {
        TextField textField = new TextField();
        textField.setPrefWidth(500);
        textField.textProperty().bind(model.getAddress());
        textField.setEditable(false);
        return textField;
    }

    private Button buildCopyButton(WalletReceiveController controller) {
        var copyButton = new Button();
        var imageView = new ImageView(new Image("images/copy_button.png"));
        imageView.setFitHeight(15.0);
        imageView.setFitWidth(15.0);
        copyButton.setGraphic(imageView);
        copyButton.setOnAction(event -> controller.copyAddress());
        return copyButton;
    }

    private Button buildGenerateAddressButton(WalletReceiveController controller) {
        var generateNewAddressButton = new Button(Res.get("wallet.receive.generateNewAddress"));
        generateNewAddressButton.setOnAction(event -> controller.onGenerateNewAddress());
        return generateNewAddressButton;
    }
}
