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

package bisq.desktop.main.content.wallet.receive.qrcode;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletAddressQrCodeView extends View<VBox, WalletAddressQrCodeModel, WalletAddressQrCodeController> {
    private final MaterialTextField amount, label;
    private final BisqMenuItem copyBtcUri;
    private final ImageView qrImageView;
    private final Label btcUri;

    public WalletAddressQrCodeView(WalletAddressQrCodeModel model, WalletAddressQrCodeController controller) {
        super(new VBox(20), model, controller);

        // Qr Code
        qrImageView = new ImageView();
        qrImageView.setFitHeight(model.getQrCodeSize());
        qrImageView.setFitWidth(model.getQrCodeSize());
        qrImageView.setPreserveRatio(true);
        btcUri = new Label();
        copyBtcUri = new BisqMenuItem("copy-green", "copy-white");
        copyBtcUri.setTooltip(Res.get("wallet.receive.copyBtcUri"));
        HBox btcUriHBox = new HBox(10, Spacer.fillHBox(), btcUri, copyBtcUri, Spacer.fillHBox());
        VBox qrCodeVBox = new VBox(10, qrImageView,  btcUriHBox);
        qrCodeVBox.setAlignment(Pos.CENTER);

        // Amount
        amount = new MaterialTextField(Res.get("wallet.receive.amountInBtc"));
        amount.setEditable(true);
        amount.setPrefWidth(230);
        amount.setValidator(model.getBitcoinUriAmountValidator());

        // Label
        label = new MaterialTextField(Res.get("wallet.receive.label"));
        label.setEditable(true);
        label.setPrefWidth(230);
        label.setValidators(model.getLabelMaxLengthValidator());

        HBox optionsHBox = new HBox(30, amount, label);
        optionsHBox.setAlignment(Pos.TOP_CENTER);

        root.getChildren().setAll(qrCodeVBox, optionsHBox);
        root.setAlignment(Pos.CENTER);
    }

    @Override
    protected void onViewAttached() {
        qrImageView.imageProperty().bind(model.getQrCodeImage());
        btcUri.textProperty().bind(model.getBtcUri());
        amount.textProperty().bindBidirectional(model.getAmount());
        model.getIsAmountValid().bind(amount.isValidProperty());
        label.textProperty().bindBidirectional(model.getLabel());

        copyBtcUri.setOnAction(e -> controller.onCopyToClipboard());
        root.setOnMouseClicked(e -> {
            root.requestFocus();
            amount.validate();
            label.validate();
        });
    }

    @Override
    protected void onViewDetached() {
        qrImageView.imageProperty().unbind();
        btcUri.textProperty().unbind();
        amount.textProperty().unbindBidirectional(model.getAmount());
        model.getIsAmountValid().unbind();
        label.textProperty().unbindBidirectional(model.getLabel());

        copyBtcUri.setOnAction(null);
        root.setOnMouseClicked(null);

        amount.resetValidation();
        label.resetValidation();
    }
}
