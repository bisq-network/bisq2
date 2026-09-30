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

import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the Optional contract of the lifecycle-dependent payment fields (#4719):
 * absent until the respective trade phase sets them, never null.
 */
class BisqEasyTradeTest {
    @Test
    void paymentFieldsAreEmptyOnNewTrade() {
        BisqEasyTrade trade = createTrade();

        assertTrue(trade.getPaymentAccountData().isEmpty());
        assertTrue(trade.getBitcoinPaymentData().isEmpty());
        assertTrue(trade.getPaymentProof().isEmpty());
    }

    @Test
    void settersExposeValuesThroughGetters() {
        BisqEasyTrade trade = createTrade();

        trade.setPaymentAccountData(Optional.of("accountData"));
        trade.setBitcoinPaymentData(Optional.of("bc1q..."));
        trade.setPaymentProof(Optional.of("txId"));

        assertEquals(Optional.of("accountData"), trade.getPaymentAccountData());
        assertEquals(Optional.of("bc1q..."), trade.getBitcoinPaymentData());
        assertEquals(Optional.of("txId"), trade.getPaymentProof());
    }

    @Test
    void settersRejectNull() {
        BisqEasyTrade trade = createTrade();

        assertThrows(NullPointerException.class, () -> trade.setPaymentAccountData(null));
        assertThrows(NullPointerException.class, () -> trade.setBitcoinPaymentData(null));
        assertThrows(NullPointerException.class, () -> trade.setPaymentProof(null));
    }

    @Test
    void observableEmitsEmptyInitiallyAndValueOnSet() {
        BisqEasyTrade trade = createTrade();
        List<Optional<String>> received = new ArrayList<>();

        trade.paymentAccountDataObservable().addObserver(received::add);
        trade.setPaymentAccountData(Optional.of("accountData"));

        assertEquals(List.of(Optional.empty(), Optional.of("accountData")), received);
    }

    private static BisqEasyTrade createTrade() {
        BisqEasyContract contract = mock(BisqEasyContract.class);
        when(contract.getTakeOfferDate()).thenReturn(0L);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offerId");
        NetworkId takerNetworkId = mock(NetworkId.class);
        when(takerNetworkId.getId()).thenReturn("takerNetworkId");
        NetworkId makerNetworkId = mock(NetworkId.class);
        return new BisqEasyTrade(contract, true, true, mock(Identity.class), offer, takerNetworkId, makerNetworkId);
    }
}
