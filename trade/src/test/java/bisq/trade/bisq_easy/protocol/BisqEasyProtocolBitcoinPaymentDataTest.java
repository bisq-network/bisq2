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

package bisq.trade.bisq_easy.protocol;

import bisq.common.util.StringUtils;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyBtcAddressMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.FAILED;
import static bisq.trade.bisq_easy.protocol.BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Bitcoin payment data which the seller's node does not accept fails the trade with an expected failure, so that
 * neither side is asked to report a bug (#5210).
 */
class BisqEasyProtocolBitcoinPaymentDataTest {
    private static final String ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4";

    private final Identity sellerIdentity = identity("seller");
    private final Identity buyerIdentity = identity("buyer");

    @Test
    void tooLongAddressFailsTradeWithExpectedFailure() {
        BisqEasyOffer offer = offer("offer-id", sellerIdentity.getNetworkId(), QUOTE_AMOUNT);
        BisqEasyTrade trade = inState(sellerAsMakerTrade(contract(offer, buyerIdentity.getNetworkId(), Optional.empty()),
                        sellerIdentity),
                MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);

        // The address pasted twice
        new BisqEasySellerAsMakerProtocol(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), trade)
                .handle(btcAddressMessage(trade, ADDRESS + ADDRESS));

        assertEquals(FAILED, trade.getState());
        assertEquals(TradeProtocolFailure.INVALID_BITCOIN_PAYMENT_DATA, trade.getTradeProtocolFailure());
        assertFalse(trade.getTradeProtocolFailure().isUnexpected());
        assertTrue(trade.getBitcoinPaymentData().isEmpty());
    }

    private BisqEasyBtcAddressMessage btcAddressMessage(BisqEasyTrade trade, String bitcoinPaymentData) {
        return new BisqEasyBtcAddressMessage(StringUtils.createUid(),
                trade.getId(),
                BisqEasyProtocol.VERSION,
                buyerIdentity.getNetworkId(),
                sellerIdentity.getNetworkId(),
                bitcoinPaymentData,
                trade.getOffer());
    }
}
