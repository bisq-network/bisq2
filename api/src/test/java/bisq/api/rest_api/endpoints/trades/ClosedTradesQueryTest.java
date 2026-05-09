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

package bisq.api.rest_api.endpoints.trades;

import bisq.api.dto.common.monetary.PriceQuoteDto;
import bisq.api.dto.presentation.closed_trades.ClosedTradeIndexedItem;
import bisq.api.dto.presentation.closed_trades.ClosedTradeListItemDto;
import bisq.api.dto.presentation.closed_trades.ClosedTradeListItemDto.ContractSlimDto;
import bisq.api.dto.presentation.closed_trades.ClosedTradeListItemDto.TradeSlimDto;
import bisq.api.dto.trade.TradeRoleDto;
import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;
import bisq.api.rest_api.pagination.SortDirection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosedTradesQueryTest {

    private static UserProfileDto profile(String userName, String nym) {
        // Heavyweight network/PoW fields are unused by ClosedTradesQuery.matches();
        // pass null so this test doesn't pull in the full DTO graph.
        return new UserProfileDto(0, null, null, 0, null, null, null, null, nym, userName, 0L);
    }

    private static PriceQuoteDto priceQuote(long value) {
        return new PriceQuoteDto(value, null, null, 0, 0, null);
    }

    private static ReputationScoreDto reputation() {
        return new ReputationScoreDto(0L, 0d, 0);
    }

    private static ClosedTradeIndexedItem item(String tradeId,
                                               TradeRoleDto role,
                                               BisqEasyTradeStateDto state,
                                               long takeOfferDate,
                                               Optional<Long> tradeCompletedDate,
                                               long quoteAmount,
                                               long baseAmount,
                                               long priceQuoteValue,
                                               String market,
                                               String makerUserName,
                                               String takerUserName,
                                               String btcMethodDisplay,
                                               String fiatMethodDisplay,
                                               String formattedPrice,
                                               String formattedBaseAmount,
                                               String formattedQuoteAmount,
                                               String formattedMyRole,
                                               String directionalTitle) {
        ClosedTradeListItemDto dto = new ClosedTradeListItemDto(
                new TradeSlimDto(tradeId, role, state, new ContractSlimDto(takeOfferDate)),
                profile(makerUserName, "maker-nym"),
                profile(takerUserName, "taker-nym"),
                Optional.empty(),
                priceQuote(priceQuoteValue),
                baseAmount,
                quoteAmount,
                "MAIN_CHAIN",
                "SEPA",
                reputation(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                tradeCompletedDate
        );
        return new ClosedTradeIndexedItem(
                dto, market, directionalTitle, formattedMyRole,
                formattedPrice, formattedBaseAmount, formattedQuoteAmount,
                btcMethodDisplay, fiatMethodDisplay);
    }

    private static ClosedTradeIndexedItem makerBuyer(String id, long takeOfferDate, Optional<Long> completed,
                                                     long quoteAmount, long baseAmount, long price, String market) {
        return item(id, TradeRoleDto.BUYER_AS_MAKER, BisqEasyTradeStateDto.BTC_CONFIRMED,
                takeOfferDate, completed, quoteAmount, baseAmount, price, market,
                "alice", "bob", "Bitcoin (Mainchain)", "SEPA",
                "30000.00 USD/BTC", "0.10 BTC", "3000 USD",
                "Maker", "Buy bitcoin");
    }

    private static ClosedTradeIndexedItem takerSeller(String id, long takeOfferDate, Optional<Long> completed,
                                                      long quoteAmount, long baseAmount, long price, String market,
                                                      BisqEasyTradeStateDto state) {
        return item(id, TradeRoleDto.SELLER_AS_TAKER, state,
                takeOfferDate, completed, quoteAmount, baseAmount, price, market,
                "carol", "dave", "Bitcoin (Mainchain)", "Zelle",
                "31000.00 USD/BTC", "0.20 BTC", "6200 USD",
                "Taker", "Sell bitcoin");
    }

    @Test
    void parseStates_emptyReturnsEmptySet() {
        assertTrue(ClosedTradesQuery.parseStates(List.of()).isEmpty());
        assertTrue(ClosedTradesQuery.parseStates(null).isEmpty());
    }

    @Test
    void parseStates_acceptsCommaSeparated() {
        Set<BisqEasyTradeStateDto> result = ClosedTradesQuery.parseStates(
                List.of("BTC_CONFIRMED,REJECTED"));
        assertEquals(Set.of(BisqEasyTradeStateDto.BTC_CONFIRMED, BisqEasyTradeStateDto.REJECTED), result);
    }

    @Test
    void parseStates_acceptsRepeated() {
        Set<BisqEasyTradeStateDto> result = ClosedTradesQuery.parseStates(
                List.of("btc_confirmed", "REJECTED"));
        assertEquals(Set.of(BisqEasyTradeStateDto.BTC_CONFIRMED, BisqEasyTradeStateDto.REJECTED), result);
    }

    @Test
    void parseStates_invalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ClosedTradesQuery.parseStates(List.of("NOT_A_STATE")));
    }

    @Test
    void roleFilter_buyerIncludesBothBuyerRoles() {
        assertTrue(ClosedTradesQuery.RoleFilter.BUYER.matches(TradeRoleDto.BUYER_AS_MAKER));
        assertTrue(ClosedTradesQuery.RoleFilter.BUYER.matches(TradeRoleDto.BUYER_AS_TAKER));
        assertEquals(false, ClosedTradesQuery.RoleFilter.BUYER.matches(TradeRoleDto.SELLER_AS_MAKER));
    }

    @Test
    void roleFilter_invalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ClosedTradesQuery.RoleFilter.parse(Optional.of("nope")));
    }

    @Test
    void apply_emptyCorpusReturnsEmpty() {
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertTrue(result.isEmpty());
    }

    @Test
    void apply_dateSortPrefersTradeCompletedDateOverTakeOfferDate() {
        // Trade A took offer earlier but completed later than B.
        ClosedTradeIndexedItem a = makerBuyer("A", 100L, Optional.of(500L), 100, 10, 30000, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 200L, Optional.of(300L), 200, 20, 31000, "BTC/EUR",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> desc = ClosedTradesQuery.apply(
                List.of(a, b), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(a, b), desc);
        List<ClosedTradeIndexedItem> asc = ClosedTradesQuery.apply(
                List.of(a, b), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.ASC);
        assertEquals(List.of(b, a), asc);
    }

    @Test
    void apply_dateSortFallsBackToTakeOfferDateWhenCompletedAbsent() {
        ClosedTradeIndexedItem a = makerBuyer("A", 100L, Optional.empty(), 100, 10, 30000, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 200L, Optional.empty(), 200, 20, 31000, "BTC/EUR",
                BisqEasyTradeStateDto.REJECTED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(b, a), result);
    }

    @Test
    void apply_marketSortAscDesc() {
        ClosedTradeIndexedItem usd = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem eur = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/EUR",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> asc = ClosedTradesQuery.apply(
                List.of(usd, eur), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.MARKET, SortDirection.ASC);
        assertEquals(List.of(eur, usd), asc);
    }

    @Test
    void apply_quoteAmountSort() {
        ClosedTradeIndexedItem small = makerBuyer("A", 1L, Optional.of(1L), 100, 10, 30000, "BTC/USD");
        ClosedTradeIndexedItem big = takerSeller("B", 1L, Optional.of(1L), 999, 20, 31000, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> desc = ClosedTradesQuery.apply(
                List.of(small, big), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.QUOTE_AMOUNT, SortDirection.DESC);
        assertEquals(List.of(big, small), desc);
    }

    @Test
    void apply_baseAmountSort() {
        ClosedTradeIndexedItem small = makerBuyer("A", 1L, Optional.of(1L), 100, 5, 30000, "BTC/USD");
        ClosedTradeIndexedItem big = takerSeller("B", 1L, Optional.of(1L), 100, 50, 30000, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> asc = ClosedTradesQuery.apply(
                List.of(small, big), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.BASE_AMOUNT, SortDirection.ASC);
        assertEquals(List.of(small, big), asc);
    }

    @Test
    void apply_roleSort() {
        ClosedTradeIndexedItem maker = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem taker = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> asc = ClosedTradesQuery.apply(
                List.of(taker, maker), Optional.empty(), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.ROLE, SortDirection.ASC);
        assertEquals(List.of(maker, taker), asc);
    }

    @Test
    void apply_roleFilterBuyer() {
        ClosedTradeIndexedItem buyer = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem seller = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(buyer, seller), Optional.empty(),
                Optional.of(ClosedTradesQuery.RoleFilter.BUYER), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(buyer), result);
    }

    @Test
    void apply_stateFilter() {
        ClosedTradeIndexedItem confirmed = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem rejected = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.REJECTED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(confirmed, rejected), Optional.empty(), Optional.empty(),
                Set.of(BisqEasyTradeStateDto.REJECTED),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(rejected), result);
    }

    @Test
    void apply_searchByTradeId() {
        ClosedTradeIndexedItem a = makerBuyer("alpha-id", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("beta-id", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.of("ALPH"), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(a), result);
    }

    @Test
    void apply_searchByPeerUserName() {
        ClosedTradeIndexedItem a = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.of("carol"), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(b), result);
    }

    @Test
    void apply_searchByFormattedPrice() {
        ClosedTradeIndexedItem a = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        // a -> "30000.00 USD/BTC", b -> "31000.00 USD/BTC"
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.of("31000"), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(b), result);
    }

    @Test
    void apply_searchByFormattedBaseAmount() {
        ClosedTradeIndexedItem a = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        // a -> "0.10 BTC"
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a), Optional.of("0.10"), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(a), result);
    }

    @Test
    void apply_searchByFiatMethodDisplay() {
        ClosedTradeIndexedItem a = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        // a -> SEPA, b -> Zelle
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.of("zelle"), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(List.of(b), result);
    }

    @Test
    void apply_searchBlankIgnored() {
        ClosedTradeIndexedItem a = makerBuyer("A", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD");
        ClosedTradeIndexedItem b = takerSeller("B", 1L, Optional.of(1L), 1, 1, 1, "BTC/USD",
                BisqEasyTradeStateDto.BTC_CONFIRMED);
        List<ClosedTradeIndexedItem> result = ClosedTradesQuery.apply(
                List.of(a, b), Optional.of("   "), Optional.empty(), Set.of(),
                ClosedTradesQuery.SortField.DATE, SortDirection.DESC);
        assertEquals(2, result.size());
    }

    @Test
    void sortField_parseFallback() {
        assertEquals(ClosedTradesQuery.SortField.DATE,
                ClosedTradesQuery.SortField.parse(Optional.empty(), ClosedTradesQuery.SortField.DATE));
        assertEquals(ClosedTradesQuery.SortField.MARKET,
                ClosedTradesQuery.SortField.parse(Optional.of("market"), ClosedTradesQuery.SortField.DATE));
    }

    @Test
    void sortField_parseInvalidThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ClosedTradesQuery.SortField.parse(Optional.of("nope"), ClosedTradesQuery.SortField.DATE));
    }
}
