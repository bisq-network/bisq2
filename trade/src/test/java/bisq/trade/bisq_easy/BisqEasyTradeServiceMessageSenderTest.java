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
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyConfirmFiatReceiptMessage;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyReportErrorMessage;
import bisq.trade.exceptions.TradeProtocolFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Only the trade peer can change the state of a trade with a trade message. The state machine moves a trade to FAILED
 * when a message handler throws, and the handlers throw for a message of another sender. So the service drops such a
 * message before the protocol handles it.
 */
class BisqEasyTradeServiceMessageSenderTest {
    private final NetworkId me = networkId("me", 1001);
    private final NetworkId peer = networkId("peer", 1002);
    private final NetworkId thirdParty = networkId("third-party", 1003);

    private BisqEasyTradeService service;
    private BisqEasyTrade trade;

    @BeforeEach
    void setUp() {
        service = new BisqEasyTradeService(mock(ServiceProvider.class, RETURNS_DEEP_STUBS), AppType.DESKTOP);
        // I am the taker and the buyer. The peer is the maker.
        BisqEasyContract contract = mock(BisqEasyContract.class);
        when(contract.getTakeOfferDate()).thenReturn(0L);
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn("offerId");
        trade = new BisqEasyTrade(contract, true, true, mock(Identity.class), offer, me, peer);
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void messageOfAThirdPartyWithTheTradeIdDoesNotReachTheProtocol() {
        startServiceWithTrade();

        assertThatCode(() -> service.onMessage(errorReport(thirdParty))).doesNotThrowAnyException();

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
        assertThat(trade.getEventQueue()).isEmpty();
    }

    @Test
    void messageOfTheTradePeerReachesTheProtocol() {
        startServiceWithTrade();

        service.onMessage(errorReport(peer));

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.FAILED_AT_PEER);
    }

    // The maker published the offer, which holds the network ID that we store for the peer, before it added a
    // transport. Its messages carry its key with the new addresses.
    @Test
    void messageOfTheTradePeerUnderOtherAddressesReachesTheProtocol() {
        startServiceWithTrade();

        service.onMessage(errorReport(withOtherAddress(peer, 2002)));

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.FAILED_AT_PEER);
    }

    @Test
    void queuedMessageOfAThirdPartyIsDroppedWhenItIsProcessed() {
        // The protocol of the trade does not exist yet, so the service keeps the message for later
        service.onMessage(errorReport(thirdParty));
        startServiceWithTrade();
        // A message of the trade peer makes the service process the kept messages again. The trade has no transition
        // for this message in its first state, so the message itself does not change the state.
        BisqEasyConfirmFiatReceiptMessage peerMessage = new BisqEasyConfirmFiatReceiptMessage("message-of-peer",
                trade.getId(), "1.0.0", peer, me);

        assertThatCode(() -> service.onMessage(peerMessage)).doesNotThrowAnyException();

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
        assertThat(trade.getEventQueue()).containsExactly(peerMessage);
    }

    private void startServiceWithTrade() {
        service.getPersistableStore().addTrade(trade);
        service.initialize().join();
        assertThat(service.findProtocol(trade.getId())).isPresent();
        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
    }

    private BisqEasyReportErrorMessage errorReport(NetworkId sender) {
        return new BisqEasyReportErrorMessage("message-of-" + sender.getPubKey().getKeyId(),
                trade.getId(),
                "1.0.0",
                sender,
                me,
                "error",
                "",
                TradeProtocolFailure.UNKNOWN);
    }

    private static NetworkId networkId(String keyId, int port) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId));
    }

    private static NetworkId withOtherAddress(NetworkId networkId, int port) {
        Address otherAddress = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
