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

package bisq.support.mediation.bisq_easy;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.chat.ChatMessageType;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage;
import bisq.common.market.Market;
import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.amount.spec.BaseSideFixedAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.support.dispute.SerializedSizeExceededException;
import bisq.user.profile.UserProfile;
import com.google.protobuf.Any;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static bisq.support.dispute.ChatMessagePruning.MAX_SERIALIZED_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BisqEasyMediationRequestTest {
    private static final String BOUNDARY_TRADE_ID = "trade-boundary";
    private static final String BOUNDARY_CHANNEL_ID = "channel-boundary";

    @Test
    void rejectsPayloadWhenFullEnvelopeExceedsLimit() {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile mediator = createUserProfile("mediator");
        BisqEasyContract contract = createContract(requester, peer, mediator);
        int textLength = findBoundaryTextLength(contract, requester, peer, mediator.getNetworkId());
        List<BisqEasyOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        bisq.support.protobuf.MediationRequest innerProto = createMediationRequestProto(contract, requester, peer, chatMessages, mediator.getNetworkId());

        assertTrue(innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE);
        assertTrue(wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE);
        SerializedSizeExceededException exception = assertThrows(SerializedSizeExceededException.class,
                () -> new BisqEasyMediationRequest(BOUNDARY_TRADE_ID,
                        contract,
                        requester,
                        peer,
                        chatMessages,
                        mediator.getNetworkId()));
        assertEquals("Serialized mediation request size must not exceed " + MAX_SERIALIZED_SIZE + " bytes", exception.getMessage());
    }

    @Test
    void acceptedPayloadCanBeEncrypted() throws GeneralSecurityException {
        UserProfile requester = createUserProfile("requester");
        UserProfile peer = createUserProfile("peer");
        UserProfile mediator = createUserProfile("mediator");
        BisqEasyContract contract = createContract(requester, peer, mediator);
        int textLength = findLargestAcceptedTextLength(contract, requester, peer, mediator.getNetworkId());
        List<BisqEasyOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, textLength);
        BisqEasyMediationRequest request = new BisqEasyMediationRequest(BOUNDARY_TRADE_ID,
                contract,
                requester,
                peer,
                chatMessages,
                mediator.getNetworkId());

        assertTrue(request.getSerializedSize() <= MAX_SERIALIZED_SIZE);
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(request.serialize(),
                mediator.getNetworkId().getPubKey().getPublicKey(),
                KeyGeneration.generateDefaultEcKeyPair());
        assertTrue(confidentialData.getCipherText().length <= 20_000);
    }

    private int findBoundaryTextLength(BisqEasyContract contract,
                                       UserProfile requester,
                                       UserProfile peer,
                                       NetworkId mediatorNetworkId) {
        int low = 1;
        int high = 8_999;
        while (low <= high) {
            int candidate = (low + high) / 2;
            List<BisqEasyOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, candidate);
            bisq.support.protobuf.MediationRequest innerProto = createMediationRequestProto(contract, requester, peer, chatMessages, mediatorNetworkId);
            if (innerProto.getSerializedSize() <= MAX_SERIALIZED_SIZE && wrappedEnvelopeSize(innerProto) > MAX_SERIALIZED_SIZE) {
                return candidate;
            }
            if (innerProto.getSerializedSize() > MAX_SERIALIZED_SIZE) {
                high = candidate - 1;
            } else {
                low = candidate + 1;
            }
        }
        throw new AssertionError("Could not find mediation request size boundary");
    }

    private int findLargestAcceptedTextLength(BisqEasyContract contract,
                                              UserProfile requester,
                                              UserProfile peer,
                                              NetworkId mediatorNetworkId) {
        int low = 1;
        int high = 8_999;
        int result = -1;
        while (low <= high) {
            int candidate = (low + high) / 2;
            List<BisqEasyOpenTradeMessage> chatMessages = createBoundaryChatMessages(requester, peer, candidate);
            bisq.support.protobuf.MediationRequest innerProto = createMediationRequestProto(contract, requester, peer, chatMessages, mediatorNetworkId);
            if (wrappedEnvelopeSize(innerProto) <= MAX_SERIALIZED_SIZE) {
                result = candidate;
                low = candidate + 1;
            } else {
                high = candidate - 1;
            }
        }
        if (result < 0) {
            throw new AssertionError("Could not find accepted mediation request size boundary");
        }
        return result;
    }

    private bisq.support.protobuf.MediationRequest createMediationRequestProto(BisqEasyContract contract,
                                                                               UserProfile requester,
                                                                               UserProfile peer,
                                                                               List<BisqEasyOpenTradeMessage> chatMessages,
                                                                               NetworkId mediatorNetworkId) {
        return bisq.support.protobuf.MediationRequest.newBuilder()
                .setTradeId(BOUNDARY_TRADE_ID)
                .setContract(contract.toProto(false))
                .setRequester(requester.toProto(false))
                .setPeer(peer.toProto(false))
                .addAllChatMessages(chatMessages.stream()
                        .map(message -> message.toValueProto(false))
                        .toList())
                .setMediatorNetworkId(mediatorNetworkId.toProto(false))
                .build();
    }

    private int wrappedEnvelopeSize(bisq.support.protobuf.MediationRequest innerProto) {
        return bisq.network.protobuf.EnvelopePayloadMessage.newBuilder()
                .setExternalNetworkMessage(bisq.network.protobuf.ExternalNetworkMessage.newBuilder()
                        .setPayload(Any.pack(innerProto)))
                .build()
                .getSerializedSize();
    }

    private List<BisqEasyOpenTradeMessage> createBoundaryChatMessages(UserProfile requester,
                                                                      UserProfile peer,
                                                                      int textLength) {
        long now = System.currentTimeMillis();
        return List.of(createChatMessage("message-1", requester, peer, "x".repeat(textLength), now),
                createChatMessage("message-2", peer, requester, "y".repeat(textLength), now + 1));
    }

    private BisqEasyOpenTradeMessage createChatMessage(String messageId,
                                                       UserProfile sender,
                                                       UserProfile receiver,
                                                       String text,
                                                       long date) {
        return new BisqEasyOpenTradeMessage(BOUNDARY_TRADE_ID,
                messageId,
                BOUNDARY_CHANNEL_ID,
                sender,
                receiver.getId(),
                receiver.getNetworkId(),
                text,
                Optional.empty(),
                date,
                false,
                Optional.empty(),
                ChatMessageType.TEXT,
                Optional.empty(),
                Set.of());
    }

    private BisqEasyContract createContract(UserProfile maker,
                                            UserProfile taker,
                                            UserProfile mediator) {
        return new BisqEasyContract(System.currentTimeMillis(),
                createOffer(maker),
                taker.getNetworkId(),
                100_000,
                3_500_000,
                new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                new FiatPaymentMethodSpec(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                Optional.of(mediator),
                new MarketPriceSpec(),
                0);
    }

    private BisqEasyOffer createOffer(UserProfile maker) {
        return new BisqEasyOffer(maker.getNetworkId(),
                Direction.BUY,
                new Market("BTC", "EUR", "Bitcoin", "Euro"),
                new BaseSideFixedAmountSpec(100_000),
                new MarketPriceSpec(),
                List.of(BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN)),
                List.of(FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK)),
                "",
                List.of("en"),
                "1.0.0");
    }

    private UserProfile createUserProfile(String nickName) {
        return new UserProfile(0,
                nickName,
                new ProofOfWork(new byte[20], 0, null, 1, new byte[72], 0),
                0,
                createNetworkId(nickName),
                "",
                "",
                "");
    }

    private NetworkId createNetworkId(String keyId) {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }
}
