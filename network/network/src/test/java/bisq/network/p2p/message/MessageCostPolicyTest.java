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

import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.network.Address;
import bisq.common.network.TransportType;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.node.Capability;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.handshake.ConnectionHandshake;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.inventory.Inventory;
import bisq.network.p2p.services.data.inventory.InventoryRequest;
import bisq.network.p2p.services.data.inventory.InventoryResponse;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedSequentialData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeRequest;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeResponse;
import bisq.network.p2p.services.peer_group.keep_alive.Ping;
import bisq.network.p2p.services.peer_group.keep_alive.Pong;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeRequest;
import bisq.network.p2p.services.peer_group.network_load.NetworkLoadExchangeResponse;
import bisq.network.p2p.services.reporting.Report;
import bisq.network.p2p.services.reporting.ReportRequest;
import bisq.network.p2p.services.reporting.ReportResponse;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The sender and the receiver of a message compute its cost factor independently, and they often run different
 * versions. So the values of MessageCostPolicy are protocol constants.
 * <p>
 * EARLIER_DATA_COST_FACTORS holds the values which the getCostFactor() methods of the types return in versions up to
 * 2.1.13. They must never change. A new type adds its value to MessageCostPolicy, not to this list.
 * storage-costs-ed35a572.csv freezes the storage inputs and exact costs from before the extraction. Unlike the
 * cross-check with current MetaData in StorableTypeMessageCostTest, it cannot change together with production code.
 * Only the four explicit rounding exceptions below may differ from those historical doubles; no tolerance is used.
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

    @ParameterizedTest
    @MethodSource("messagesWithHistoricalConstantCosts")
    void messagesWithoutContentDependentCostKeepTheirHistoricalCost(EnvelopePayloadMessage message, double cost) {
        assertCosts(message, cost, cost, message.getClass().getName());
    }

    private static Stream<Arguments> messagesWithHistoricalConstantCosts() {
        Capability capability = new Capability(1, Address.from("127.0.0.1", 8000),
                new ArrayList<>(List.of(TransportType.CLEAR)), new ArrayList<>(), "2.1.13");
        NetworkLoad networkLoad = new NetworkLoad();
        Report report = new Report(new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), 0, 0, 0, 0);
        // Values of the individual getCostFactor() implementations at ed35a572.
        return Stream.of(
                Arguments.of(new ConnectionHandshake.Request(capability, Optional.empty(), networkLoad, 0), 0.05),
                Arguments.of(new ConnectionHandshake.Response(capability, networkLoad), 0.05),
                Arguments.of(new CloseConnectionMessage(CloseReason.SHUTDOWN), 0.05),
                Arguments.of(new Ping(1), 0.05),
                Arguments.of(new Pong(1), 0.05),
                Arguments.of(new NetworkLoadExchangeRequest(1, networkLoad), 0.05),
                Arguments.of(new NetworkLoadExchangeResponse(1, networkLoad), 0.05),
                Arguments.of(new PeerExchangeRequest(1, new ArrayList<>()), 0.1),
                Arguments.of(new PeerExchangeResponse(1, new ArrayList<>()), 0.1),
                Arguments.of(new InventoryRequest(new HashSetFilter(InventoryFilterType.HASH_SET_V2, new ArrayList<>()), 1), 0.25),
                Arguments.of(new InventoryResponse(new Inventory(List.of(), false), 1), 0.1),
                Arguments.of(new ReportRequest("request"), 0.5),
                Arguments.of(new ReportResponse("request", report), 0.2));
    }

    @ParameterizedTest
    @CsvSource({"0, 0.1", "32, 0.1", "1999, 0.1", "2000, 0.1", "2001, 0.10005",
            "10000, 0.5", "19999, 0.99995", "20000, 1.0"})
    void confidentialMessagesKeepTheirHistoricalCostAtSizeBoundaries(int cipherTextSize, double cost)
            throws UnresolvableProtobufException {
        assertCosts(createConfidentialMessage(cipherTextSize), cost, cost, "cipherTextSize=" + cipherTextSize);
    }

    @ParameterizedTest(name = "{0}")
    @CsvFileSource(resources = "storage-costs-ed35a572.csv")
    void storageAndRequestCostsKeepHistoricalValuesExceptForDocumentedRounding(String className,
                                                                             long ttl,
                                                                             int maxMapSize,
                                                                             double historicalStorageCost)
            throws GeneralSecurityException, UnresolvableProtobufException {
        MetaData historicalMetaData = new MetaData(ttl, className, maxMapSize);
        double storageCost = storageCostAfterDocumentedRounding(className, historicalStorageCost);
        assertEquals(Optional.of(storageCost), MessageCostPolicy.findStorageCostFactor(className), className);

        double historicalRemoveCost = bound(0.1, 0.3, historicalStorageCost);
        double removeCost = bound(0.1, 0.3, storageCost);
        double removeCostToPay = Math.max(removeCost, historicalRemoveCost);
        if (EARLIER_DATA_COST_FACTORS.containsKey(className)) {
            double historicalDataCost = EARLIER_DATA_COST_FACTORS.get(className);
            // Only market-price data changes the final authenticated-add cost after rounding.
            double addCost = className.equals("AuthorizedMarketPriceData") ?
                    0.6 : bound(0.25, 0.75, historicalStorageCost + historicalDataCost);
            assertCosts(createAddRequest(historicalMetaData), addCost, addCost, className + " add");
            // The receiver expects the policy value for every version: no version keeps the received MetaData
            for (int version = 0; version <= 3; version++) {
                assertCosts(createRemoveRequest(historicalMetaData, version), removeCost, removeCostToPay,
                        className + " remove version=" + version);
                assertCosts(createRefreshRequest(historicalMetaData, version), removeCost, removeCostToPay,
                        className + " refresh version=" + version);
            }
        } else {
            for (int version = 0; version <= 3; version++) {
                assertCosts(createRemoveMailboxRequest(historicalMetaData, version), removeCost, removeCostToPay,
                        className + " remove mailbox version=" + version);
            }
            for (int size : mailboxCipherTextSizes(storageCost)) {
                double confidentialCost = bound(0.1, 1, size / 20_000d);
                double historicalAddCost = bound(0.25, 1, historicalStorageCost + confidentialCost);
                double addCost = bound(0.25, 1, storageCost + confidentialCost);
                double addCostToPay = Math.max(addCost, historicalAddCost);
                MailboxData mailboxData = new MailboxData(historicalMetaData, createConfidentialMessage(size));
                for (int version = 0; version <= 3; version++) {
                    MailboxData versionedData = MailboxData.fromProto(mailboxData.toProto(false).toBuilder()
                            .setVersion(version).build());
                    assertCosts(AddMailboxRequest.from(versionedData, keyPair, keyPair.getPublic()),
                            addCost, addCostToPay, className + " add mailbox version=" + version + " size=" + size);
                }
            }
        }
    }

    @Test
    void appendOnlyRequestsKeepTheirHistoricalMinimumCost() {
        // There are no concrete append-only types at ed35a572. Exercise the historical bounds with synthetic data.
        for (String className : List.of("AccountTimestamp", "UserProfile", "AuthorizedAccountAgeData", "UnknownType")) {
            AddAppendOnlyDataRequest request = new AddAppendOnlyDataRequest(
                    new PayloadOfType(new MetaData(MetaData.TTL_10_DAYS, className)));
            assertCosts(request, 0.5, 0.5, className + " append only");
        }
    }

    private static double storageCostAfterDocumentedRounding(String className, double historicalCost) {
        // Rule 10 and Compatibility in docs/specifications/network/message-cost.md deliberately round these values.
        // Do not round the entire fixture here: any further difference must fail as an unexpected protocol change.
        return switch (className) {
            case "AckMessage" -> 0.06; // Previously 0.060000000000000005
            case "AuthorizedAccountTimestamp" -> 0.3; // Previously 0.30000000000000004
            case "AuthorizedMarketPriceData" -> 0.1; // Previously 0.10006944444444445
            case "MuSigOfferMessage" -> 0.12; // Previously 0.12000000000000001
            default -> historicalCost;
        };
    }

    private static int[] mailboxCipherTextSizes(double storageCost) {
        int lowerBoundary = (int) Math.round((0.25 - storageCost) * 20_000);
        int upperBoundary = (int) Math.round((1 - storageCost) * 20_000);
        return IntStream.of(0, 32, 1999, 2000, 2001, 10_000, 19_999, 20_000,
                        lowerBoundary - 1, lowerBoundary, lowerBoundary + 1,
                        upperBoundary - 1, upperBoundary, upperBoundary + 1)
                .filter(size -> size >= 0 && size <= 20_000)
                .distinct()
                .toArray();
    }

    // Formulas from the individual request classes at ed35a572, using frozen inputs rather than production lookups.
    private static double bound(double min, double max, double value) {
        return Math.max(min, Math.min(max, value));
    }

    private static void assertCosts(EnvelopePayloadMessage message, double expectedCost, double expectedCostToPay,
                                   String context) {
        assertEquals(expectedCost, MessageCostPolicy.getCostFactor(message), context + " receiver");
        assertEquals(expectedCostToPay, MessageCostPolicy.getCostFactorToPay(message), context + " sender");
    }

    @Test
    void addAuthenticatedDataRequestAddsStorageAndDataCostFactorOfThePayloadType() throws GeneralSecurityException {
        assertEquals(0.42, MessageCostPolicy.getCostFactor(createAddRequest("MuSigOfferMessage")));
        assertEquals(0.75, MessageCostPolicy.getCostFactor(createAddRequest("UserProfile")));
        // A type which the policy does not know adds nothing, so the lower bound applies
        assertEquals(0.25, MessageCostPolicy.getCostFactor(createAddRequest("UnknownType")));
    }

    @Test
    void removeAndRefreshRequestsUseTheStorageCostFactorOfTheirClassName() throws UnresolvableProtobufException {
        // 0.12 is inside the bounds 0.1 and 0.3
        assertEquals(0.12, MessageCostPolicy.getCostFactor(createRemoveRequest("MuSigOfferMessage")));
        assertEquals(0.3, MessageCostPolicy.getCostFactor(createRemoveRequest("UserProfile")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRemoveRequest("UnknownType")));

        assertEquals(0.12, MessageCostPolicy.getCostFactor(createRefreshRequest("MuSigOfferMessage")));
        assertEquals(0.3, MessageCostPolicy.getCostFactor(createRefreshRequest("UserProfile")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRefreshRequest("UnknownType")));
    }

    @Test
    void mailboxRequestsUseTheStorageCostFactorOfTheirClassName()
            throws GeneralSecurityException, UnresolvableProtobufException {
        // AddMailboxRequest.from() takes the MetaData of the type from the registry
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(10_000));
        assertEquals(0.56, MessageCostPolicy.getCostFactor(
                AddMailboxRequest.from(mailboxData, keyPair, keyPair.getPublic())));

        assertEquals(0.16,
                MessageCostPolicy.getCostFactor(createRemoveMailboxRequest("TwoPartyPrivateChatMessage")));
        assertEquals(0.1, MessageCostPolicy.getCostFactor(createRemoveMailboxRequest("UnknownType")));
    }

    @Test
    void senderPaysTheCostFactorOfThePolicyForMessagesWithoutMetaDataField()
            throws GeneralSecurityException, UnresolvableProtobufException {
        Ping ping = new Ping(1);
        assertEquals(MessageCostPolicy.getCostFactor(ping), MessageCostPolicy.getCostFactorToPay(ping));
        AddAuthenticatedDataRequest addRequest = createAddRequest("UserProfile");
        assertEquals(MessageCostPolicy.getCostFactor(addRequest), MessageCostPolicy.getCostFactorToPay(addRequest));
        ConfidentialMessage confidentialMessage = createConfidentialMessage(10_000);
        assertEquals(MessageCostPolicy.getCostFactor(confidentialMessage),
                MessageCostPolicy.getCostFactorToPay(confidentialMessage));
    }

    @Test
    void messageTypeWithoutRuleHasTheHighestCostFactor() {
        // Peers send a MailboxMessage only inside a ConfidentialMessage or an AddMailboxRequest
        assertEquals(1, MessageCostPolicy.getCostFactor(new AckMessage("id")));
    }

    private ConfidentialMessage createConfidentialMessage(int cipherTextSize) throws UnresolvableProtobufException {
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
        return createAddRequest(new MetaData(MetaData.TTL_10_DAYS, className));
    }

    private AddAuthenticatedDataRequest createAddRequest(MetaData metaData) throws GeneralSecurityException {
        byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
        AuthenticatedSequentialData sequentialData = new AuthenticatedSequentialData(
                new DefaultAuthenticatedData(new PayloadOfType(metaData, pubKeyHash)), 1, pubKeyHash,
                System.currentTimeMillis());
        byte[] signature = SignatureUtil.sign(DigestUtil.hash(sequentialData.serializeForHash()), keyPair.getPrivate());
        // Without signature V2 and with version 0, as nodes of earlier versions send it
        return new AddAuthenticatedDataRequest(sequentialData, signature, keyPair.getPublic().getEncoded(),
                keyPair.getPublic(), new byte[]{}, 0);
    }

    private RemoveAuthenticatedDataRequest createRemoveRequest(String className) throws UnresolvableProtobufException {
        return createRemoveRequest(new MetaData(MetaData.TTL_10_DAYS, className), 1);
    }

    private RemoveAuthenticatedDataRequest createRemoveRequest(MetaData metaData, int version)
            throws UnresolvableProtobufException {
        //noinspection deprecation
        return RemoveAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RemoveAuthenticatedDataRequest
                .newBuilder()
                .setMetaData(metaData.toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSequenceNumber(1)
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setCreated(System.currentTimeMillis())
                .setVersion(version)
                .setClassName(metaData.getClassName())
                .build());
    }

    private RefreshAuthenticatedDataRequest createRefreshRequest(String className)
            throws UnresolvableProtobufException {
        return createRefreshRequest(new MetaData(MetaData.TTL_10_DAYS, className), 1);
    }

    private RefreshAuthenticatedDataRequest createRefreshRequest(MetaData metaData, int version)
            throws UnresolvableProtobufException {
        //noinspection deprecation
        return RefreshAuthenticatedDataRequest.fromProto(bisq.network.protobuf.RefreshAuthenticatedDataRequest
                .newBuilder()
                .setMetaData(metaData.toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setOwnerPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSequenceNumber(1)
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setVersion(version)
                .setCreated(System.currentTimeMillis())
                .setClassName(metaData.getClassName())
                .build());
    }

    private RemoveMailboxRequest createRemoveMailboxRequest(String className) throws UnresolvableProtobufException {
        return createRemoveMailboxRequest(new MetaData(MetaData.TTL_10_DAYS, className), 1);
    }

    private RemoveMailboxRequest createRemoveMailboxRequest(MetaData metaData, int version)
            throws UnresolvableProtobufException {
        //noinspection deprecation
        return RemoveMailboxRequest.fromProto(bisq.network.protobuf.RemoveMailboxRequest.newBuilder()
                .setMetaData(metaData.toProto(false))
                .setHash(ByteString.copyFrom(new byte[20]))
                .setReceiverPublicKeyBytes(ByteString.copyFrom(keyPair.getPublic().getEncoded()))
                .setSignature(ByteString.copyFrom(new byte[70]))
                .setCreated(System.currentTimeMillis())
                .setVersion(version)
                .setClassName(metaData.getClassName())
                .build());
    }

    // Synthetic data whose class name selects the cost of a real type, without depending on higher-level modules. It
    // names the key which publishes it as its owner, as valid data of the real types does.
    private record PayloadOfType(MetaData metaData, byte[] ownerPubKeyHash)
            implements AppendOnlyData, OwnershipVerifiableData {
        private PayloadOfType(MetaData metaData) {
            this(metaData, new byte[20]);
        }

        @Override
        public void verify() {
        }

        @Override
        public byte[] getOwnerPubKeyHash() {
            return ownerPubKeyHash;
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(getClassName());
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return metaData;
        }

        @Override
        public String getClassName() {
            return metaData.getClassName();
        }

        // network.proto: message MetaData { ...; string className = 4; }
        private static final CanonicalSchema<PayloadOfType> DEFAULT_SCHEMA = CanonicalSchema.<PayloadOfType>newBuilder()
                .string(4, PayloadOfType::getClassName)
                .build();

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            CanonicalSchema<PayloadOfType> schema = switch (encodingPurpose) {
                case DEFAULT -> DEFAULT_SCHEMA;
                case SIGNATURE -> DEFAULT_SCHEMA;
                case STORAGE_KEY -> DEFAULT_SCHEMA;
                case PROOF_OF_WORK -> DEFAULT_SCHEMA;
                case TRADE_CONTRACT -> DEFAULT_SCHEMA;
            };
            return canonicalEncoder.encode(this, schema, encodingPurpose);
        }
    }
}
