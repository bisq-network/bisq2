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

package bisq.desktop.main.content.chat.message_container.list;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.settings.ChatMessageType;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class ChatMessagesListControllerTest extends TestFxHeadlessSupport {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    // 5e18 sat converts to the quote side at 1 USD per BTC but overflows a long at 50,000 USD.
    private static final long BASE_SIDE_AMOUNT = 5_000_000_000_000_000_000L;
    private static final double FITTING_PRICE = 1;
    private static final double OVERFLOWING_PRICE = 50_000;

    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private final Observable<ChatChannel<? extends ChatMessage>> selectedChannel = new Observable<>();
    private BisqEasyOfferbookMessageService messageService;
    private ChatMessagesListController controller;
    private BisqEasyOfferbookChannel channel;
    private BisqEasyOfferbookMessage message;

    @BeforeEach
    void setUp() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)));

        UserProfile authorUserProfile = mock(UserProfile.class);
        when(authorUserProfile.getId()).thenReturn("author-id");
        when(authorUserProfile.getNickName()).thenReturn("alice");
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        when(userService.getUserProfileService().findUserProfile("author-id")).thenReturn(Optional.of(authorUserProfile));
        when(userService.getUserProfileService().getIgnoredUserProfileIds()).thenReturn(new ObservableSet<>());
        when(userService.getUserIdentityService().getSelectedUserIdentityObservable()).thenReturn(new Observable<>(mock(UserIdentity.class)));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.findReputationScore(any(UserProfile.class))).thenReturn(Optional.empty());
        when(reputationService.getUserProfileIdWithScoreChange()).thenReturn(new Observable<>());
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        when(userService.getReputationService()).thenReturn(reputationService);

        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(any(ChatMessage.class))).thenReturn(true);
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getChatChannelSelectionServices().get(ChatChannelDomain.BISQ_EASY_OFFERBOOK).getSelectedChannel())
                .thenReturn(selectedChannel);
        messageService = new BisqEasyOfferbookMessageService(chatService, userService, sellersReputationService, marketPriceService);
        messageService.initialize().join();

        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getBisqEasyOfferbookMessageTypeFilter()).thenReturn(new Observable<>(ChatMessageType.ALL));
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getChatService()).thenReturn(chatService);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService().getAuthorizedBondedRoleStream())
                .thenAnswer(invocation -> Stream.empty());
        when(serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService()).thenReturn(messageService);
        when(serviceProvider.getNetworkService().getMessageDeliveryStatusByMessageId()).thenReturn(new ObservableHashMap<>());
        when(serviceProvider.getNetworkService().getResendMessageService()).thenReturn(Optional.empty());

        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.BUY);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(BASE_SIDE_AMOUNT));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of());
        message = mock(BisqEasyOfferbookMessage.class);
        when(message.getId()).thenReturn("offer-message-id");
        when(message.getAuthorUserProfileId()).thenReturn("author-id");
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(message.getCitation()).thenReturn(Optional.empty());

        channel = new BisqEasyOfferbookChannel(MARKET);
        WaitForAsyncUtils.asyncFx(() -> {
            controller = new ChatMessagesListController(serviceProvider,
                    userProfile -> {
                    },
                    chatMessage -> {
                    },
                    chatMessage -> {
                    },
                    () -> {
                    },
                    ChatChannelDomain.BISQ_EASY_OFFERBOOK);
            controller.onActivate();
        });
        WaitForAsyncUtils.waitForFxEvents();
        selectedChannel.set(channel);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() {
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();
        messageService.shutdown().join();
        Transitions.setSettingsService(null);
    }

    @Test
    void offerMessageReceivedBeforeItsMarketPriceIsListedOnceThePriceArrives() {
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).doesNotContain("offer-message-id");
        assertThat(processedMessageIds()).doesNotContain("offer-message-id");

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).contains("offer-message-id");
        assertThat(processedMessageIds()).contains("offer-message-id");
    }

    @Test
    void listedOfferMessageWhoseAmountsOverflowAtANewMarketPriceIsRemoved() {
        putMarketPrice(FITTING_PRICE);
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).contains("offer-message-id");

        putMarketPrice(OVERFLOWING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).doesNotContain("offer-message-id");
        assertThat(processedMessageIds()).doesNotContain("offer-message-id");
        assertThat(messageService.isValid(message)).isFalse();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).contains("offer-message-id");
    }

    @Test
    void priceChangeAfterDeactivationDoesNotRepopulateTheList() {
        channel.addChatMessage(message);
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).doesNotContain("offer-message-id");
        assertThat(processedMessageIds()).doesNotContain("offer-message-id");
        WaitForAsyncUtils.asyncFx(() -> controller.onActivate());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void messageWhoseListItemCannotBeCreatedDoesNotBreakTheChannelBinding() {
        putMarketPrice(FITTING_PRICE);
        Optional<BisqEasyOffer> offer = message.getBisqEasyOffer();
        BisqEasyOfferbookMessage failingMessage = mock(BisqEasyOfferbookMessage.class);
        when(failingMessage.getId()).thenReturn("failing-message-id");
        when(failingMessage.getAuthorUserProfileId()).thenReturn("failing-author-id");
        when(failingMessage.hasBisqEasyOffer()).thenReturn(true);
        when(failingMessage.getBisqEasyOffer()).thenReturn(offer);
        when(failingMessage.getCitation()).thenThrow(new IllegalStateException("test"));
        BisqEasyOfferbookMessage laterMessage = mock(BisqEasyOfferbookMessage.class);
        when(laterMessage.getId()).thenReturn("later-message-id");
        when(laterMessage.getAuthorUserProfileId()).thenReturn("author-id");
        when(laterMessage.hasBisqEasyOffer()).thenReturn(true);
        when(laterMessage.getBisqEasyOffer()).thenReturn(offer);
        when(laterMessage.getCitation()).thenReturn(Optional.empty());
        BisqEasyOfferbookChannel otherChannel = new BisqEasyOfferbookChannel(new Market("BTC", "EUR", "Bitcoin", "Euro"));
        otherChannel.addChatMessage(failingMessage);
        otherChannel.addChatMessage(message);

        selectedChannel.set(otherChannel);
        WaitForAsyncUtils.waitForFxEvents();
        otherChannel.addChatMessage(laterMessage);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).contains("offer-message-id", "later-message-id");
        assertThat(displayedMessageIds()).doesNotContain("failing-message-id");
    }

    private void putMarketPrice(double price) {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(price, "USD"));
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
    }

    private List<String> displayedMessageIds() {
        return controller.getModel().getChatMessages().stream()
                .map(item -> item.getChatMessage().getId())
                .toList();
    }

    private Set<String> processedMessageIds() {
        return controller.getModel().getChatMessageIds();
    }
}
