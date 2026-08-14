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
class ProfileCardOfferListItemTest extends TestFxHeadlessSupport {
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
        when(offer.getDisplayDirection()).thenReturn(Direction.BUY);
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
        ProfileCardOfferListItem item = new ProfileCardOfferListItem(message, senderUserProfile, reputationService, marketPriceService);

        assertThat(item.getPriceSpecAsPercent()).isEmpty();
        assertThat(item.getFormattedPercentagePrice().get()).isNull();
        item.dispose();
    }

    @Test
    void percentAppearsWhenMarketPriceArrives() {
        ProfileCardOfferListItem item = new ProfileCardOfferListItem(message, senderUserProfile, reputationService, marketPriceService);
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
    void emptyPercentagesSortLastInBothDirections() {
        ProfileCardOfferListItem atMarket = itemWithMarketPrice(50_000.0); // 0%
        ProfileCardOfferListItem plus25 = itemWithMarketPrice(40_000.0);   // +25%
        ProfileCardOfferListItem noPrice = itemWithMarketPrice(null);      // empty

        javafx.scene.control.TableColumn<ProfileCardOfferListItem, ?> column = new javafx.scene.control.TableColumn<>();
        java.util.Comparator<ProfileCardOfferListItem> comparator =
                bisq.desktop.main.content.user.profile_card.offers.ProfileCardOffersView.priceComparator(() -> column);

        column.setSortType(javafx.scene.control.TableColumn.SortType.ASCENDING);
        assertThat(sortAsJavaFx(List.of(noPrice, plus25, atMarket), comparator, column))
                .containsExactly(atMarket, plus25, noPrice);

        column.setSortType(javafx.scene.control.TableColumn.SortType.DESCENDING);
        assertThat(sortAsJavaFx(List.of(noPrice, atMarket, plus25), comparator, column))
                .containsExactly(plus25, atMarket, noPrice);

        atMarket.dispose();
        plus25.dispose();
        noPrice.dispose();
    }

    @Test
    void resolvingAPercentageReSortsTheRow() {
        ProfileCardOffersModel model = new ProfileCardOffersModel();
        ObservableHashMap<Market, MarketPrice> priceMap = new ObservableHashMap<>();
        MarketPriceService priceService = mock(MarketPriceService.class);
        when(priceService.findMarketPrice(MARKET)).thenReturn(Optional.empty());
        when(priceService.getMarketPriceByCurrencyMap()).thenReturn(priceMap);

        ProfileCardOfferListItem known = itemWithMarketPrice(40_000.0); // +25%
        ProfileCardOfferListItem resolving = new ProfileCardOfferListItem(
                messageWithFixedPrice(50_000), senderUserProfile, reputationService, priceService);
        model.getOfferbookListItems().addAll(known, resolving);

        javafx.scene.control.TableColumn<ProfileCardOfferListItem, ?> column = new javafx.scene.control.TableColumn<>();
        column.setSortType(javafx.scene.control.TableColumn.SortType.ASCENDING);
        javafx.collections.transformation.SortedList<ProfileCardOfferListItem> sorted =
                new javafx.collections.transformation.SortedList<>(model.getOfferbookListItems());
        sorted.setComparator((o1, o2) -> bisq.desktop.main.content.user.profile_card.offers.ProfileCardOffersView
                .priceComparator(() -> column).compare(o1, o2));

        // Empty percentage sorts last initially.
        assertThat(sorted).containsExactly(known, resolving);

        MarketPrice marketPrice = mock(MarketPrice.class);
        when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(100_000, "USD")); // -50%
        when(priceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        priceMap.put(MARKET, marketPrice);
        WaitForAsyncUtils.waitForFxEvents();

        // Once the percentage resolves to -50% it moves ahead of the +25% row.
        assertThat(sorted).containsExactly(resolving, known);

        known.dispose();
        resolving.dispose();
    }

    private static List<ProfileCardOfferListItem> sortAsJavaFx(List<ProfileCardOfferListItem> items,
                                                               java.util.Comparator<ProfileCardOfferListItem> comparator,
                                                               javafx.scene.control.TableColumn<?, ?> column) {
        // Mirrors JavaFX TableColumnComparator: a descending column reverses the comparator result.
        boolean descending = column.getSortType() == javafx.scene.control.TableColumn.SortType.DESCENDING;
        java.util.Comparator<ProfileCardOfferListItem> effective = descending ? comparator.reversed() : comparator;
        return items.stream().sorted(effective).collect(java.util.stream.Collectors.toList());
    }

    private ProfileCardOfferListItem itemWithMarketPrice(Double marketPriceValue) {
        MarketPriceService priceService = mock(MarketPriceService.class);
        when(priceService.getMarketPriceByCurrencyMap()).thenReturn(new ObservableHashMap<>());
        if (marketPriceValue == null) {
            when(priceService.findMarketPrice(MARKET)).thenReturn(Optional.empty());
        } else {
            MarketPrice marketPrice = mock(MarketPrice.class);
            when(marketPrice.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(marketPriceValue, "USD"));
            when(priceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        }
        return new ProfileCardOfferListItem(messageWithFixedPrice(50_000), senderUserProfile, reputationService, priceService);
    }

    private BisqEasyOfferbookMessage messageWithFixedPrice(double fixedPrice) {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of());
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getAmountSpec()).thenReturn(new QuoteSideFixedAmountSpec(40_000_000L));
        when(offer.getPriceSpec()).thenReturn(new FixPriceSpec(PriceQuote.fromFiatPrice(fixedPrice, "USD")));
        when(offer.getDisplayDirection()).thenReturn(Direction.BUY);
        when(offer.getDate()).thenReturn(1_700_000_000_000L);
        BisqEasyOfferbookMessage msg = mock(BisqEasyOfferbookMessage.class);
        when(msg.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        when(msg.getAuthorUserProfileId()).thenReturn("authorProfileId");
        return msg;
    }
}
