/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package bisq.api.rest_api.endpoints.trades;

import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TradeStatusQueryTest {
    private static BisqEasyTrade trade(String tradeId, String offerId,
                                       BisqEasyTradeState state) {
        BisqEasyTrade trade = mock(BisqEasyTrade.class);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(trade.getId()).thenReturn(tradeId);
        when(trade.getOffer()).thenReturn(offer);
        when(offer.getId()).thenReturn(offerId);
        when(trade.getTradeState()).thenReturn(state);
        return trade;
    }

    @Test
    void includesOnlyRequestedOfferAndNoPrivateData() {
        BisqEasyTrade a = trade("trade-a", "offer-1", BisqEasyTradeState.INIT);
        BisqEasyTrade b = trade("trade-b", "other-offer", BisqEasyTradeState.INIT);
        List<TradeStatusDto> matches = TradeStatusQuery.byOffer(
                Stream.of(a, b), Stream.empty(), "offer-1");

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().tradeId()).isEqualTo("trade-a");
        assertThat(matches.getFirst().offerId()).isEqualTo("offer-1");
        assertThat(matches.getFirst().lifecycle()).isEqualTo("OPEN");
        assertThat(matches.getFirst().tradeState().name()).isEqualTo("INIT");
    }

    @Test
    void closedStoreWinsWhenTheSameTradeIsTemporarilyInBoth() {
        BisqEasyTrade trade = trade("trade-1", "offer-1", BisqEasyTradeState.BTC_CONFIRMED);
        BisqEasyClosedTrade closed = new BisqEasyClosedTrade(trade, null, null);
        List<TradeStatusDto> matches = TradeStatusQuery.byOffer(
                Stream.of(trade), Stream.of(closed), "offer-1");
        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().lifecycle()).isEqualTo("CLOSED");
    }

    @Test
    void aMissingTradeDoesNotBecomeAClaimOfFailureOrSafeRetry() {
        assertThat(TradeStatusQuery.byOffer(Stream.empty(), Stream.empty(), "offer-1")).isEmpty();
    }

    @Test
    void rejectMissingOrMalformedOfferFilter() {
        for (String id : new String[]{null, "", "../other", "contains space"}) {
            assertThatThrownBy(() ->
                    TradeStatusQuery.byOffer(Stream.empty(), Stream.empty(), id))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
