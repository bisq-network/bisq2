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

package bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
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
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class OfferbookListControllerTest extends TestFxHeadlessSupport {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    // A base-side amount which converts to the quote side at one BTC price but overflows a
    // long at a higher one: 5e18 sat * 1 USD fits, 5e18 sat * 50,000 USD does not.
    private static final long BASE_SIDE_AMOUNT = 5_000_000_000_000_000_000L;
    private static final double FITTING_PRICE = 1;
    private static final double OVERFLOWING_PRICE = 50_000;

    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private final ObservableSet<String> ignoredUserProfileIds = new ObservableSet<>();
    private BisqEasyOfferbookMessageService messageService;
    private UserProfileService userProfileService;
    private ReputationService reputationService;
    private OfferbookListController controller;
    private BisqEasyOfferbookChannel channel;
    private BisqEasyOfferbookMessage message;

    @BeforeEach
    void setUp() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)));

        UserProfile authorUserProfile = mock(UserProfile.class);
        when(authorUserProfile.getNickName()).thenReturn("alice");
        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        userProfileService = userService.getUserProfileService();
        when(userProfileService.findUserProfile("author-id")).thenReturn(Optional.of(authorUserProfile));
        when(userProfileService.getIgnoredUserProfileIds()).thenReturn(ignoredUserProfileIds);
        when(userProfileService.isChatUserIgnored(any(String.class)))
                .thenAnswer(invocation -> ignoredUserProfileIds.contains(invocation.<String>getArgument(0)));
        when(userService.getUserIdentityService().getSelectedUserIdentityObservable()).thenReturn(new Observable<>(mock(UserIdentity.class)));
        reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(any(UserProfile.class))).thenReturn(ReputationScore.NONE);
        when(reputationService.getUserProfileIdWithScoreChange()).thenReturn(new Observable<>());
        when(reputationService.getScoreByUserProfileId()).thenReturn(new ObservableHashMap<>());
        when(userService.getReputationService()).thenReturn(reputationService);

        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(any(ChatMessage.class))).thenReturn(true);
        messageService = new BisqEasyOfferbookMessageService(mock(ChatService.class, RETURNS_DEEP_STUBS),
                userService,
                sellersReputationService,
                marketPriceService);
        messageService.initialize().join();

        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getShowBuyOffers()).thenReturn(new Observable<>(true));
        when(settingsService.getShowOfferListExpanded()).thenReturn(new Observable<>(true));
        when(settingsService.getShowMyOffersOnly()).thenReturn(new Observable<>(false));

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getSettingsService()).thenReturn(settingsService);
        when(serviceProvider.getUserService()).thenReturn(userService);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService()).thenReturn(messageService);

        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.BUY);
        when(offer.getAmountSpec()).thenReturn(new BaseSideFixedAmountSpec(BASE_SIDE_AMOUNT));
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getDate()).thenReturn(1_700_000_000_000L);
        message = mock(BisqEasyOfferbookMessage.class);
        when(message.getId()).thenReturn("offer-message-id");
        when(message.getAuthorUserProfileId()).thenReturn("author-id");
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));

        channel = new BisqEasyOfferbookChannel(MARKET);
        WaitForAsyncUtils.asyncFx(() -> {
            controller = new OfferbookListController(serviceProvider);
            controller.onActivate();
            controller.setSelectedChannel(channel);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() {
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();
        messageService.shutdown().join();
    }

    @Test
    void offerReceivedBeforeItsMarketPriceIsListedOnceThePriceArrives(FxRobot robot) {
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).isEmpty();
        assertThat(processedMessageIds()).isEmpty();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).containsExactly("offer-message-id");
        assertThat(processedMessageIds()).containsExactly("offer-message-id");
    }

    @Test
    void listedOfferWhoseAmountsOverflowAtANewMarketPriceIsRemoved(FxRobot robot) {
        putMarketPrice(FITTING_PRICE);
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).containsExactly("offer-message-id");

        putMarketPrice(OVERFLOWING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).isEmpty();
        assertThat(processedMessageIds()).isEmpty();
        assertThat(messageService.isValid(message)).isFalse();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).containsExactly("offer-message-id");
    }

    @Test
    void listedOfferIsRemovedWhenItsMarketPriceDisappears(FxRobot robot) {
        putMarketPrice(FITTING_PRICE);
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).containsExactly("offer-message-id");

        marketPriceByCurrencyMap.remove(MARKET);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).isEmpty();
        assertThat(processedMessageIds()).isEmpty();
    }

    @Test
    void priceChangeAfterDeactivationDoesNotRepopulateTheList(FxRobot robot) {
        channel.addChatMessage(message);
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).isEmpty();
        assertThat(processedMessageIds()).isEmpty();
        WaitForAsyncUtils.asyncFx(() -> controller.onActivate());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void offerWhoseListItemCannotBeCreatedDoesNotBlockTheOthers(FxRobot robot) {
        // Two mocks in the order the channel's set iterates them: the first one's list item
        // construction fails (its author's reputation lookup throws), the second is fine.
        List<BisqEasyOfferbookMessage> messages = new ArrayList<>(ConcurrentHashMap.<BisqEasyOfferbookMessage>newKeySet());
        while (messages.size() < 2) {
            messages.clear();
            Set<BisqEasyOfferbookMessage> orderProbe = ConcurrentHashMap.newKeySet();
            orderProbe.add(mock(BisqEasyOfferbookMessage.class));
            orderProbe.add(mock(BisqEasyOfferbookMessage.class));
            messages.addAll(orderProbe);
        }
        BisqEasyOfferbookMessage failingMessage = messages.get(0);
        BisqEasyOfferbookMessage secondMessage = messages.get(1);
        BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
        when(failingMessage.getId()).thenReturn("failing-message-id");
        when(failingMessage.getAuthorUserProfileId()).thenReturn("failing-author-id");
        when(failingMessage.hasBisqEasyOffer()).thenReturn(true);
        when(failingMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(secondMessage.getId()).thenReturn("second-message-id");
        when(secondMessage.getAuthorUserProfileId()).thenReturn("author-id");
        when(secondMessage.hasBisqEasyOffer()).thenReturn(true);
        when(secondMessage.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        UserProfile failingAuthor = mock(UserProfile.class);
        when(userProfileService.findUserProfile("failing-author-id")).thenReturn(Optional.of(failingAuthor));
        when(reputationService.getReputationScore(failingAuthor)).thenThrow(new IllegalStateException("test"));
        channel.addChatMessage(failingMessage);
        channel.addChatMessage(secondMessage);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).isEmpty();

        putMarketPrice(FITTING_PRICE);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(displayedMessageIds()).containsExactly("second-message-id");
        assertThat(processedMessageIds()).containsExactly("second-message-id");
    }

    @Test
    void ignoringAndUnignoringTheMakerRemovesAndRestoresTheListedOffer(FxRobot robot) {
        putMarketPrice(FITTING_PRICE);
        channel.addChatMessage(message);
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).containsExactly("offer-message-id");

        ignoredUserProfileIds.add("author-id");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).isEmpty();
        assertThat(processedMessageIds()).isEmpty();

        ignoredUserProfileIds.remove("author-id");
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(displayedMessageIds()).containsExactly("offer-message-id");
    }

    private void putMarketPrice(double price) {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(price, "USD"));
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
    }

    private List<String> displayedMessageIds() {
        return controller.getModel().getOfferbookListItems().stream()
                .map(item -> item.getBisqEasyOfferbookMessage().getId())
                .toList();
    }

    private Set<String> processedMessageIds() {
        return controller.getModel().getChatMessageIds();
    }
}
