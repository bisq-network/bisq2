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

package bisq.desktop.primary.overlay.onboarding.offer.complete;

import bisq.application.DefaultApplicationService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.popups.Popup;
import bisq.desktop.primary.main.content.components.ChatMessagesListView;
import bisq.desktop.primary.overlay.OverlayController;
import bisq.offer.spec.Direction;
import bisq.social.chat.ChatService;
import bisq.social.chat.channels.PublicTradeChannel;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import bisq.social.offer.TradeChatOfferService;
import bisq.social.user.reputation.ReputationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
public class OfferCompletedController implements Controller {
    private final OfferCompletedModel model;
    @Getter
    private final OfferCompletedView view;
    private final TradeChatOfferService tradeChatOfferService;
    private final ChatService chatService;
    private final ReputationService reputationService;
    private Pin selectedChannelPin;
    private Pin chatMessagesPin;

    public OfferCompletedController(DefaultApplicationService applicationService) {
        tradeChatOfferService = applicationService.getTradeChatOfferService();
        chatService = applicationService.getChatService();
        reputationService = applicationService.getReputationService();
        model = new OfferCompletedModel();
        view = new OfferCompletedView(model, this);
    }

    public void setDirection(Direction direction) {
        log.error("direction {}", direction);
        model.getDirection().set(direction);
    }

    public void setMarket(Market market) {
        log.error("market {}", market);
        model.getMarket().set(market);
    }

    public void setPaymentMethod(String paymentMethod) {
        log.error("paymentMethod {}", paymentMethod);
        model.getPaymentMethod().set(paymentMethod);
    }

    public void setBaseSideAmount(Monetary monetary) {
        log.error("setBaseSideAmount {}", monetary);
        model.getBaseSideAmount().set(monetary);
    }

    public void setQuoteSideAmount(Monetary monetary) {
        log.error("setQuoteSideAmount {}", monetary);
        model.getQuoteSideAmount().set(monetary);
    }

    @Override
    public void onActivate() {
        model.getSortedTakerMessages().setComparator(Comparator.comparing(ChatMessagesListView.ChatMessageListItem::getReputationScore));
        selectedChannelPin = chatService.getSelectedTradeChannel().addObserver(channel -> {
            log.error("channel " + channel);
            model.getSelectedChannel().set(channel);

            if (channel instanceof PublicTradeChannel publicTradeChannel) {
                chatMessagesPin = FxBindings.<PublicTradeChatMessage, ChatMessagesListView.ChatMessageListItem<? extends ChatMessage>>bind(model.getTakerMessages())
                        .map(chatMessage -> new ChatMessagesListView.ChatMessageListItem<>(chatMessage, chatService, reputationService))
                        .to(publicTradeChannel.getChatMessages());
            }
        });
    }

    @Override
    public void onDeactivate() {
        selectedChannelPin.unbind();
        if (chatMessagesPin != null) {
            chatMessagesPin.unbind();
        }
    }

    public void onPublishOffer() {
        //todo mock
        Market market = MarketRepository.getDefault();
        long amount = 1000000 + new Random().nextInt(1000) * 1000;
        PublicTradeChannel channel = chatService.getPublicTradeChannels().stream()
                .filter(m -> m.getMarket().get().equals(market))
                .findAny().get();
        chatService.selectTradeChannel(channel);
        tradeChatOfferService.publishTradeChatOffer(market,
                        amount,
                        Set.of("SEPA", "REVOLUT"),
                        "")
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        UIThread.run(() -> {
                            Navigation.navigateTo(NavigationTarget.CREATE_OFFER_OFFER_PUBLISHED);
                        });
                    } else {
                        //todo
                        new Popup().error(throwable.toString()).show();
                    }
                });
    }

    public void onTakeOffer() {
        //todo mock
        chatService.getPersistableStore().getChatUserById().values().stream().findAny()
                .ifPresent(chatUser -> {
                    chatService.createPrivateTradeChannel(chatUser).ifPresent(privateTradeChannel -> {
                        chatService.selectTradeChannel(privateTradeChannel);
                        chatService.sendPrivateTradeChatMessage("Hallo, I would like to take your offer.",
                                        Optional.empty(), privateTradeChannel)
                                .whenComplete((result, t) -> {
                                    OverlayController.hide();
                                    Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
                                });
                    });
                });
    }
}
