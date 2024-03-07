package bisq.desktop.main.content.bisq_easy.open_trades;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.common.Icons;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.chatMessages.ChatMessageListItem;
import bisq.desktop.main.content.components.chatMessages.ChatMessagesListView;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import de.jensd.fx.fontawesome.AwesomeDude;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
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

        systemMessageBg.getChildren().add(deliveryState);

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
