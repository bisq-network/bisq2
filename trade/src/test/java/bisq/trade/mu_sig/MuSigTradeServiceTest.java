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

package bisq.trade.mu_sig;

import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.identity.Identity;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.exceptions.TradeProtocolException;
import bisq.trade.exceptions.TradeProtocolFailure;
import bisq.trade.mu_sig.messages.network.MuSigReportErrorMessage;
import bisq.trade.mu_sig.messages.network.SetupTradeMessage_A;
import bisq.trade.mu_sig.protocol.MuSigProtocol;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MuSigTradeServiceTest {
    @Test
    void takeOfferRejectionIsReportedFromTheReceivingIdentity() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        Identity makerIdentity = mock(Identity.class);
        NetworkIdWithKeyPair makerNetworkIdWithKeyPair = mock(NetworkIdWithKeyPair.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        NetworkId takerNetworkId = createNetworkId("taker", 9999);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(request.getSender()).thenReturn(takerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.of(makerIdentity));
        when(makerIdentity.getNetworkId()).thenReturn(makerNetworkId);
        when(makerIdentity.getNetworkIdWithKeyPair()).thenReturn(makerNetworkIdWithKeyPair);
        when(networkService.confidentialSend(any(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair)))
                .thenReturn(CompletableFuture.completedFuture(null));
        TradeProtocolException rejection = new TradeProtocolException("x".repeat(600),
                TradeProtocolFailure.PRICE_DEVIATION);

        MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request, rejection);

        ArgumentCaptor<MuSigReportErrorMessage> captor = ArgumentCaptor.forClass(MuSigReportErrorMessage.class);
        verify(networkService).confidentialSend(captor.capture(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair));
        MuSigReportErrorMessage report = captor.getValue();
        assertThat(report.getTradeId()).isEqualTo("trade-id");
        assertThat(report.getProtocolVersion()).isEqualTo(MuSigProtocol.VERSION);
        assertThat(report.getSender()).isEqualTo(makerNetworkId);
        assertThat(report.getReceiver()).isEqualTo(takerNetworkId);
        assertThat(report.getTradeProtocolFailure()).isEqualTo(TradeProtocolFailure.PRICE_DEVIATION);
        assertThat(report.getErrorMessage()).hasSize(MuSigReportErrorMessage.MAX_LENGTH_ERROR_MESSAGE);
        assertThat(report.getStackTrace()).isEmpty();
    }

    @Test
    void takeOfferRejectionIsNotSentWithoutTheReceivingIdentity() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.empty());

        MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE));

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void takeOfferRejectionSendFailuresDoNotEscape() {
        NetworkService networkService = mock(NetworkService.class);
        IdentityService identityService = mock(IdentityService.class);
        SetupTradeMessage_A request = mock(SetupTradeMessage_A.class);
        Identity makerIdentity = mock(Identity.class);
        NetworkIdWithKeyPair makerNetworkIdWithKeyPair = mock(NetworkIdWithKeyPair.class);
        NetworkId makerNetworkId = createNetworkId("maker", 9997);
        NetworkId takerNetworkId = createNetworkId("taker", 9999);
        when(request.getTradeId()).thenReturn("trade-id");
        when(request.getReceiver()).thenReturn(makerNetworkId);
        when(request.getSender()).thenReturn(takerNetworkId);
        when(identityService.findAnyIdentityByNetworkId(makerNetworkId)).thenReturn(Optional.of(makerIdentity));
        when(makerIdentity.getNetworkId()).thenReturn(makerNetworkId);
        when(makerIdentity.getNetworkIdWithKeyPair()).thenReturn(makerNetworkIdWithKeyPair);
        when(networkService.confidentialSend(any(), eq(takerNetworkId), same(makerNetworkIdWithKeyPair)))
                .thenThrow(new IllegalStateException("synchronous send failure"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("asynchronous send failure")));

        assertThatCode(() -> MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE)))
                .doesNotThrowAnyException();
        assertThatCode(() -> MuSigTradeService.reportTakeOfferRejection(networkService, identityService, request,
                new TradeProtocolException(TradeProtocolFailure.OFFER_NOT_AVAILABLE)))
                .doesNotThrowAnyException();
    }

    private static NetworkId createNetworkId(String keyIdSuffix, int port) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        return new NetworkId(addresses, new PubKey(keyPair.getPublic(), "test-key-" + keyIdSuffix));
    }
}
