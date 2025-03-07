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

package bisq.desktop.main.content.chat.message_container.list.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.common.data.Triple;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashMap;
import java.util.Map;

public class MessageDeliveryStatusBox extends HBox {
    private final Subscription messageDeliveryStatusNodePin;
    private final Map<String, Triple<BisqMenuItem, Label, BisqTooltip>> deliveryStateTripleByProfileId = new HashMap<>();

    public MessageDeliveryStatusBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                                    ChatMessagesListController controller) {
        super(7.5);
        setAlignment(Pos.CENTER);

        messageDeliveryStatusNodePin = EasyBind.subscribe(item.getMessageDeliveryStatusByPeerProfileId(), map -> {
            setManaged(map != null);
            setVisible(map != null);
            if (map == null) {
                return;
            }
            map.forEach((peersProfileId, triple) -> {
                MessageDeliveryStatus status = triple.getFirst();
                String ackRequestingMessageId = triple.getSecond();
                boolean canManuallyResendMessage = triple.getThird();

                Triple<BisqMenuItem, Label, BisqTooltip> deliveryStateTriple = deliveryStateTripleByProfileId.get(peersProfileId);
                BisqMenuItem tryAgainMenuItem;
                Label icon;
                BisqTooltip tooltip;
                if (deliveryStateTriple == null) {
                    tryAgainMenuItem = new BisqMenuItem("try-again-grey", "try-again-white");
                    tryAgainMenuItem.useIconOnly(22);
                    tryAgainMenuItem.setTooltip(new BisqTooltip(Res.get("chat.message.resendMessage")));

                    icon = new Label();
                    tooltip = new BisqTooltip();
                    icon.setTooltip(tooltip);

                    HBox hBox = new HBox(1, tryAgainMenuItem, icon);
                    hBox.setAlignment(Pos.CENTER);
                    getChildren().add(hBox);

                    deliveryStateTripleByProfileId.put(peersProfileId, new Triple<>(tryAgainMenuItem, icon, tooltip));
                } else {
                    tryAgainMenuItem = deliveryStateTriple.getFirst();
                    icon = deliveryStateTriple.getSecond();
                    tooltip = deliveryStateTriple.getThird();
                }

                tryAgainMenuItem.setVisible(canManuallyResendMessage);
                tryAgainMenuItem.setManaged(canManuallyResendMessage);
                if (canManuallyResendMessage) {
                    tryAgainMenuItem.setOnAction(e -> controller.onResendMessage(ackRequestingMessageId));
                } else {
                    tryAgainMenuItem.setOnAction(null);
                }


                String deliveryState = Res.get("chat.message.deliveryState." + status.name());
                if (map.size() > 1) {
                    String userName = controller.getUserName(peersProfileId);
                    tooltip.setText(Res.get("chat.message.deliveryState.multiplePeers", userName, deliveryState));
                } else {
                    tooltip.setText(deliveryState);
                }

                switch (status) {
                    // Successful delivery
                    case ACK_RECEIVED:
                    case MAILBOX_MSG_RECEIVED:
                        icon.setGraphic(ImageUtil.getImageViewById("received-check-grey"));
                        break;
                    // Pending delivery
                    case CONNECTING:
                        icon.setGraphic(ImageUtil.getImageViewById("connecting-grey"));
                        break;
                    case SENT:
                    case TRY_ADD_TO_MAILBOX:
                        icon.setGraphic(ImageUtil.getImageViewById("sent-message-grey"));
                        break;
                    case ADDED_TO_MAILBOX:
                        icon.setGraphic(ImageUtil.getImageViewById("mailbox-grey"));
                        break;
                    case FAILED:
                        icon.setGraphic(ImageUtil.getImageViewById("undelivered-message-yellow"));
                        break;
                }
            });
        });
    }

    public void dispose() {
        messageDeliveryStatusNodePin.unsubscribe();
        deliveryStateTripleByProfileId.values().forEach(triple ->
                triple.getFirst().setOnAction(null));
    }
}
