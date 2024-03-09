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

package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import de.jensd.fx.fontawesome.AwesomeDude;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class MyProtocolLogMessageBox extends PeerProtocolLogMessageBox {
    protected final Label deliveryState;
    private final Subscription messageDeliveryStatusIconPin;

    public MyProtocolLogMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                   ChatMessagesListView.Controller controller) {
        super(item);

        deliveryState = new Label();
        deliveryState.setCursor(Cursor.HAND);
        deliveryState.setTooltip(new BisqTooltip(true));

        systemMessageBg.getChildren().remove(message);
        HBox hBox = new HBox(10, message, deliveryState);
        hBox.setAlignment(Pos.CENTER);
        systemMessageBg.getChildren().add(0, hBox);

        deliveryState.getTooltip().textProperty().bind(item.getMessageDeliveryStatusTooltip());

        messageDeliveryStatusIconPin = EasyBind.subscribe(item.getMessageDeliveryStatusIcon(), icon -> {
                    deliveryState.setManaged(icon != null);
                    deliveryState.setVisible(icon != null);
                    if (icon != null) {
                        AwesomeDude.setIcon(deliveryState, icon, AwesomeDude.DEFAULT_ICON_SIZE);
                        item.getMessageDeliveryStatusIconColor().ifPresent(color ->
                                Icons.setAwesomeIconColor(deliveryState, color));

                        boolean allowResend = item.getMessageDeliveryStatus() == MessageDeliveryStatus.FAILED;
                        String messageId = item.getMessageId();
                        if (allowResend && controller.canResendMessage(messageId)) {
                            deliveryState.setOnMouseClicked(e -> controller.onResendMessage(messageId));
                            deliveryState.setCursor(Cursor.HAND);
                        } else {
                            deliveryState.setOnMouseClicked(null);
                            deliveryState.setCursor(null);
                        }
                    }
                }
        );
    }

    @Override
    public void cleanup() {
        deliveryState.getTooltip().textProperty().unbind();
        messageDeliveryStatusIconPin.unsubscribe();
    }
}
