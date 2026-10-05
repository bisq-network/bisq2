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

package bisq.trade.bisq_easy;

import bisq.bonded_roles.release.AppType;
import bisq.identity.Identity;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeActionNotAppliedException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * The buyer does not send bitcoin payment data which requests another amount than the trade amount, as the seller's
 * node would fail the trade (docs/specifications/trade/bisq-easy-bitcoin-payment-data.md). The trade amount is
 * 0.001 BTC.
 */
class BisqEasyTradeServiceBitcoinPaymentDataTest {
    private static final String ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";

    private final BisqEasyTradeService service =
            new BisqEasyTradeService(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), AppType.DESKTOP);
    private final Identity buyerIdentity = identity("buyer");
    private final BisqEasyTrade trade = buyerAsTakerTrade(
            contract(offer("offer-id", identity("seller").getNetworkId(), QUOTE_AMOUNT), buyerIdentity.getNetworkId(),
                    Optional.empty()),
            buyerIdentity);

    @Test
    void dataForAnotherAmountIsNotSent() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buyerSendBitcoinPaymentData(trade, "bitcoin:" + ADDRESS + "?amount=0.01"));

        assertTrue(trade.getBitcoinPaymentData().isEmpty());
    }

    @Test
    void addressPassesTheCheck() {
        // Without a protocol of the trade the action is not applied, after the check let the data through
        assertThrows(TradeActionNotAppliedException.class, () -> service.buyerSendBitcoinPaymentData(trade, ADDRESS));
    }
}
