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

package bisq.network.p2p.message;

import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sender and the receiver of a message compute its cost factor independently, and they often run different
 * versions. So the values of MessageCostPolicy are protocol constants.
 * <p>
 * EARLIER_DATA_COST_FACTORS holds the values which the getCostFactor() methods of the types return in versions up to
 * 2.1.13. They must never change. A new type adds its value to MessageCostPolicy, not to this list.
 * StorableTypeMessageCostTest cross-checks the storage cost factors with the MetaData of the types.
 */
class MessageCostPolicyTest {
    private static final Map<String, Double> EARLIER_DATA_COST_FACTORS = Map.ofEntries(
            entry("AccountTimestamp", 0.2),
            entry("AuthorizedAccountAgeData", 0.5),
            entry("AuthorizedAccountTimestamp", 0.2),
            entry("AuthorizedAlertData", 0.5),
            entry("AuthorizedBondedReputationData", 0.5),
            entry("AuthorizedBondedRole", 0.5),
            entry("AuthorizedBurningmanListByBlock", 0.5),
            entry("AuthorizedDifficultyAdjustmentData", 0.5),
            entry("AuthorizedMarketPriceData", 0.5),
            entry("AuthorizedMinRequiredReputationScoreData", 0.5),
            entry("AuthorizedOracleNode", 0.5),
            entry("AuthorizedProofOfBurnData", 0.5),
            entry("AuthorizedSignedWitnessData", 0.5),
            entry("AuthorizedTimestampData", 0.5),
            entry("BannedUserProfileData", 0.5),
            entry("BisqEasyOfferbookMessage", 0.3),
            entry("BisqEasyOfferbookMessageReaction", 0.5),
            entry("CommonPublicChatMessage", 0.3),
            entry("CommonPublicChatMessageReaction", 0.5),
            entry("MuSigOfferMessage", 0.3),
            entry("ReleaseNotification", 0.5),
            entry("UserProfile", 0.3));

    private final KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();

    @Test
    void dataCostFactorsKeepTheValuesOfEarlierVersions() {
        EARLIER_DATA_COST_FACTORS.forEach((className, costFactor) ->
                assertEquals(Optional.of(costFactor), MessageCostPolicy.findDataCostFactor(className), className));
    }

    @Test
    void messagesWithoutContentDependentCostHaveAConstantCostFactor() {
        assertEquals(0.05, MessageCostPolicy.getCostFactor(new Ping(1)));
        assertEquals(0.05, MessageCostPolicy.getCostFactor(new CloseConnectionMessage(CloseReason.SHUTDOWN)));
    }

    @Test
    void costFactorOfConfidentialMessageGrowsWithTheCipherTextSize() {
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createConfidentialMessage(32)));
        assertEquals(0.5, MessageCostPolicy.getCostFactor(createConfidentialMessage(10_000)));
        assertEquals(1, MessageCostPolicy.getCostFactor(createConfidentialMessage(20_000)));
    }

    @Test
    void addAuthenticatedDataRequestAddsStorageAndDataCostFactorOfThePayloadType() throws GeneralSecurityException {
        double muSigOfferCostFactor = storageCostFactor("MuSigOfferMessage") +
                EARLIER_DATA_COST_FACTORS.get("MuSigOfferMessage");
        assertEquals(muSigOfferCostFactor, MessageCostPolicy.getCostFactor(createAddRequest("MuSigOfferMessage")));
        assertEquals(0.75, MessageCostPolicy.getCostFactor(createAddRequest("UserProfile")));
        // A type which the policy does not know adds nothing, so the lower bound applies
        assertEquals(0.25, MessageCostPolicy.getCostFactor(createAddRequest("UnknownType")));
    }

    @Test
    void removeAndRefreshRequestsUseTheStorageCostFactorOfTheirClassName() {
        // 0.12 is inside the bounds 0.1 and 0.3
        double muSigOfferCostFactor = storageCostFactor("MuSigOfferMessage");
        assertEquals(muSigOfferCostFactor,
                MessageCostPolicy.getCostFactor(createRemoveRequest("MuSigOfferMessage")));
        assertEquals(0.3, MessageCostPolicy.getCostFactor(createRemoveRequest("UserProfile")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRemoveRequest("UnknownType")));

        assertEquals(muSigOfferCostFactor,
                MessageCostPolicy.getCostFactor(createRefreshRequest("MuSigOfferMessage")));
        assertEquals(0.3, MessageCostPolicy.getCostFactor(createRefreshRequest("UserProfile")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRefreshRequest("UnknownType")));
    }

    @Test
    void mailboxRequestsUseTheStorageCostFactorOfTheirClassName() throws GeneralSecurityException {
        // AddMailboxRequest.from() takes the MetaData of the type from the registry
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(10_000));
        assertEquals(storageCostFactor("AckMessage") + 0.5, MessageCostPolicy.getCostFactor(
                AddMailboxRequest.from(mailboxData, keyPair, keyPair.getPublic())));

        assertEquals(storageCostFactor("TwoPartyPrivateChatMessage"),
                MessageCostPolicy.getCostFactor(createRemoveMailboxRequest("TwoPartyPrivateChatMessage")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRemoveMailboxRequest("UnknownType")));
    }

    @Test
    void messageTypeWithoutRuleHasTheHighestCostFactor() {
        // Peers send a MailboxMessage only inside a ConfidentialMessage or an AddMailboxRequest
        assertEquals(1, MessageCostPolicy.getCostFactor(new AckMessage("id")));
    }

    private static double storageCostFactor(String className) {
        return MessageCostPolicy.findStorageCostFactor(className).orElseThrow();
    }

    private ConfidentialMessage createConfidentialMessage(int cipherTextSize) {
        // The policy does not decrypt the message, so the content does not need to be valid cipher text.
        return ConfidentialMessage.fromProto(bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(bisq.security.protobuf.ConfidentialData.newBuilder()
                        .setSenderPublicKey(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                        .setIv(ByteString.copyFrom(new byte[16]))
                        .setCipherText(ByteString.copyFrom(new byte[cipherTextSize]))
                        .setSignature(ByteString.copyFrom(new byte[70])))
                .setReceiverKeyId("receiverKeyId")
                .build());
    }

    private AddAuthenticatedDataRequest createAddRequest(String className) throws GeneralSecurityException {
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(
                new DefaultAuthenticatedData(new PayloadOfType(className)), 1, pubKeyHash, System.currentTimeMillis());
        byte[] signature = SignatureUtil.sign(DigestUtil.hash(sequentialData.serializeForHash()), keyPair.getPrivate());
        return new AddAuthenticatedDataRequest(sequentialData, signature, keyPair.getPublic().getEncoded(),
                keyPair.getPublic());
    }

    private RemoveAuthenticatedDataRequest createRemoveRequest(String className) {
        //noinspection deprecation
        return RemoveAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest
                .newBuilder()
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, className).toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSequenceNumber(1)
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setCreated(System.currentTimeMillis())
                .setVersion(1)
                .setClassName(className)
                .build());
    }

    private RefreshAuthenticatedDataRequest createRefreshRequest(String className) {
        //noinspection deprecation
        return RefreshAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest
                .newBuilder()
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, className).toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSequenceNumber(1)
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setVersion(1)
                .setCreated(System.currentTimeMillis())
                .setClassName(className)
                .build());
    }

    private RemoveMailboxRequest createRemoveMailboxRequest(String className) {
        //noinspection deprecation
        return RemoveMailboxRequest.fromProto(bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setMetaData(new MetaData(MetaData.TTL_10_DAYS, className).toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setCreated(System.currentTimeMillis())
                .setVersion(1)
                .setClassName(className)
                .build());
    }

    // DistributedData whose class name is the name of a real type. The policy selects its values by the class name.
    private record PayloadOfType(String className) implements DistributedData {
        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(className);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return new MetaData(MetaData.TTL_10_DAYS, className);
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public boolean isDataInvalid(byte[] pubKeyHash) {
            return false;
        }
    }
}
