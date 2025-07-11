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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.data.Pair;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class SellerStateLightning3b extends BaseState {
    private final Controller controller;

    public SellerStateLightning3b(ServiceProvider serviceProvider,
                                  BisqEasyTrade bisqEasyTrade,
                                  BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Pin channelPin, chatMessagesPin;

        private Controller(ServiceProvider serviceProvider,
                           BisqEasyTrade bisqEasyTrade,
                           BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.getBuyerHasConfirmedBitcoinReceipt().set(null);

            // We check if we have received the log message from the peer which signals that they have received the bitcoin payment.
            // We use the i18n key not the string itself for comparison, thus even changing the string will not affect us.
            // Though the key ('bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln') must not be changed.
            // This is a bit of a hack, but we did not want to alter the trade protocol for this relative small feature.
            ChatChannelSelectionService selectionService = chatService.getChatChannelSelectionServices().get(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
            channelPin = selectionService.getSelectedChannel().addObserver(channel -> {
                if (channel instanceof BisqEasyOpenTradeChannel bisqEasyOpenTradeChannel) {
                    String peersUserName = bisqEasyOpenTradeChannel.getPeer().getUserName();
                    if (chatMessagesPin != null) {
                        chatMessagesPin.unbind();
                    }
                    chatMessagesPin = bisqEasyOpenTradeChannel.getChatMessages().addObserver(new CollectionObserver<>() {
                        @Override
                        public void add(BisqEasyOpenTradeMessage message) {
                            if (message.getChatMessageType() == ChatMessageType.PROTOCOL_LOG_MESSAGE && message.getText().isPresent()) {
                                String encodedLogMessage = message.getText().get();
                                String expectedEncoded = Res.encode("bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln", peersUserName);
                                if (encodedLogMessage.equals(expectedEncoded)) {
                                    UIThread.run(() -> model.getBuyerHasConfirmedBitcoinReceipt().set(Res.get("bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.ln")));
                                }
                            }
                        }

                        @Override
                        public void remove(Object element) {
                        }

                        @Override
                        public void clear() {
                        }
                    });
                }
            });
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();

            channelPin.unbind();
            if (chatMessagesPin != null) {
                chatMessagesPin.unbind();
                chatMessagesPin = null;
            }
        }

        private void onButtonClicked() {
            // todo should we send a system message? if so we should change the text
            //sendTradeLogMessage(Res.get("bisqEasy.tradeState.info.phase3b.tradeLogMessage", model.getChannel().getMyUserIdentity().getUserName()));
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final StringProperty buyerHasConfirmedBitcoinReceipt = new SimpleStringProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final WaitingAnimation waitingAnimation;
        private final HBox waitingInfo, btcReceiptConfirmedHBox;
        private final WrappingText btcReceiptConfirmed, btcReceiptConfirmedHeadline, btcReceiptConfirmedInfo;
        private Subscription buyerHasConfirmedBitcoinReceiptPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.seller.phase3b.headline.ln"));
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.seller.phase3b.info.ln"));
            waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            Pair<WrappingText, HBox> confirmPair = FormUtils.getConfirmInfo();
            btcReceiptConfirmed = confirmPair.getFirst();
            btcReceiptConfirmedHBox = confirmPair.getSecond();

            btcReceiptConfirmedHeadline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.headline.ln"));
            btcReceiptConfirmedInfo = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.seller.phase3b.receiptConfirmed.info.ln"));

            button = new Button();
            button.setDefaultButton(true);
            VBox.setMargin(button, new Insets(25, 0, 5, 0));

            VBox.setMargin(btcReceiptConfirmedHBox, new Insets(0, 0, 15, 0));
            root.getChildren().addAll(btcReceiptConfirmedHBox, btcReceiptConfirmedHeadline, btcReceiptConfirmedInfo, waitingInfo, button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            buyerHasConfirmedBitcoinReceiptPin = EasyBind.subscribe(model.getBuyerHasConfirmedBitcoinReceipt(), buyerHasConfirmedBitcoinReceipt -> {
                btcReceiptConfirmed.setText(buyerHasConfirmedBitcoinReceipt);
                boolean hasConfirmed = StringUtils.isNotEmpty(buyerHasConfirmedBitcoinReceipt);
                btcReceiptConfirmedHBox.setVisible(hasConfirmed);
                btcReceiptConfirmedHBox.setManaged(hasConfirmed);
                btcReceiptConfirmedHeadline.setVisible(hasConfirmed);
                btcReceiptConfirmedHeadline.setManaged(hasConfirmed);
                btcReceiptConfirmedInfo.setVisible(hasConfirmed);
                btcReceiptConfirmedInfo.setManaged(hasConfirmed);

                waitingInfo.setVisible(!hasConfirmed);
                waitingInfo.setManaged(!hasConfirmed);

                button.setDefaultButton(hasConfirmed);
                button.setText(hasConfirmed
                        ? Res.get("bisqEasy.tradeState.info.seller.phase3b.confirmButton.ln")
                        : Res.get("bisqEasy.tradeState.info.seller.phase3b.confirmButton.skipWaitForConfirmation.ln"));
            });

            button.setOnAction(e -> controller.onButtonClicked());
            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            buyerHasConfirmedBitcoinReceiptPin.unsubscribe();

            button.setOnAction(null);
            waitingAnimation.stop();
        }
    }
}
