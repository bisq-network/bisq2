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

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.market.Market;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.testutil.TestFxHeadlessSupport;
import bisq.offer.Direction;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class OfferbookListItemTest extends TestFxHeadlessSupport {
    private static final Market MARKET = new Market("BTC", "USD", "Bitcoin", "US Dollar");

    private final ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
    private BisqEasyOfferbookMessage message;
    private UserProfile senderUserProfile;
    private ReputationService reputationService;
    private MarketPriceService marketPriceService;

    @BeforeEach
    void setUp() {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(40_000_000L));
        when(offer.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(50_000, "USD")));
        when(offer.getDirection()).thenReturn(Direction.BUY);
        when(offer.getDate()).thenReturn(1_700_000_000_000L);

        message = mock(BisqEasyOfferbookMessage.class);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(message.getAuthorUserProfileId()).thenReturn("authorProfileId");

        senderUserProfile = mock(UserProfile.class);
        when(senderUserProfile.getNickName()).thenReturn("alice");

        reputationService = mock(ReputationService.class);
        when(reputationService.getReputationScore(senderUserProfile)).thenReturn(ReputationScore.NONE);

        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.empty());
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
    }

    @Test
    void fixedPriceOfferInMarketWithoutPriceConstructsWithoutPercent() {
        OfferbookListItem item = new OfferbookListItem(message, senderUserProfile, reputationService, marketPriceService);

        assertThat(item.getPriceSpecAsPercent()).isEmpty();
        assertThat(item.getFormattedPercentagePrice().get()).isNull();
        item.dispose();
    }

    @Test
    void percentAppearsWhenMarketPriceArrives() {
        OfferbookListItem item = new OfferbookListItem(message, senderUserProfile, reputationService, marketPriceService);
        Label boundLabel = new Label();
        boundLabel.textProperty().bind(item.getFormattedPercentagePrice());

        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(40_000, "USD"));
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(item.getPriceSpecAsPercent()).hasValue(0.25);
        assertThat(item.getFormattedPercentagePrice().get()).isNotNull();
        assertThat(boundLabel.getText()).isEqualTo(item.getFormattedPercentagePrice().get());
        item.dispose();
    }

    @Test
    void marketPricePresentAtConstructionSetsPercent() {
        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(40_000, "USD"));
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));

        OfferbookListItem item = new OfferbookListItem(message, senderUserProfile, reputationService, marketPriceService);

        assertThat(item.getPriceSpecAsPercent()).hasValue(0.25);
        assertThat(item.getFormattedPercentagePrice().get()).isNotNull();
        item.dispose();
    }
}
