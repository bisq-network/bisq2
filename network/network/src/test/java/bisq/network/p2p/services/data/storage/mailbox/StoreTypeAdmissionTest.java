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

package bisq.network.p2p.services.data.storage.mailbox;

import bisq.common.util.StringUtils;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.concurrent.CompletionException;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.BEFORE_CUTOFF;
import static bisq.network.p2p.services.data.storage.StoreType.ALL;
import static bisq.network.p2p.services.data.storage.StoreType.APPEND_ONLY_DATA_STORE;
import static bisq.network.p2p.services.data.storage.StoreType.AUTHENTICATED_DATA_STORE;
import static bisq.network.p2p.services.data.storage.StoreType.MAILBOX_DATA_STORE;
import static bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mailbox data and remove requests carry only a class name. Signature V2 covers it, but signature V1 does not, and
 * nodes accept requests with only signature V1 from nodes of earlier versions. A relay can therefore remove signature
 * V2 and change the class name. A store must accept only class names of its own store type. Otherwise a mailbox
 * request could name a DistributedData type and get its MetaData, for example a longer time to live.
 */
class StoreTypeAdmissionTest {
    private static final String MAILBOX_TYPE = AckMessage.class.getSimpleName();
    private static final String DISTRIBUTED_DATA_TYPE = TestDistributedData.class.getSimpleName();

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();

    @TempDir
    private Path appDataDir;
    private StorageService storageService;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
    }

    @BeforeEach
    void setUp() {
        storageService = new StorageService(new PersistenceService(appDataDir));
    }

    // The stores write asynchronously. JUnit deletes appDataDir after the test, and fails if a write still creates
    // files in it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        storageService.getStoresByStoreType(ALL).forEach(StoreTypeAdmissionTest::waitForPendingWrites);
        storageService.shutdown();
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void registryDerivesStoreTypeFromJavaType() {
        assertTrue(StorableTypesRegistry.isRegistered(MAILBOX_TYPE, MAILBOX_DATA_STORE));
        assertFalse(StorableTypesRegistry.isRegistered(MAILBOX_TYPE, AUTHENTICATED_DATA_STORE));
        assertTrue(StorableTypesRegistry.isRegistered(DISTRIBUTED_DATA_TYPE, AUTHENTICATED_DATA_STORE));
        assertFalse(StorableTypesRegistry.isRegistered(DISTRIBUTED_DATA_TYPE, MAILBOX_DATA_STORE));
        assertFalse(StorableTypesRegistry.getClassNames(MAILBOX_DATA_STORE).contains(DISTRIBUTED_DATA_TYPE));
    }

    @Test
    void registerRejectsTypeWhichIsNeitherMailboxMessageNorDistributedData() {
        MetaData metaData = new MetaData(MetaData.TTL_10_DAYS, String.class.getSimpleName());

        assertThrows(IllegalArgumentException.class, () -> StorableTypesRegistry.register(String.class, metaData));
    }

    @Test
    void storesRejectClassNamesOfOtherStoreTypes() {
        assertThrows(CompletionException.class,
                () -> storageService.getOrCreateMailboxDataStore(DISTRIBUTED_DATA_TYPE).join());
        assertThrows(CompletionException.class,
                () -> storageService.getOrCreateAuthenticatedDataStore(MAILBOX_TYPE).join());
        assertThrows(CompletionException.class,
                () -> storageService.getOrCreateAppendOnlyDataStore(MAILBOX_TYPE).join());
        assertThrows(CompletionException.class,
                () -> storageService.getOrCreateAppendOnlyDataStore(DISTRIBUTED_DATA_TYPE).join());
    }

    @Test
    void addMailboxRequestNamingDistributedDataTypeIsRejected() throws Exception {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        bisq.network.protobuf.AddMailboxRequest.Builder builder = AddMailboxRequest.from(mailboxData,
                senderKeyPair,
                receiverKeyPair.getPublic()).toValueProto(false).toBuilder();
        AddMailboxRequest request = AddMailboxRequest.fromProto(builder.build());
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setClassName(DISTRIBUTED_DATA_TYPE);
        assertTrue(AddMailboxRequest.fromProto(builder.build()).isSignatureInvalid());
        // Before the cutoff date of signature V1, a relay can remove signature V2
        AddMailboxRequest redirected = AddMailboxRequest.fromProto(builder.clearSignatureV2().clearVersion().build());
        assertFalse(redirected.isSignatureInvalid(BEFORE_CUTOFF));

        assertThrows(CompletionException.class, () -> storageService.onAddDataRequest(redirected).join());
        assertTrue(storageService.onAddDataRequest(request).join().isSuccess());
    }

    @Test
    void removeMailboxRequestNamingDistributedDataTypeIsRejected() throws Exception {
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
        bisq.network.protobuf.RemoveMailboxRequest.Builder builder = RemoveMailboxRequest.from(mailboxData,
                receiverKeyPair).toValueProto(false).toBuilder().setClassName(DISTRIBUTED_DATA_TYPE);
        assertTrue(RemoveMailboxRequest.fromProto(builder.build()).isSignatureInvalid());
        // Before the cutoff date of signature V1, a relay can remove signature V2
        RemoveMailboxRequest redirected = RemoveMailboxRequest.fromProto(builder.clearSignatureV2().build());
        assertFalse(redirected.isSignatureInvalid(BEFORE_CUTOFF));

        assertThrows(CompletionException.class, () -> storageService.onRemoveDataRequest(redirected).join());
    }

    @Test
    void persistedMailboxStoreOfDistributedDataTypeIsNotRestored() throws Exception {
        storageService.shutdown();
        Path mailboxDir = appDataDir.resolve(DbSubDirectory.NETWORK_DB.getDbPath())
                .resolve(MAILBOX_DATA_STORE.getStoreName());
        Files.createDirectories(mailboxDir);
        String fileName = StringUtils.camelCaseToSnakeCase(
                DISTRIBUTED_DATA_TYPE + DataStorageService.STORE_POST_FIX) + Persistence.EXTENSION;
        Files.createFile(mailboxDir.resolve(fileName));

        storageService = new StorageService(new PersistenceService(appDataDir));

        assertFalse(storageService.getStoresByStoreType(MAILBOX_DATA_STORE)
                .anyMatch(store -> DISTRIBUTED_DATA_TYPE.equals(store.getStoreKey())));
    }
}
