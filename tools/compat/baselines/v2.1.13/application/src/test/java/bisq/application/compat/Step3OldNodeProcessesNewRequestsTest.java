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

package bisq.application.compat;

import bisq.application.ResolverConfig;
import bisq.common.application.DevMode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static bisq.application.compat.OldCompatSupport.NEW_REQUESTS_FILE;
import static bisq.application.compat.OldCompatSupport.OLD_DATA_DIR;
import static bisq.application.compat.OldCompatSupport.OLD_REQUESTS_FILE;
import static bisq.application.compat.OldCompatSupport.RELAYED_COPIES_FILE;
import static bisq.application.compat.OldCompatSupport.applicationData;
import static bisq.application.compat.OldCompatSupport.base64;
import static bisq.application.compat.OldCompatSupport.exchangeDir;
import static bisq.application.compat.OldCompatSupport.flags;
import static bisq.application.compat.OldCompatSupport.parse;
import static bisq.application.compat.OldCompatSupport.proofOfWorkPayloadHash;
import static bisq.application.compat.OldCompatSupport.recordingListener;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 3 of the compatibility chain, with the code of v2.1.13. Nodes of v2.1.13 process the requests of an updated
 * oracle node and an updated user from step 2, and the requests of v2.1.13 which a node of the new version relayed. A
 * node of v2.1.13 then relays the requests of the new version. It drops the fields which it does not know, such as
 * signature V2. The step writes these copies for step 4.
 */
@EnabledIfSystemProperty(named = OldCompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step3OldNodeProcessesNewRequestsTest {
    private static final List<String> MIGRATION_ADDS = List.of(
            "migrationAdd.AuthorizedBondedRole", "migrationAdd.AuthorizedTimestampData");
    private static final List<String> MIGRATION_REMOVES = List.of(
            "migrationRemove.AuthorizedBondedRole", "migrationRemove.AuthorizedTimestampData");
    // The requests of the updated user in the order in which it created them. A node of v2.1.13 does not process a
    // refresh request which it receives (storage-refresh-requests.md, compatibility), so refreshUserProfile is missing.
    private static final List<String> USER_REQUESTS = List.of(
            "refreshedAddUserProfile", "addUserProfile",
            "removeChat3", "addChat4", "removeChat4",
            "removeMailbox1", "addMailbox4", "removeMailbox4");
    // The requests of v2.1.13 from step 1 in the order of their creation, without the refresh request and the refreshed
    // add request, which nodes of v2.1.13 do not relay to each other as received requests
    private static final List<String> OLD_REQUESTS = List.of(
            "addUserProfile",
            "addChat1", "addChat2", "addChat3", "removeChat2",
            "addRole1", "addRole2", "addRole3", "addTimestamp",
            "addMailbox1", "addMailbox2", "addMailbox3", "removeMailbox2");

    private static Properties old;
    private static Properties updated;

    @BeforeAll
    static void readTheRequests() throws Exception {
        DevMode.setDevMode(true);
        ResolverConfig.config();
        old = OldCompatSupport.read(OLD_REQUESTS_FILE);
        updated = OldCompatSupport.read(NEW_REQUESTS_FILE);
    }

    @Test
    void proofOfWorkPayloadAndCostOfTheNewRequestsAreUnchangedForTheOldVersion() throws Exception {
        List<String> names = OldCompatSupport.namesWithPrefix(updated, "request.");
        assertEquals(13, names.size(), names.toString());
        for (String name : names) {
            EnvelopePayloadMessage message = newRequest(name);
            assertEquals(updated.getProperty("pow." + name), proofOfWorkPayloadHash(message),
                    name + ": the proof of work payload differs, so the proof of work of the new version would fail");
            assertEquals(Double.parseDouble(updated.getProperty("costToPay." + name)), message.getCostFactor(), 1e-9,
                    name + ": v2.1.13 expects another cost factor than the new version pays");
        }
    }

    @Test
    void oldNodeWithTheDataOfTheOldOracleKeepsTheMigratedDataAndRejectsTheLaterRemoval() throws Exception {
        List<String> events = new ArrayList<>();
        DataService node = loadedNode("step3-loaded-node-add-first", events);
        StorageService storageService = node.getStorageService();

        // The migration creates the removal with sequence number n + 1 and the add request with n + 2. A node of v2.1.13
        // stores both under one storage key. When the add request arrives first, the node rejects the removal. This is
        // the cause of inventory-migration-removals-loop in the release test plan of v2.1.14.
        for (int i = 0; i < MIGRATION_ADDS.size(); i++) {
            assertEquals(Set.of("success"), process(storageService, newRequest(MIGRATION_ADDS.get(i))), MIGRATION_ADDS.get(i));
            assertEquals(Set.of("sequenceNrInvalid"), process(storageService, newRequest(MIGRATION_REMOVES.get(i))), MIGRATION_REMOVES.get(i));
        }
        assertEquals(List.of("authorizedAdded:AuthorizedBondedRole", "authorizedAdded:AuthorizedTimestampData"),
                events.stream().filter(event -> event.startsWith("authorized")).sorted().toList());

        for (String name : USER_REQUESTS) {
            assertTrue(process(storageService, newRequest(name)).contains("success"), name);
        }
        assertEquals(List.of("AuthorizedBondedRole", "AuthorizedTimestampData", "CommonPublicChatMessage", "UserProfile"),
                applicationData(node));
        node.shutdown();
    }

    @Test
    void oldNodeWithTheDataOfTheOldOracleAcceptsTheMigrationWhenTheRemovalArrivesFirst() throws Exception {
        List<String> events = new ArrayList<>();
        DataService node = loadedNode("step3-loaded-node-remove-first", events);
        StorageService storageService = node.getStorageService();

        for (int i = 0; i < MIGRATION_ADDS.size(); i++) {
            assertTrue(process(storageService, newRequest(MIGRATION_REMOVES.get(i))).contains("success"), MIGRATION_REMOVES.get(i));
            assertTrue(process(storageService, newRequest(MIGRATION_ADDS.get(i))).contains("success"), MIGRATION_ADDS.get(i));
        }
        List<String> data = applicationData(node);
        assertTrue(data.contains("AuthorizedBondedRole") && data.contains("AuthorizedTimestampData"), data.toString());
        // The same removal again, for example from an inventory response
        assertFalse(process(storageService, newRequest(MIGRATION_REMOVES.get(0))).contains("success"));
        node.shutdown();
    }

    @Test
    void freshOldNodeAcceptsTheRequestsOfTheNewVersion() throws Exception {
        DataService node = freshNode("step3-fresh-node", new ArrayList<>());
        StorageService storageService = node.getStorageService();

        for (String name : MIGRATION_ADDS) {
            assertTrue(process(storageService, newRequest(name)).contains("success"), name);
        }
        for (String name : List.of("addUserProfile", "addChat4", "removeChat4", "addMailbox4", "removeMailbox4")) {
            assertTrue(process(storageService, newRequest(name)).contains("success"), name);
        }
        assertEquals(List.of("AuthorizedBondedRole", "AuthorizedTimestampData", "UserProfile"), applicationData(node));
        node.shutdown();
    }

    @Test
    void freshOldNodeAcceptsItsRequestsAfterANewNodeRelayedThem() throws Exception {
        DataService node = freshNode("step3-fresh-node-relayed-by-new", new ArrayList<>());
        StorageService storageService = node.getStorageService();

        for (String name : OLD_REQUESTS) {
            EnvelopePayloadMessage relayedCopy = parse(updated.getProperty("relayedByNewNode." + name));
            assertEquals(old.getProperty("pow." + name), proofOfWorkPayloadHash(relayedCopy), name);
            assertTrue(process(storageService, relayedCopy).contains("success"), name);
        }
        assertEquals(List.of("AuthorizedBondedRole", "AuthorizedTimestampData", "CommonPublicChatMessage",
                        "CommonPublicChatMessage", "UserProfile"),
                applicationData(node));
        node.shutdown();
    }

    @Test
    void oldNodeRelaysTheRequestsOfTheNewVersion() throws Exception {
        Map<String, String> out = new TreeMap<>();
        for (String name : OldCompatSupport.namesWithPrefix(updated, "request.")) {
            EnvelopePayloadMessage message = newRequest(name);
            // A node of v2.1.13 serializes a request again when it relays, persists or serves it
            byte[] relayed = message.serialize();
            out.put("relayed." + name, base64(relayed));
            out.put("pow." + name, proofOfWorkPayloadHash(message));
            out.put("cost." + name, String.valueOf(message.getCostFactor()));
        }
        OldCompatSupport.write(RELAYED_COPIES_FILE, out,
                "Requests of the new version as a node of v2.1.13 relays them (step 3 of the compatibility chain)");
    }

    private static Set<String> process(StorageService storageService, EnvelopePayloadMessage message) {
        return flags(OldCompatSupport.process(storageService, message));
    }

    private static EnvelopePayloadMessage newRequest(String name) throws Exception {
        return parse(updated.getProperty("request." + name));
    }

    private static DataService loadedNode(String workDirName, List<String> events) throws Exception {
        Path dir = OldCompatSupport.newWorkDir(workDirName);
        OldCompatSupport.copyRecursively(exchangeDir().resolve(OLD_DATA_DIR), dir);
        PersistenceService persistenceService = new PersistenceService(dir);
        DataService node = new DataService(persistenceService);
        assertTrue(persistenceService.readAllPersisted().join());
        node.addListener(recordingListener(events));
        return node;
    }

    private static DataService freshNode(String workDirName, List<String> events) throws Exception {
        DataService node = new DataService(new PersistenceService(OldCompatSupport.newWorkDir(workDirName)));
        node.addListener(recordingListener(events));
        return node;
    }
}
