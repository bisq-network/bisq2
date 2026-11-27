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
import bisq.contract.Role;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static bisq.trade.bisq_easy.BisqEasyTradeTestData.QUOTE_AMOUNT;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyOffer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.buyerAsTakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.contract;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.identity;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.offer;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsMakerTrade;
import static bisq.trade.bisq_easy.BisqEasyTradeTestData.sellerAsTakerTrade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A stored trade can stay in INIT, for example when the node stopped before it wrote the next state. The desktop
 * shows the reject button in INIT, so the protocol of each role must apply the rejection there
 * (docs/specifications/trade/bisq-easy/reject-trade.md).
 */
class BisqEasyTradeServiceRejectTradeTest {
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

    @ParameterizedTest(name = "{0}")
    @EnumSource(Trader.class)
    void tradeInInitCanBeRejected(Trader trader) {
        Identity makerIdentity = identity("maker");
        Identity takerIdentity = identity("taker");
        BisqEasyOffer offer = trader.isMakerTheBuyer()
                ? buyOffer("offer-id", makerIdentity.getNetworkId(), QUOTE_AMOUNT)
                : offer("offer-id", makerIdentity.getNetworkId(), QUOTE_AMOUNT);
        BisqEasyContract contract = contract(offer, takerIdentity.getNetworkId(), Optional.empty());
        BisqEasyTrade trade = switch (trader) {
            case SELLER_AS_MAKER -> sellerAsMakerTrade(contract, makerIdentity);
            case BUYER_AS_MAKER -> buyerAsMakerTrade(contract, makerIdentity);
            case BUYER_AS_TAKER -> buyerAsTakerTrade(contract, takerIdentity);
            case SELLER_AS_TAKER -> sellerAsTakerTrade(contract, takerIdentity);
        };
        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.INIT);
        service.getPersistableStore().addTrade(trade);
        service.initialize().join();

        service.rejectTrade(trade);

        assertThat(trade.getState()).isEqualTo(BisqEasyTradeState.REJECTED);
        assertThat(trade.getInterruptTradeInitiator().get()).isEqualTo(trade.isMaker() ? Role.MAKER : Role.TAKER);
        NetworkService networkService = serviceProvider.getNetworkService();
        ArgumentCaptor<EnvelopePayloadMessage> sentMessage = ArgumentCaptor.forClass(EnvelopePayloadMessage.class);
        NetworkId peersNetworkId = trade.isMaker() ? takerIdentity.getNetworkId() : makerIdentity.getNetworkId();
        verify(networkService).confidentialSend(sentMessage.capture(), eq(peersNetworkId), any());
        assertThat(sentMessage.getValue()).isInstanceOf(BisqEasyRejectTradeMessage.class);
    }

    enum Trader {
        SELLER_AS_MAKER,
        BUYER_AS_MAKER,
        BUYER_AS_TAKER,
        SELLER_AS_TAKER;

        // The maker of a buy offer is the buyer, its taker the seller
        boolean isMakerTheBuyer() {
            return this == BUYER_AS_MAKER || this == SELLER_AS_TAKER;
        }
    }
}
