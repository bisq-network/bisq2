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

package bisq.application.compat.v2_1_13;

import bisq.application.ResolverConfig;
import bisq.application.compat.CompatSupport;
import bisq.common.application.DevMode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.StoreType;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static bisq.application.compat.CompatSupport.NEW_REQUESTS_FILE;
import static bisq.application.compat.CompatSupport.RELAYED_COPIES_FILE;
import static bisq.application.compat.CompatSupport.applicationData;
import static bisq.application.compat.CompatSupport.describeStores;
import static bisq.application.compat.CompatSupport.flags;
import static bisq.application.compat.CompatSupport.parse;
import static bisq.application.compat.CompatSupport.proofOfWorkPayloadHash;
import static bisq.network.p2p.services.data.storage.SignedRequest.SIGNATURE_V1_CUTOFF_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 4 of the compatibility chain, with the code of this version. A node of v2.1.13 relayed the requests of the new
 * version in step 3 and dropped signature V2 and the other fields which it does not know. This step checks how nodes
 * of this version process these copies before and after the cutoff date of signature V1
 * (docs/specifications/network/storage-request-signatures.md).
 * <p>
 * The step does not check the order of a copy and the complete request before the cutoff date. That behavior is an
 * open point of storage-request-signatures.md.
 */
@EnabledIfSystemProperty(named = CompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step4NewNodeProcessesRelayedCopiesTest {
    private static final long BEFORE_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime() - 1;
    private static final long AT_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime();

    // Copies which a node of v2.1.13 relays, in a natural order of arrival. Nodes of v2.1.13 do not relay refresh
    // requests (storage-refresh-requests.md, compatibility).
    private static final List<String> RELAYED_REQUESTS = List.of(
            "migrationAdd.AuthorizedBondedRole", "migrationAdd.AuthorizedTimestampData",
            "addUserProfile", "addChat4", "removeChat4", "addMailbox4", "removeMailbox4");

    private static Properties updated;
    private static Properties relayed;

    @BeforeAll
    static void readTheRelayedCopies() throws Exception {
        DevMode.setDevMode(true);
        ResolverConfig.config();
        updated = CompatSupport.read(NEW_REQUESTS_FILE);
        relayed = CompatSupport.read(RELAYED_COPIES_FILE);
    }

    @Test
    void relayedCopiesHaveTheProofOfWorkPayloadOfTheCompleteRequests() throws Exception {
        for (String name : CompatSupport.namesWithPrefix(updated, "request.")) {
            EnvelopePayloadMessage copy = relayedCopy(name);
            assertEquals(updated.getProperty("pow." + name), proofOfWorkPayloadHash(copy),
                    name + ": a node of this version would reject the proof of work of a relay of v2.1.13");
            assertFalse(((SignedRequest) copy).isSignatureV2Present(), name + ": v2.1.13 cannot keep signature V2");
        }
    }

    @Test
    void freshNodeAcceptsRelayedCopiesBeforeTheCutoff() throws Exception {
        DataService node = newNode("step4-fresh-node-before-cutoff", new AtomicLong(BEFORE_CUTOFF));
        for (String name : RELAYED_REQUESTS) {
            Set<String> result = flags(CompatSupport.process(node.getStorageService(), relayedCopy(name)));
            assertTrue(result.contains("success"), name + ": " + result);
        }
        // The authorized key published the migrated data, so the applications get it (storage-ownership.md)
        assertEquals(List.of("AuthorizedBondedRole", "AuthorizedTimestampData", "UserProfile"), applicationData(node));
        node.shutdown();
    }

    @Test
    void freshNodeRejectsRelayedCopiesFromTheCutoffOn() throws Exception {
        DataService node = newNode("step4-fresh-node-at-cutoff", new AtomicLong(AT_CUTOFF));
        for (String name : RELAYED_REQUESTS) {
            Set<String> result = flags(CompatSupport.process(node.getStorageService(), relayedCopy(name)));
            assertTrue(result.contains("signatureV2Missing"), name + ": " + result);
            assertFalse(result.contains("success"), name + ": " + result);
        }
        assertEquals(List.of(), describeStores(node.getStorageService()));
        node.shutdown();
    }

    @Test
    void dataOfARelayedCopyReturnsWithTheCompleteRequestAfterTheCutoff() throws Exception {
        Path dir = CompatSupport.newWorkDir("step4-node-across-cutoff");
        AtomicLong now = new AtomicLong(BEFORE_CUTOFF);
        DataService node = new DataService(new PersistenceService(dir), now::get);
        String name = "migrationAdd.AuthorizedBondedRole";
        assertTrue(flags(CompatSupport.process(node.getStorageService(), relayedCopy(name))).contains("success"));
        assertEquals(List.of("AuthorizedBondedRole"), applicationData(node));
        persistAll(node.getStorageService());
        node.shutdown();

        // The node starts again after the cutoff date. The store removes the copy without signature V2 at load (rule 9
        // of storage-request-signatures.md). Then the complete request arrives, for example with an inventory
        // response, and the data is back.
        now.set(AT_CUTOFF);
        PersistenceService persistenceService = new PersistenceService(dir);
        node = new DataService(persistenceService, now::get);
        assertTrue(persistenceService.readAllPersisted().join());
        assertEquals(List.of(), describeStores(node.getStorageService()));
        EnvelopePayloadMessage completeRequest = parse(updated.getProperty("request." + name));
        Set<String> result = flags(CompatSupport.process(node.getStorageService(), completeRequest));
        assertTrue(result.contains("success"), result.toString());
        assertEquals(List.of("AuthorizedBondedRole"), applicationData(node));
        node.shutdown();
    }

    private static EnvelopePayloadMessage relayedCopy(String name) throws Exception {
        return parse(relayed.getProperty("relayed." + name));
    }

    private static DataService newNode(String workDirName, AtomicLong now) throws Exception {
        return new DataService(new PersistenceService(CompatSupport.newWorkDir(workDirName)), now::get);
    }

    private static void persistAll(StorageService storageService) {
        storageService.getStoresByStoreType(StoreType.ALL).forEach(store -> store.persist().join());
    }
}
