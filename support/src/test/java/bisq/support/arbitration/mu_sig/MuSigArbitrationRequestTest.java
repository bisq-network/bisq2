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

package bisq.support.arbitration.mu_sig;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeMessage;
import bisq.contract.ContractService;
import bisq.contract.mu_sig.MuSigContract;
import bisq.network.identity.NetworkId;
import bisq.support.dispute.SerializedSizeExceededException;
import bisq.support.mediation.MediationPayoutDistributionType;
import bisq.support.mediation.MediationResultReason;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.user.profile.UserProfile;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;

import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.TRADE_ID;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.assertCanBeEncrypted;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createContract;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createBoundaryChatMessages;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.createUserProfile;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findBoundaryTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.findLargestAcceptedTextLength;
import static bisq.support.mu_sig.MuSigRequestSizeTestFixtures.wrappedEnvelopeSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuSigArbitrationRequestTest {
    private static final byte[] MEDIATION_RESULT_SIGNATURE = new byte[72];

    @Test
    void rejectsPayloadWhenFullEnvelopeExceedsLimit() {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile arbitrator = createUserProfile("arbitrator");
        MuSigContract contract = createArbitrationContract(requester, peer, arbitrator);
        MuSigMediationResult mediationResult = createMediationResult(contract);
        int textLength = findBoundaryTextLength(textLengthCandidate ->
                createArbitrationRequestProto(contract,
                        mediationResult,
                        requester,
                        peer,
                        createBoundaryChatMessages(requester, peer, textLengthCandidate),
                        arbitrator.getNetworkId()));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        bisq.support.protobuf.MuSigArbitrationRequest innerProto =
                createArbitrationRequestProto(contract, mediationResult, requester, peer, chatMessages, arbitrator.getNetworkId());

        assertTrue(innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE);
        assertTrue(wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE);
        SerializedSizeExceededException exception = assertThrows(SerializedSizeExceededException.class,
                () -> new MuSigArbitrationRequest(TRADE_ID,
                        contract,
                        mediationResult,
                        MEDIATION_RESULT_SIGNATURE,
                        requester,
                        peer,
                        chatMessages,
                        arbitrator.getNetworkId()));
        assertEquals("Serialized arbitration request size must not exceed " + MAX_SERIALIZED_SIZE + " bytes",
                exception.getMessage());
    }

    @Test
    void acceptedPayloadCanBeEncrypted() throws GeneralSecurityException {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile arbitrator = createUserProfile("arbitrator");
        MuSigContract contract = createArbitrationContract(requester, peer, arbitrator);
        MuSigMediationResult mediationResult = createMediationResult(contract);
        int textLength = findLargestAcceptedTextLength(textLengthCandidate ->
                createArbitrationRequestProto(contract,
                        mediationResult,
                        requester,
                        peer,
                        createBoundaryChatMessages(requester, peer, textLengthCandidate),
                        arbitrator.getNetworkId()));
        List<MuSigOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        MuSigArbitrationRequest request = new MuSigArbitrationRequest(TRADE_ID,
                contract,
                mediationResult,
                MEDIATION_RESULT_SIGNATURE,
                requester,
                peer,
                chatMessages,
                arbitrator.getNetworkId());

        assertCanBeEncrypted(request.serialize(), arbitrator.getNetworkId());
    }

    private MuSigContract createArbitrationContract(UserProfile maker,
                                                    UserProfile taker,
                                                    UserProfile arbitrator) {
        return createContract(maker, taker, Optional.empty(), Optional.of(arbitrator), "offer-arbitration-boundary");
    }

    private MuSigMediationResult createMediationResult(MuSigContract contract) {
        return new MuSigMediationResult(ContractService.getContractHash(contract),
                MediationResultReason.OTHER,
                MediationPayoutDistributionType.NO_PAYOUT,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    private bisq.support.protobuf.MuSigArbitrationRequest createArbitrationRequestProto(MuSigContract contract,
                                                                                        MuSigMediationResult mediationResult,
                                                                                        UserProfile requester,
                                                                                        UserProfile peer,
                                                                                        List<MuSigOpenTradeMessage> chatMessages,
                                                                                        NetworkId arbitratorNetworkId) {
        return bisq.support.protobuf.MuSigArbitrationRequest.newBuilder()
                .setTradeId(TRADE_ID)
                .setContract(contract.toProto(false))
                .setMuSigMediationResult(mediationResult.toProto(false))
                .setMediationResultSignature(ByteString.copyFrom(MEDIATION_RESULT_SIGNATURE))
                .setRequester(requester.toProto(false))
                .setPeer(peer.toProto(false))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.toValueProto(false))
                        .toList())
                .setArbitratorNetworkId(arbitratorNetworkId.toProto(false))
                .build();
    }
}
