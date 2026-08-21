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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.common.monetary.PriceQuote;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ProfileCardOffersControllerTest extends TestFxHeadlessSupport {
    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private MarketPriceService marketPriceService;
    private ProfileCardOffersController controller;
    private UserProfile userProfile;

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET))
                .thenAnswer(invocation -> Optional.ofNullable(marketPriceByCurrencyMap.get(MARKET)));

        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(40_000_000L));
        when(offer.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")));
        when(offer.getDisplayDirection()).thenReturn(Direction.BUY);
        when(offer.getDate()).thenReturn(1_700_000_000_000L);
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(message.getAuthorUserProfileId()).thenReturn("profile-id");
        userProfile = mock(UserProfile.class);
        when(userProfile.getId()).thenReturn("profile-id");
        when(userProfile.getNickName()).thenReturn("alice");

        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        when(messageService.getAllOfferbookMessagesWithOffer("profile-id")).thenAnswer(invocation -> Stream.of(message));
        ReputationService reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(any(UserProfile.class))).thenReturn(ReputationScore.NONE);

        ServiceProvider serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService()).thenReturn(messageService);
        when(serviceProvider.getUserService().getReputationService()).thenReturn(reputationService);

        WaitForAsyncUtils.asyncFx(() -> controller = new ProfileCardOffersController(serviceProvider));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    void rowsRebuiltAfterReactivationStillFollowMarketPriceUpdates() {
        WaitForAsyncUtils.asyncFx(() -> {
            controller.setUserProfile(userProfile);
            controller.onActivate();
            controller.onDeactivate();
            controller.onActivate();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(controller.getModel().getOfferbookListItems()).hasSize(1);
        ProfileCardOfferListItem item = controller.getModel().getOfferbookListItems().get(0);
        assertThat(item.getFormattedPercentagePrice().get()).isEqualTo(Res.get("data.na"));

        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(40_000, "USD"));
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(item.getFormattedPercentagePrice().get())
                .isEqualTo(PercentageFormatter.formatToPercentWithSignAndSymbol(0.25));
        WaitForAsyncUtils.asyncFx(() -> controller.onDeactivate());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(controller.getModel().getOfferbookListItems()).isEmpty();
    }
}
