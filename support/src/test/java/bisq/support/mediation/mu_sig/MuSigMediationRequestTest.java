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

package bisq.support.mediation.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.support.dispute.SerializedSizeExceededException;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.TRADE_ID;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.assertCanBeEncrypted;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createBoundaryChatMessages;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createContract;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createUserProfile;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findBoundaryTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findLargestAcceptedTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.wrappedEnvelopeSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuSigMediationRequestTest {

    @Test
    void rejectsPayloadWhenFullEnvelopeExceedsLimit() {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile mediator = createUserProfile("mediator");
        MuSigContract contract = createMediationContract(requester, peer, mediator);
        int textLength = findBoundaryTextLength(textLengthCandidate ->
                createMediationRequestProto(contract,
                        requester,
                        peer,
                        createBoundaryChatMessages(requester, peer, textLengthCandidate),
                        mediator.getNetworkId()));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        bisq.support.protobuf.MuSigMediationRequest innerProto =
                createMediationRequestProto(contract, requester, peer, chatMessages, mediator.getNetworkId());

        assertTrue(innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE);
        assertTrue(wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE);
        SerializedSizeExceededException exception = assertThrows(SerializedSizeExceededException.class,
                () -> new MuSigMediationRequest(TRADE_ID,
                        contract,
                        requester,
                        peer,
                        chatMessages,
                        mediator.getNetworkId()));
        assertEquals("Serialized mediation request size must not exceed " + MAX_SERIALIZED_SIZE + " bytes",
                exception.getMessage());
    }

    @Test
    void acceptedPayloadCanBeEncrypted() throws GeneralSecurityException {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile mediator = createUserProfile("mediator");
        MuSigContract contract = createMediationContract(requester, peer, mediator);
        int textLength = findLargestAcceptedTextLength(textLengthCandidate ->
                createMediationRequestProto(contract,
                        requester,
                        peer,
                        createBoundaryChatMessages(requester, peer, textLengthCandidate),
                        mediator.getNetworkId()));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        MuSigMediationRequest request = new MuSigMediationRequest(TRADE_ID,
                contract,
                requester,
                peer,
                chatMessages,
                mediator.getNetworkId());

        assertCanBeEncrypted(request.serialize(), mediator.getNetworkId());
    }

    private MuSigContract createMediationContract(UserProfile maker,
                                                  UserProfile taker,
                                                  UserProfile mediator) {
        return createContract(maker, taker, Optional.of(mediator), Optional.empty(), "offer-mediation-boundary");
    }

    private bisq.support.protobuf.MuSigMediationRequest createMediationRequestProto(MuSigContract contract,
                                                                                    UserProfile requester,
                                                                                    UserProfile peer,
                                                                                    List<MuSigOpenTradeMessage> chatMessages,
                                                                                    NetworkId mediatorNetworkId) {
        return bisq.support.protobuf.MuSigMediationRequest.newBuilder()
                .setTradeId(TRADE_ID)
                .setContract(contract.toProto(false))
                .setRequester(requester.toProto(false))
                .setPeer(peer.toProto(false))
                .setMediatorNetworkId(mediatorNetworkId.toProto(false))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.toValueProto(false))
                        .toList())
                .build();
    }
}
