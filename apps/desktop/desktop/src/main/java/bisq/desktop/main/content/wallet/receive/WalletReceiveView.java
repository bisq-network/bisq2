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
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletReceiveView extends View<VBox, WalletReceiveModel, WalletReceiveController> {
    private final MaterialTextField address;
    private final Button copyButton, closeIconButton, closeButton;

    public WalletReceiveView(WalletReceiveModel model, WalletReceiveController controller) {
        super(new VBox(30), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH - 20);
        root.setPrefHeight(OverlayModel.HEIGHT - 20);
        root.setAlignment(Pos.TOP_LEFT);

        closeIconButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeIconButton);
        closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

        Label titleLabel = new Label(Res.get("wallet.receive.header"));
        titleLabel.getStyleClass().add("bisq-text-headline-2");
        HBox headlineBox = new HBox(Spacer.fillHBox(), titleLabel, Spacer.fillHBox());

        address = new MaterialTextField(Res.get("wallet.receive.address"));
        address.setEditable(false);
        address.showCopyIcon();

        VBox contentBox = new VBox(20);
        contentBox.setPadding(new Insets(0, 70, 0, 70));
        contentBox.getChildren().addAll(address);

        closeButton = new Button(Res.get("action.close"));
        copyButton = new Button(Res.get("wallet.receive.copy"));
        copyButton.setDefaultButton(true);
        HBox buttonsBox = new HBox(20, closeButton, copyButton);
        buttonsBox.setAlignment(Pos.CENTER);

        root.getChildren().setAll(closeButtonRow, headlineBox, contentBox, buttonsBox);
    }

    @Override
    protected void onViewAttached() {
        address.textProperty().bind(model.getReceiveAddress());

        copyButton.setOnAction(e -> controller.onCopyToClipboard());
        address.getIconButton().setOnAction(e -> controller.onCopyToClipboard());
        closeIconButton.setOnAction(e -> controller.onClose());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        address.textProperty().unbind();

        copyButton.setOnAction(null);
        address.getIconButton().setOnAction(null);
        closeIconButton.setOnAction(null);
        closeButton.setOnAction(null);
    }
}
