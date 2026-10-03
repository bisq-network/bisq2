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

package bisq.network.p2p.node.authorization;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2TokenService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.security.keys.KeyGeneration;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the cost of MessageCostPolicy end to end: a node creates a token for a message, the message and the token
 * travel as protobuf, and the receiver parses both and checks the token. Parsing replaces the field metaData of mailbox
 * data and remove requests in versions 1 to 3 by the registered MetaData, and keeps it in version 0.
 */
class MessageCostAuthorizationTest {
    private static final String RECEIVER_ADDRESS = "receiver.onion:1000";
    // At this load no bound of the difficulty applies: the difficulty is 65 536 × 0.5 × cost factor
    private static final double LOAD = 0.5;
    private static final NetworkLoad NETWORK_LOAD = new NetworkLoad(LOAD, 1);
    // A time to live of 100 days gives the storage cost factor 1 in the formula of earlier versions
    private static final MetaData HIGH_META_DATA = new MetaData(MetaData.TTL_100_DAYS,
            MetaData.DEFAULT_PRIORITY,
            AckMessage.class.getSimpleName(),
            0);

    // A registered mailbox type for which the policy has no value. Its MetaData gives the storage cost factor 0.4 in
    // the formula of earlier versions. The storage layer never instantiates the type, so an interface is sufficient.
    private interface TypeWithoutPolicyValue extends MailboxMessage {
        MetaData META_DATA = new MetaData(TimeUnit.DAYS.toMillis(30), TypeWithoutPolicyValue.class.getSimpleName());
    }

    private final HashCashV2TokenService tokenService = new HashCashV2TokenService(new HashCashProofOfWorkService());
    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private int messageCounter;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        StorableTypesRegistry.register(TypeWithoutPolicyValue.class, TypeWithoutPolicyValue.META_DATA);
    }

    @Test
    void relayedRequestWithReplacedMetaDataIsAuthorized()
            throws GeneralSecurityException, UnresolvableProtobufException {
        // A relay receives a request whose metaData a sender or another relay changed. Parsing replaces it.
        bisq.network.protobuf.AddMailboxRequest.Builder builder = createAddMailboxRequest(AckMessage.META_DATA)
                .toValueProto(false).toBuilder();
        //noinspection deprecation
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setMetaData(HIGH_META_DATA.toProto(false));
        AddMailboxRequest relayedRequest = AddMailboxRequest.fromProto(builder.build());

        HashCashV2Token token = createToken(relayedRequest);

        // The registered MetaData of AckMessage gives the value of the policy, so the relay pays only that value
        assertEquals(difficulty(MessageCostPolicy.getCostFactor(relayedRequest)),
                token.getProofOfWork().getDifficulty());
        assertTrue(isAuthorizedAfterTransfer(relayedRequest, token));
    }

    @Test
    void requestOfVersion0CarriesTheRegisteredMetaDataAndIsAuthorized()
            throws GeneralSecurityException, UnresolvableProtobufException {
        // Parsing replaces the received metaData by the registered MetaData in every version, so a sender cannot raise
        // the cost which relays pay. Version 0 parses only without signature V2.
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage());
        //noinspection deprecation
        RemoveMailboxRequest request = RemoveMailboxRequest.fromProto(
                RemoveMailboxRequest.from(mailboxData, receiverKeyPair).toValueProto(false).toBuilder()
                        .setVersion(0)
                        .clearSignatureV2()
                        .setMetaData(HIGH_META_DATA.toProto(false))
                        .build());

        HashCashV2Token token = createToken(request);

        // The registered MetaData of AckMessage has the values of the policy
        assertEquals(difficulty(0.1), token.getProofOfWork().getDifficulty());
        assertTrue(isAuthorizedAfterTransfer(request, token));
    }

    @Test
    void senderPaysTheHigherCostWhichEarlierVersionsExpect()
            throws GeneralSecurityException, UnresolvableProtobufException {
        AddMailboxRequest request = createAddMailboxRequest(TypeWithoutPolicyValue.META_DATA);
        double costFactorToPay = MessageCostPolicy.getCostFactorToPay(request);
        assertTrue(costFactorToPay > MessageCostPolicy.getCostFactor(request));

        HashCashV2Token token = createToken(request);

        // The receiver of this version expects the lower value of the policy and accepts the higher difficulty
        assertEquals(difficulty(costFactorToPay), token.getProofOfWork().getDifficulty());
        assertTrue(isAuthorizedAfterTransfer(request, token));
    }

    private HashCashV2Token createToken(EnvelopePayloadMessage message) {
        return tokenService.createToken(message, NETWORK_LOAD, RECEIVER_ADDRESS, messageCounter++);
    }

    // Sends the message and the token as protobuf. The receiver parses both, as Connection does.
    private boolean isAuthorizedAfterTransfer(EnvelopePayloadMessage message, AuthorizationToken token)
            throws UnresolvableProtobufException {
        EnvelopePayloadMessage receivedMessage = EnvelopePayloadMessage.fromProto(message.toProto(false));
        AuthorizationToken receivedToken = AuthorizationToken.fromProto(token.toProto(false));
        return tokenService.isAuthorized(receivedMessage,
                receivedToken,
                NETWORK_LOAD,
                Optional.empty(),
                ConcurrentHashMap.newKeySet(),
                RECEIVER_ADDRESS);
    }

    private static double difficulty(double costFactor) {
        return HashCashV2TokenService.TARGET_DIFFICULTY * costFactor * LOAD * 1;
    }

    private AddMailboxRequest createAddMailboxRequest(MetaData metaData)
            throws GeneralSecurityException, UnresolvableProtobufException {
        MailboxData mailboxData = new MailboxData(metaData, createConfidentialMessage());
        return AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
    }

    private ConfidentialMessage createConfidentialMessage() throws UnresolvableProtobufException {
        // The token service does not decrypt the message, so the content does not need to be valid cipher text.
        return ConfidentialMessage.fromProto(bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(bisq.security.protobuf.ConfidentialData.newBuilder()
                        .setSenderPublicKey(ByteString.copyFrom(senderKeyPair.getPublic().getEncoded()))
                        .setIv(ByteString.copyFrom(new byte[16]))
                        .setCipherText(ByteString.copyFrom(new byte[32]))
                        .setSignature(ByteString.copyFrom(new byte[70])))
                .setReceiverKeyId("receiverKeyId")
                .build());
    }
}
