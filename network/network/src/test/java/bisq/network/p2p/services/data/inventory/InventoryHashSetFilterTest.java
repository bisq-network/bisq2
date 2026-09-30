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

package bisq.network.p2p.services.data.inventory;

import bisq.common.data.ByteArray;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.RemoveDataRequest;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilter;
import bisq.network.p2p.services.data.inventory.filter.InventoryFilterType;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilter;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterEntry;
import bisq.network.p2p.services.data.inventory.filter.hash_set.HashSetFilterService;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.Sequential;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.append.AppendOnlyData;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.PersistenceService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.CLOCK_BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The inventory hash set filter types HASH_SET (storage key V1) and HASH_SET_V2 (storage key V2). See
 * docs/specifications/network/inventory-hash-set-filter.md.
 * <p>
 * Requests without signature V2 are accepted only before SignedRequest.SIGNATURE_V1_CUTOFF_DATE, so the nodes of these
 * tests have a time before that date.
 */
class InventoryHashSetFilterTest {
    private static final int MAX_SIZE = 2_000_000;

    @TempDir
    Path appDataDir;

    private DataService dataService;
    private StorageService storageService;
    // The node which responds to the requests of dataService, if a test needs one
    private Optional<DataService> responder = Optional.empty();
    private AuthenticatedDataStorageService authenticatedDataStore;
    private final KeyPair ownerA = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair ownerB = KeyGeneration.generateDefaultEcKeyPair();

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(InventoryTestData.class, InventoryTestData.META_DATA);
        StorableTypesRegistry.register(InventoryTestAppendOnlyData.class, InventoryTestAppendOnlyData.META_DATA);
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @BeforeEach
    void setUp() {
        dataService = new DataService(new PersistenceService(appDataDir), CLOCK_BEFORE_CUTOFF);
        storageService = dataService.getStorageService();
        authenticatedDataStore = storageService.getOrCreateAuthenticatedDataStore(InventoryTestData.class.getSimpleName())
                .join();
    }

    // The stores write asynchronously. JUnit deletes appDataDir after the test, and fails if a write still creates
    // files in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        responder.ifPresent(InventoryHashSetFilterTest::shutdown);
        shutdown(dataService);
    }

    private static void shutdown(DataService dataService) {
        dataService.getStorageService().getStoresByStoreType(StoreType.ALL)
                .forEach(InventoryHashSetFilterTest::waitForPendingWrites);
        dataService.shutdown();
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void bothHashSetTypesSurviveProtobufRoundTrip() throws UnresolvableProtobufException {
        List<HashSetFilterEntry> entries = List.of(new HashSetFilterEntry(new byte[20], 3));
        for (InventoryFilterType type : List.of(InventoryFilterType.HASH_SET, InventoryFilterType.HASH_SET_V2)) {
            HashSetFilter filter = new HashSetFilter(type, new ArrayList<>(entries));

            InventoryFilter restored = InventoryFilter.fromProto(filter.toProto(false));

            assertEquals(type, restored.getInventoryFilterType());
            assertEquals(filter.getFilterEntries(), ((HashSetFilter) restored).getFilterEntries());
        }
    }

    // The type defines which storage key the entries contain, so it must never be guessed
    @Test
    void unknownOrMismatchedFilterTypeIsRejected() {
        bisq.network.protobuf.InventoryFilter proto = new HashSetFilter(InventoryFilterType.HASH_SET_V2, new ArrayList<>())
                .toProto(false);

        bisq.network.protobuf.InventoryFilter unspecified = proto.toBuilder()
                .setInventoryFilterType(bisq.network.protobuf.InventoryFilterType.INVENTORYFILTERTYPE_UNSPECIFIED)
                .build();
        assertThrows(UnresolvableProtobufMessageException.class, () -> InventoryFilter.fromProto(unspecified));

        bisq.network.protobuf.InventoryFilter unknown = proto.toBuilder().setInventoryFilterTypeValue(99).build();
        assertThrows(UnresolvableProtobufMessageException.class, () -> InventoryFilter.fromProto(unknown));

        bisq.network.protobuf.InventoryFilter miniSketchType = proto.toBuilder()
                .setInventoryFilterType(bisq.network.protobuf.InventoryFilterType.INVENTORYFILTERTYPE_MINI_SKETCH)
                .build();
        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> InventoryFilter.fromProto(miniSketchType)).getCause());
    }

    // Rule 7: an entry which the requester cannot parse does not remove the other entries of the response
    @Test
    void entriesWhichCannotBeParsedAreSkipped() throws Exception {
        RemoveDataRequest removal = createRemovalsOfBothStoreTypes().get(0);
        bisq.network.protobuf.DataRequest valid = removal.toProto(false).getDataRequest();
        bisq.network.protobuf.DataRequest withoutRequest = bisq.network.protobuf.DataRequest.getDefaultInstance();
        bisq.network.protobuf.DataRequest withInvalidKey = valid.toBuilder()
                .setRemoveAuthenticatedDataRequest(valid.getRemoveAuthenticatedDataRequest().toBuilder()
                        .setOwnerPublicKeyBytes(ByteString.copyFrom(new byte[]{1, 2, 3})))
                .build();
        assertThrows(UnresolvableProtobufException.class, () -> DataRequest.fromProto(withoutRequest));
        assertThrows(UnresolvableProtobufException.class, () -> DataRequest.fromProto(withInvalidKey));

        int maxSize = Inventory.maxSize;
        Inventory.setMaxSize(MAX_SIZE);
        try {
            Inventory inventory = Inventory.fromProto(bisq.network.protobuf.Inventory.newBuilder()
                    .addEntries(withoutRequest)
                    .addEntries(valid)
                    .addEntries(withInvalidKey)
                    .setMaxSizeReached(true)
                    .build());

            assertEquals(1, inventory.getEntries().size());
            assertArrayEquals(removal.serialize(), inventory.getEntries().get(0).serialize());
            assertTrue(inventory.isMaxSizeReached());
        } finally {
            Inventory.setMaxSize(maxSize);
        }
    }

    // With equal state, a responder sends nothing, for requesters of both versions
    @Test
    void responderWithEqualStateSendsNothingForBothTypes() throws Exception {
        addStoreContent();

        // A node of an earlier version sends storage key V1
        HashSetFilter legacyFilter = createFilterOfRequester(InventoryFilterType.HASH_SET, DataRequest::getStorageKeyV1);
        assertTrue(createInventory(InventoryFilterType.HASH_SET, legacyFilter).getEntries().isEmpty());

        HashSetFilter filterV2 = createFilterOfRequester(InventoryFilterType.HASH_SET_V2, DataRequest::getStorageKeyV2);
        assertTrue(createInventory(InventoryFilterType.HASH_SET_V2, filterV2).getEntries().isEmpty());

        // The services create the same filters from the store
        assertSameFilter(legacyFilter, service(InventoryFilterType.HASH_SET).getFilter());
        assertSameFilter(filterV2, service(InventoryFilterType.HASH_SET_V2).getFilter());
    }

    // The V2 filter uses the map keys. This requires that the stores hold every request under storage key V2.
    @Test
    void mapKeysAreStorageKeyV2() throws Exception {
        addStoreContent();

        storageService.getAllDataRequestMapEntries().forEach(entry ->
                assertEquals(entry.getValue().getStorageKeyV2(), entry.getKey()));
    }

    // Two owners with the same data have the same storage key V1, but different storage keys V2
    @Test
    void filterV2OfOneOwnerDoesNotHideTheEntryOfAnotherOwner() throws Exception {
        AddAuthenticatedDataRequest requestA = add(new InventoryTestData("shared", ownerA), ownerA);
        AddAuthenticatedDataRequest requestB = add(new InventoryTestData("shared", ownerB), ownerB);
        assertEquals(requestA.getStorageKeyV1(), requestB.getStorageKeyV1());
        assertEquals(requestA.getSequenceNumber(), requestB.getSequenceNumber());

        HashSetFilter filterWithA = new HashSetFilter(InventoryFilterType.HASH_SET_V2,
                new ArrayList<>(List.of(toEntry(requestA, requestA.getStorageKeyV2()))));
        assertEquals(List.of(requestB), createInventory(InventoryFilterType.HASH_SET_V2, filterWithA).getEntries());

        // A V1 filter cannot tell the owners apart (see the specification)
        HashSetFilter legacyFilterWithA = new HashSetFilter(InventoryFilterType.HASH_SET,
                new ArrayList<>(List.of(toEntry(requestA, requestA.getStorageKeyV1()))));
        assertTrue(createInventory(InventoryFilterType.HASH_SET, legacyFilterWithA).getEntries().isEmpty());
    }

    @Test
    void serviceRejectsFilterOfOtherType() {
        HashSetFilter legacyFilter = new HashSetFilter(InventoryFilterType.HASH_SET, new ArrayList<>());

        assertThrows(IllegalArgumentException.class,
                () -> service(InventoryFilterType.HASH_SET_V2).createInventory(legacyFilter));
    }

    // Updated nodes request only HASH_SET_V2 and respond to both types
    @Test
    void factoryRequestsOnlyPreferredTypeAndRespondsToBothTypes() throws Exception {
        addStoreContent();
        InventoryFilterFactory factory = new InventoryFilterFactory(
                Set.of(Feature.INVENTORY_HASH_SET, Feature.INVENTORY_HASH_SET_V2),
                dataService,
                new InventoryService.Config(2000, 600_000, 3, 8, 7, 4, 3, List.of(InventoryFilterType.HASH_SET_V2)));

        List<Feature> featuresOfEarlierVersion = List.of(Feature.INVENTORY_HASH_SET, Feature.AUTHORIZATION_HASH_CASH);
        assertTrue(factory.getPreferredFilterType(featuresOfEarlierVersion).isEmpty());
        assertThrows(IllegalStateException.class, () -> factory.createInventoryFilterForRequest(featuresOfEarlierVersion));

        List<Feature> featuresOfThisVersion = List.of(Feature.INVENTORY_HASH_SET, Feature.INVENTORY_HASH_SET_V2);
        InventoryFilter filter = factory.createInventoryFilterForRequest(featuresOfThisVersion);
        assertEquals(InventoryFilterType.HASH_SET_V2, filter.getInventoryFilterType());
        assertTrue(factory.createInventoryForResponse(new InventoryRequest(filter, 1)).getEntries().isEmpty());

        HashSetFilter legacyFilter = createFilterOfRequester(InventoryFilterType.HASH_SET, DataRequest::getStorageKeyV1);
        assertTrue(factory.createInventoryForResponse(new InventoryRequest(legacyFilter, 1)).getEntries().isEmpty());
        HashSetFilter emptyLegacyFilter = new HashSetFilter(InventoryFilterType.HASH_SET, new ArrayList<>());
        assertFalse(factory.createInventoryForResponse(new InventoryRequest(emptyLegacyFilter, 1)).getEntries().isEmpty());
    }

    // With the preference list of the app configurations, a node uses HASH_SET_V2 if the peer supports it, and falls back
    // to HASH_SET for peers of earlier versions. The order of the list decides, not the order of the peer's features.
    @Test
    void factoryPrefersV2AndFallsBackToV1ForPeersOfEarlierVersions() throws Exception {
        addStoreContent();
        InventoryFilterFactory factory = new InventoryFilterFactory(
                Set.of(Feature.INVENTORY_HASH_SET, Feature.INVENTORY_HASH_SET_V2),
                dataService,
                new InventoryService.Config(2000, 600_000, 3, 8, 7, 4, 3,
                        List.of(InventoryFilterType.HASH_SET_V2, InventoryFilterType.HASH_SET)));

        List<Feature> featuresOfThisVersion = List.of(Feature.INVENTORY_HASH_SET, Feature.INVENTORY_HASH_SET_V2);
        assertEquals(Optional.of(InventoryFilterType.HASH_SET_V2), factory.getPreferredFilterType(featuresOfThisVersion));
        assertEquals(Optional.of(InventoryFilterType.HASH_SET_V2),
                factory.getPreferredFilterType(List.of(Feature.INVENTORY_HASH_SET_V2, Feature.INVENTORY_HASH_SET)));
        assertEquals(Optional.of(InventoryFilterType.HASH_SET_V2),
                factory.getPreferredFilterType(List.of(Feature.INVENTORY_HASH_SET_V2)));

        List<Feature> featuresOfEarlierVersion = List.of(Feature.INVENTORY_HASH_SET, Feature.AUTHORIZATION_HASH_CASH);
        assertEquals(Optional.of(InventoryFilterType.HASH_SET), factory.getPreferredFilterType(featuresOfEarlierVersion));
        assertTrue(factory.getPreferredFilterType(List.of(Feature.AUTHORIZATION_HASH_CASH)).isEmpty());

        // The V1 filter contains storage key V1, which a responder of an earlier version uses as map key
        InventoryFilter legacyFilter = factory.createInventoryFilterForRequest(featuresOfEarlierVersion);
        assertEquals(InventoryFilterType.HASH_SET, legacyFilter.getInventoryFilterType());
        assertSameFilter(createFilterOfRequester(InventoryFilterType.HASH_SET, DataRequest::getStorageKeyV1),
                (HashSetFilter) legacyFilter);

        InventoryFilter filterV2 = factory.createInventoryFilterForRequest(featuresOfThisVersion);
        assertEquals(InventoryFilterType.HASH_SET_V2, filterV2.getInventoryFilterType());
        assertSameFilter(createFilterOfRequester(InventoryFilterType.HASH_SET_V2, DataRequest::getStorageKeyV2),
                (HashSetFilter) filterV2);
    }

    // The fallback needs the service of the type: a node registers services only for the features it advertises
    @Test
    void factoryDoesNotFallBackToTypeWithoutFeature() {
        InventoryFilterFactory factory = new InventoryFilterFactory(
                Set.of(Feature.INVENTORY_HASH_SET_V2),
                dataService,
                new InventoryService.Config(2000, 600_000, 3, 8, 7, 4, 3,
                        List.of(InventoryFilterType.HASH_SET_V2, InventoryFilterType.HASH_SET)));

        assertTrue(factory.getPreferredFilterType(List.of(Feature.INVENTORY_HASH_SET)).isEmpty());
    }

    // A relay of an earlier version removes signature V2 from a removal and keeps its sequence number. The removal with
    // signature V2 replaces such a copy (see storage-request-ordering.md), so the responder must send it, for both store
    // types.
    @Test
    void responderSendsRemovalWithSignatureV2ToRequesterWithCopyWithoutIt() throws Exception {
        StorageService responderStorageService = createResponder();
        List<RemoveDataRequest> removals = createRemovalsOfBothStoreTypes();
        for (RemoveDataRequest removal : removals) {
            assertTrue(storageService.onRemoveDataRequest(withoutSignatureV2(removal)).join().isSuccess());
            assertTrue(responderStorageService.onRemoveDataRequest(removal).join().isSuccess());
        }

        Inventory inventory = createInventoryV2(responderStorageService);

        assertEquals(Set.copyOf(removals), Set.copyOf(inventory.getEntries()));
        inventory.getEntries().forEach(removal ->
                assertTrue(storageService.onRemoveDataRequest((RemoveDataRequest) removal).join().isSuccess()));
        assertTrue(createInventoryV2(responderStorageService).getEntries().isEmpty());
    }

    // A copy without signature V2 never replaces a stored removal. So copies have equal entries, also if a relay changed
    // the sequence number, which signature V1 does not cover.
    @Test
    void responderDoesNotSendCopyWithoutSignatureV2ToRequesterWithCopy() throws Exception {
        StorageService responderStorageService = createResponder();
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                new DefaultAuthenticatedData(new InventoryTestData("removed", ownerA)),
                ownerA);
        RemoveAuthenticatedDataRequest copy = (RemoveAuthenticatedDataRequest) withoutSignatureV2(removal);
        RemoveAuthenticatedDataRequest changedCopy = RemoveAuthenticatedDataRequest.fromProto(copy.toValueProto(false)
                .toBuilder()
                .setSequenceNumber(Integer.MAX_VALUE)
                .build());
        assertTrue(storageService.onRemoveDataRequest(copy).join().isSuccess());
        assertTrue(responderStorageService.onRemoveDataRequest(changedCopy).join().isSuccess());

        assertTrue(createInventoryV2(responderStorageService).getEntries().isEmpty());
    }

    // Nodes of earlier versions list removals with their sequence number, so HASH_SET keeps it
    @Test
    void onlyFilterV2ListsRemovalWithoutSignatureV2WithoutItsSequenceNumber() throws Exception {
        RemoveAuthenticatedDataRequest removal = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                new DefaultAuthenticatedData(new InventoryTestData("removed", ownerA)),
                ownerA);
        RemoveAuthenticatedDataRequest copy = RemoveAuthenticatedDataRequest.fromProto(
                ((RemoveAuthenticatedDataRequest) withoutSignatureV2(removal)).toValueProto(false).toBuilder()
                        .setSequenceNumber(5)
                        .build());
        assertTrue(storageService.onRemoveDataRequest(copy).join().isSuccess());

        assertEquals(List.of(new HashSetFilterEntry(copy.getStorageKeyV1().getBytes(), 5)),
                service(InventoryFilterType.HASH_SET).getFilter().getFilterEntries());
        assertEquals(List.of(new HashSetFilterEntry(copy.getStorageKeyV2().getBytes(), 0)),
                service(InventoryFilterType.HASH_SET_V2).getFilter().getFilterEntries());
    }

    // An authenticated data entry of owner A, a remove request of owner B without entry, and append-only data
    private void addStoreContent() throws Exception {
        add(new InventoryTestData("a", ownerA), ownerA);
        RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(authenticatedDataStore,
                new DefaultAuthenticatedData(new InventoryTestData("removed", ownerB)),
                ownerB);
        assertTrue(storageService.onRemoveDataRequest(removeRequest).join().isNoEntry());
        assertTrue(storageService.onAddDataRequest(new AddAppendOnlyDataRequest(new InventoryTestAppendOnlyData("x")))
                .join().isSuccess());
        assertEquals(3, storageService.getAllDataRequestMapEntries().count());
    }

    private AddAuthenticatedDataRequest add(InventoryTestData data, KeyPair keyPair) throws Exception {
        AddAuthenticatedDataRequest request = AddAuthenticatedDataRequest.from(authenticatedDataStore,
                new DefaultAuthenticatedData(data),
                keyPair);
        assertTrue(storageService.onAddDataRequest(request).join().isSuccess());
        return request;
    }

    // The filter which a requester with the same store content sends
    private HashSetFilter createFilterOfRequester(InventoryFilterType type, Function<DataRequest, ByteArray> keyFunction) {
        List<HashSetFilterEntry> entries = storageService.getAllDataRequestMapEntries()
                .map(Map.Entry::getValue)
                .map(request -> toEntry(request, keyFunction.apply(request)))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        return new HashSetFilter(type, entries);
    }

    // InventoryFilter has no equals
    private static void assertSameFilter(HashSetFilter expected, HashSetFilter actual) {
        assertEquals(expected.getInventoryFilterType(), actual.getInventoryFilterType());
        assertEquals(expected.getFilterEntries(), actual.getFilterEntries());
    }

    private static HashSetFilterEntry toEntry(DataRequest request, ByteArray key) {
        int sequenceNumber = request instanceof Sequential sequential ? sequential.getSequenceNumber() : 0;
        return new HashSetFilterEntry(key.getBytes(), sequenceNumber);
    }

    private Inventory createInventory(InventoryFilterType type, HashSetFilter filter) {
        return service(type).createInventory(filter);
    }

    private HashSetFilterService service(InventoryFilterType type) {
        return new HashSetFilterService(storageService, MAX_SIZE, type);
    }

    private StorageService createResponder() {
        DataService responderDataService = new DataService(new PersistenceService(appDataDir.resolve("responder")),
                CLOCK_BEFORE_CUTOFF);
        responder = Optional.of(responderDataService);
        return responderDataService.getStorageService();
    }

    // The inventory which the responder sends for the HASH_SET_V2 filter of dataService
    private Inventory createInventoryV2(StorageService responderStorageService) {
        return new HashSetFilterService(responderStorageService, MAX_SIZE, InventoryFilterType.HASH_SET_V2)
                .createInventory(service(InventoryFilterType.HASH_SET_V2).getFilter());
    }

    // A removal of authenticated data by owner A, and a removal of mailbox data by its receiver, owner B
    private List<RemoveDataRequest> createRemovalsOfBothStoreTypes() throws Exception {
        RemoveAuthenticatedDataRequest authenticatedDataRemoval = RemoveAuthenticatedDataRequest.from(
                authenticatedDataStore,
                new DefaultAuthenticatedData(new InventoryTestData("removed", ownerA)),
                ownerA);
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(ownerA));
        return List.of(authenticatedDataRemoval, RemoveMailboxRequest.from(mailboxData, ownerB));
    }

    // As a relay of an earlier version sends it
    private static RemoveDataRequest withoutSignatureV2(RemoveDataRequest removal)
            throws UnresolvableProtobufException {
        if (removal instanceof RemoveAuthenticatedDataRequest request) {
            return RemoveAuthenticatedDataRequest.fromProto(request.toValueProto(false).toBuilder()
                    .setVersion(1)
                    .clearSignatureV2()
                    .build());
        }
        RemoveMailboxRequest request = (RemoveMailboxRequest) removal;
        return RemoveMailboxRequest.fromProto(request.toValueProto(false).toBuilder()
                .setVersion(1)
                .clearSignatureV2()
                .build());
    }

    // The owner is not serialized, so data of different owners can have the same storage key V1
    private static final class InventoryTestData implements OwnershipVerifiableData {
        private static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
                InventoryTestData.class.getSimpleName());

        private final String content;
        private final byte[] ownerPubKeyHash;

        private InventoryTestData(String content, KeyPair owner) {
            this.content = content;
            this.ownerPubKeyHash = DigestUtil.hash(owner.getPublic().getEncoded());
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(content);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public boolean isDataInvalid() {
            return false;
        }

        @Override
        public byte[] getOwnerPubKeyHash() {
            return ownerPubKeyHash;
        }
    }

    private static final class InventoryTestAppendOnlyData implements AppendOnlyData {
        private static final MetaData META_DATA = new MetaData(MetaData.TTL_10_DAYS,
                InventoryTestAppendOnlyData.class.getSimpleName());
        // network.proto: message MetaData { ...; string className = 4; }
        private static final CanonicalSchema<InventoryTestAppendOnlyData> DEFAULT_SCHEMA =
                CanonicalSchema.<InventoryTestAppendOnlyData>newBuilder()
                        .string(4, data -> data.content)
                        .build();

        private final String content;

        private InventoryTestAppendOnlyData(String content) {
            this.content = content;
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.MetaData.Builder getBuilder(boolean serializeForHash) {
            return bisq.network.protobuf.MetaData.newBuilder().setClassName(content);
        }

        @Override
        public bisq.network.protobuf.MetaData toProto(boolean serializeForHash) {
            return unsafeToProto(serializeForHash);
        }

        @Override
        public MetaData getMetaData() {
            return META_DATA;
        }

        @Override
        public boolean isDataInvalid() {
            return false;
        }

        @Override
        public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
            CanonicalSchema<InventoryTestAppendOnlyData> schema = switch (encodingPurpose) {
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
