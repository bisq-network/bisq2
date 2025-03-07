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

package bisq.desktop.main.content.bisq_easy.open_trades.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class MyProtocolLogMessageBox extends PeerProtocolLogMessageBox {
    private final Subscription shouldShowTryAgainPin, messageDeliveryStatusNodePin;
    private final BisqMenuItem tryAgainMenuItem;

    public MyProtocolLogMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                   ChatMessagesListController controller) {
        super(item);

        Label deliveryStateIcon = new Label();
        BisqTooltip deliveryStateIconToolTip = new BisqTooltip();
        deliveryStateIcon.setTooltip(deliveryStateIconToolTip);

        HBox deliveryStateHBox = new HBox(deliveryStateIcon);
        deliveryStateHBox.setAlignment(Pos.CENTER);

        tryAgainMenuItem = new BisqMenuItem("try-again-grey", "try-again-white");
        tryAgainMenuItem.useIconOnly(22);
        tryAgainMenuItem.setTooltip(new BisqTooltip(Res.get("chat.message.resendMessage")));

        HBox messageStatusHbox = new HBox(5, tryAgainMenuItem, deliveryStateHBox);
        messageStatusHbox.setAlignment(Pos.CENTER);

        messageDeliveryStatusNodePin = EasyBind.subscribe(item.getMessageDeliveryStatus(), status -> {
            messageStatusHbox.setManaged(status != null);
            messageStatusHbox.setVisible(status != null);
            deliveryStateIconToolTip.setText(item.getMessageDeliveryStatusTooltip());
            if (status != null) {
                switch (status) {
                    // Successful delivery
                    case ACK_RECEIVED:
                    case MAILBOX_MSG_RECEIVED:
                        deliveryStateIcon.setGraphic(ImageUtil.getImageViewById("received-check-grey"));
                        break;
                    // Pending delivery
                    case CONNECTING:
                        deliveryStateIcon.setGraphic(ImageUtil.getImageViewById("connecting-grey"));
                        break;
                    case SENT:
                    case TRY_ADD_TO_MAILBOX:
                        deliveryStateIcon.setGraphic(ImageUtil.getImageViewById("sent-message-grey"));
                        break;
                    case ADDED_TO_MAILBOX:
                        deliveryStateIcon.setGraphic(ImageUtil.getImageViewById("mailbox-grey"));
                        break;
                    case FAILED:
                        deliveryStateIcon.setGraphic(ImageUtil.getImageViewById("undelivered-message-yellow"));
                        break;
                }
            }
        });

        shouldShowTryAgainPin = EasyBind.subscribe(item.getCanManuallyResendMessage(), showTryAgain -> {
            tryAgainMenuItem.setVisible(showTryAgain);
            tryAgainMenuItem.setManaged(showTryAgain);
            if (showTryAgain) {
                tryAgainMenuItem.setOnAction(e -> controller.onResendMessage(item.getMessageId()));
            } else {
                tryAgainMenuItem.setOnAction(null);
            }
        });

        dateTimeHBox.getChildren().add(0, messageStatusHbox);
    }

    @Override
    public void dispose() {
        shouldShowTryAgainPin.unsubscribe();
        messageDeliveryStatusNodePin.unsubscribe();
        tryAgainMenuItem.setOnAction(null);
    }
}
