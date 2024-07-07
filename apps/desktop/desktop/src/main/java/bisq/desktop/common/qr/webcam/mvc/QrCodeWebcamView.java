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

package bisq.desktop.common.qr.webcam.mvc;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class QrCodeWebcamView extends View<BorderPane, QrCodeWebcamModel, QrCodeWebcamController> /*implements FillStageView */ {
    private final ImageView imageView;
    private final Button closeButton;
    private final VBox waitingInfo;
    private final WaitingAnimation waitingAnimation;
    private Subscription webcamImagePin;

    public QrCodeWebcamView(QrCodeWebcamModel model, QrCodeWebcamController controller) {
        super(new BorderPane(), model, controller);

        int videoWidth = model.getVideoSize().getWidth();
        int videoHeight = model.getVideoSize().getHeight();

        root.setPrefWidth(videoWidth);
        root.setPrefHeight(videoHeight + 23 + 20);

        closeButton = BisqIconButton.createIconButton("close");
        HBox.setMargin(closeButton, new Insets(10, 10, 10, 0));
        HBox closeButtonBox = new HBox(Spacer.fillHBox(), closeButton);
        root.setTop(closeButtonBox);

        waitingAnimation = new WaitingAnimation(WaitingState.SCAN_WITH_CAMERA);
        waitingInfo = createWaitingInfo(waitingAnimation, new Label(Res.get("bisqEasy.tradeState.info.buyer.phase1a.scanQrCode.connectingToCamera")));

        imageView = new ImageView();
        imageView.setFitWidth(videoWidth);
        imageView.setFitHeight(videoHeight);
        // Mirror image
        imageView.setScaleX(-1);

        StackPane.setAlignment(waitingInfo, Pos.TOP_CENTER);
        StackPane stackPane = new StackPane(waitingInfo, imageView);
        stackPane.setPrefWidth(videoWidth);
        stackPane.setPrefHeight(videoHeight);
        root.setBottom(stackPane);
    }

    @Override
    protected void onViewAttached() {
        webcamImagePin = EasyBind.subscribe(model.getWebcamImage(), webcamImage -> {
            boolean isConnecting = webcamImage == null;
            waitingInfo.setVisible(isConnecting);
            waitingInfo.setManaged(isConnecting);
            imageView.setVisible(!isConnecting);
            imageView.setManaged(!isConnecting);
            if (!isConnecting) {
                imageView.setImage(webcamImage);
                waitingAnimation.stop();
            }
        });

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        webcamImagePin.unsubscribe();
        closeButton.setOnAction(null);
        waitingAnimation.stop();
    }

    private VBox createWaitingInfo(WaitingAnimation animation, Label headline) {
        animation.setAlignment(Pos.CENTER);
        headline.setAlignment(Pos.CENTER);
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.getStyleClass().add("qr-code-webcam");
        VBox.setMargin(animation, new Insets(model.getVideoSize().getHeight() / 4d, 0, 0, 0));
        VBox waitingInfo = new VBox(animation, headline);
        waitingInfo.setSpacing(20);
        waitingInfo.setAlignment(Pos.TOP_CENTER);
        return waitingInfo;
    }
}