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

import bisq.network.p2p.services.data.storage.MetaData;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageService;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The report of an inventory response is written to the log. Remove requests carry the class name of the removed data
 * as a field which the peer sets. Parsing checks the format of the class name (MetaData.validateClassName), but a peer
 * can still send any class name of that format, also one which no node registered (see
 * docs/specifications/network/storage-metadata.md).
 */
class InventoryPrinterTest {
    @TempDir
    Path tempDir;

    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private DataService dataService;
    private RemoveAuthenticatedDataRequest removal;

    @BeforeAll
    static void registerTypes() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
    }

    @BeforeEach
    void setUp() throws GeneralSecurityException {
        dataService = new DataService(new PersistenceService(tempDir));
        AuthenticatedDataStorageService store = dataService.getStorageService()
                .getOrCreateAuthenticatedDataStore(TestDistributedData.class.getSimpleName())
                .join();
        removal = RemoveAuthenticatedDataRequest.from(store,
                new DefaultAuthenticatedData(new TestDistributedData("content", ownerKeyPair.getPublic())),
                ownerKeyPair);
    }

    // The stores write asynchronously. JUnit deletes tempDir after the test, and fails if a write still creates files in
    // it. A write queued now completes after all earlier ones.
    @AfterEach
    void tearDown() {
        dataService.getStorageService().getStoresByStoreType(StoreType.ALL)
                .forEach(InventoryPrinterTest::waitForPendingWrites);
        dataService.shutdown();
    }

    @Test
    void registeredClassNameIsWrittenToTheReport() {
        String report = InventoryPrinter.createReport(new Inventory(List.of(removal), false));

        assertEquals("   1 item(s) of RemoveAuthenticatedDataRequest.TestDistributedData", report);
    }

    // A peer can set any class name of a valid format. Parsing rejects other formats (see
    // AuthenticatedDataSignatureV2Test). The report has one line per name, so it writes only registered names.
    @Test
    void classNameWhichIsNotRegisteredIsNotWrittenToTheReport() throws UnresolvableProtobufException {
        String className = "UnregisteredTypeOfPeer";
        // A node of a later version fills className and the className of metaData with the same name
        //noinspection deprecation
        RemoveAuthenticatedDataRequest withOtherClassName = RemoveAuthenticatedDataRequest.fromProto(
                removal.toValueProto(false).toBuilder()
                        .setClassName(className)
                        .setMetaData(new MetaData(MetaData.TTL_10_DAYS, className).toProto(false))
                        .build());
        assertEquals(className, withOtherClassName.getClassName());

        String report = InventoryPrinter.createReport(new Inventory(List.of(removal, withOtherClassName), false));

        assertFalse(report.contains(className));
        assertEquals(2, report.lines().count());
        assertTrue(report.contains("RemoveAuthenticatedDataRequest.TestDistributedData"));
        assertTrue(report.contains("RemoveAuthenticatedDataRequest." + InventoryPrinter.UNREGISTERED_TYPE));
    }

    private static <T extends DataRequest> void waitForPendingWrites(DataStorageService<T> store) {
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }
}
