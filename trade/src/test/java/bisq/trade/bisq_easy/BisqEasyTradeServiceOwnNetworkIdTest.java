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
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import bisq.trade.bisq_easy.protocol.messages.BisqEasyRejectTradeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.inState;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A node sends its trade messages under the network ID which its trade peer stored for it. The maker's identity can
 * have a newer address map than its offer, for example if the maker added a transport after it published the offer.
 * Takers of version 2.1.13 compare the sender with the network ID of the offer including the addresses.
 */
class BisqEasyTradeServiceOwnNetworkIdTest {
    private ServiceProvider serviceProvider;
    private BisqEasyTradeService service;

    @BeforeEach
    void setUp() {
        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        service = new BisqEasyTradeService(serviceProvider, AppType.DESKTOP);
    }

    @AfterEach
    void tearDown() {
        service.shutdown().join();
    }

    @Test
    void makerSendsUnderTheNetworkIdOfItsOfferAfterItsAddressesChanged() {
        Identity makerIdentity = identity("maker");
        NetworkId offersNetworkId = withOtherAddress(makerIdentity.getNetworkId(), 2000);
        BisqEasyOffer offer = offer("offer-id", offersNetworkId, QUOTE_AMOUNT);
        Identity takerIdentity = identity("taker");
        BisqEasyContract contract = contract(offer, takerIdentity.getNetworkId(), Optional.empty());
        // The maker of a sell offer is the seller. The taker addressed its take offer request to the network ID of the
        // offer, and the maker's trade holds its current identity.
        BisqEasyTrade trade = inState(new BisqEasyTrade(contract,
                        false,
                        false,
                        makerIdentity,
                        offer,
                        takerIdentity.getNetworkId(),
                        offersNetworkId),
                BisqEasyTradeState.MAKER_SENT_TAKE_OFFER_RESPONSE__SELLER_DID_NOT_SENT_ACCOUNT_DATA__SELLER_DID_NOT_RECEIVED_BTC_ADDRESS);
        service.getPersistableStore().addTrade(trade);
        service.initialize().join();

        service.rejectTrade(trade);

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.REJECTED);
        NetworkService networkService = serviceProvider.getNetworkService();
        ArgumentCaptor<EnvelopePayloadMessage> sentMessage = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        verify(networkService).confidentialSend(sentMessage.capture(), eq(takerIdentity.getNetworkId()), any());
        assertThat(sentMessage.getValue()).isInstanceOf(BisqEasyRejectTradeMessage.class);
        assertThat(((BisqEasyRejectTradeMessage) sentMessage.getValue()).getSender()).isEqualTo(offersNetworkId);
    }

    private static NetworkId withOtherAddress(NetworkId networkId, int port) {
        Address otherAddress = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(otherAddress.getTransportType(), otherAddress)),
                networkId.getPubKey());
    }
}
