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

package bisq.desktop.primary.main.content.user.notifications;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SendNotificationView extends View<VBox, SendNotificationModel, SendNotificationController> {
    private final Button sendButton;
    private final MaterialTextField publicKey;
    private final MaterialPasswordField privateKey;
    private final MaterialTextArea message;


    public SendNotificationView(SendNotificationModel model,
                                SendNotificationController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("user.sendAlert.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        // List<String> collect = List.of(AlertType.values()).stream().map(e -> Res.get("user.sendNotification." + e.name())).collect(Collectors.toList());
        privateKey = new MaterialPasswordField(Res.get("user.sendAlert.privateKey"));
        publicKey = new MaterialTextField(Res.get("user.sendAlert.publicKey"));
        message = new MaterialTextArea(Res.get("user.sendAlert.message"));

        sendButton = new Button(Res.get("user.sendAlert.sendAlert"));
        sendButton.setDefaultButton(true);
        sendButton.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(sendButton, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, privateKey, publicKey, message, sendButton);
    }

    @Override
    protected void onViewAttached() {
        privateKey.textProperty().bindBidirectional(model.getPrivateKey());
        publicKey.textProperty().bindBidirectional(model.getPublicKey());
        message.textProperty().bindBidirectional(model.getMessage());
        sendButton.disableProperty().bind(model.getSendButtonDisabled());

        sendButton.setOnAction(e -> controller.onSendAlert());
    }

    @Override
    protected void onViewDetached() {
        privateKey.textProperty().unbindBidirectional(model.getPrivateKey());
        publicKey.textProperty().unbindBidirectional(model.getPublicKey());
        message.textProperty().unbindBidirectional(model.getMessage());
        sendButton.disableProperty().unbind();

        sendButton.setOnAction(null);
    }
}
