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
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 3 of the compatibility chain, with the code of v2.1.15. Nodes of v2.1.15 process the requests of an updated
 * oracle node and an updated user from step 2, and the requests of v2.1.15 which a node of the new version relayed. A
 * node of v2.1.15 then relays the requests of the new version. The step writes these copies for step 4.
 */
@EnabledIfSystemProperty(named = OldCompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step3OldNodeProcessesNewRequestsTest {
    // The requests of the updated oracle and the updated user in the order in which they created them, without the
    // refreshed add request. A node which applied the refresh request holds that add request already.
    private static final List<String> UPDATED_REQUESTS = List.of(
            "addRole4", "refreshUserProfile", "addUserProfile",
            "removeChat3", "addChat4", "removeChat4",
            "removeMailbox1", "addMailbox4", "removeMailbox4");
    // A refresh request needs the add request of the data in the store. The refreshed add request of step 1 is the
    // content of a store, not a request which the owner sent.
    private static final Set<String> OLD_REFRESH_REQUESTS = Set.of("refreshUserProfile", "refreshedAddUserProfile");

    // The requests of step 1 in the order of their creation, without the refresh requests
    private static List<String> oldRequests;
    private static Properties old;
    private static Properties updated;

    @BeforeAll
    static void readTheRequests() throws Exception {
        DevMode.setDevMode(true);
        ResolverConfig.config();
        old = OldCompatSupport.read(OLD_REQUESTS_FILE);
        updated = OldCompatSupport.read(NEW_REQUESTS_FILE);
        oldRequests = Arrays.stream(old.getProperty("order").split(","))
                .filter(name -> !OLD_REFRESH_REQUESTS.contains(name))
                .toList();
    }

    @Test
    void proofOfWorkPayloadAndCostOfTheNewRequestsAreUnchangedForTheOldVersion() throws Exception {
        List<String> names = OldCompatSupport.namesWithPrefix(updated, "request.");
        assertEquals(10, names.size(), names.toString());
        for (String name : names) {
            EnvelopePayloadMessage message = newRequest(name);
            assertEquals(updated.getProperty("pow." + name), proofOfWorkPayloadHash(message),
                    name + ": the proof of work payload differs, so the proof of work of the new version would fail");
            // The sender may pay more than v2.1.15 requires, for receivers of versions up to 2.1.13 (message-cost.md)
            double paid = Double.parseDouble(updated.getProperty("costToPay." + name));
            assertTrue(paid >= MessageCostPolicy.getCostFactor(message) - 1e-9,
                    name + ": v2.1.15 requires a higher cost factor than the new version pays");
        }
    }

    @Test
    void oldNodeWithItsStoresAppliesTheRequestsOfTheUpdatedNodes() throws Exception {
        List<String> events = new ArrayList<>();
        DataService node = loadedNode("step3-loaded-node", events);
        StorageService storageService = node.getStorageService();

        for (String name : UPDATED_REQUESTS) {
            Set<String> result = process(storageService, newRequest(name));
            assertTrue(result.contains("success"), name + ": " + result);
        }
        // The data of step 1 without chat3, which the updated user removed. chat4 was added and removed.
        List<String> expected = new ArrayList<>(oldApplicationData());
        assertTrue(expected.remove("CommonPublicChatMessage"));
        assertEquals(expected, applicationData(node));
        node.shutdown();
    }

    @Test
    void freshOldNodeAcceptsTheRequestsOfTheNewVersion() throws Exception {
        DataService node = freshNode("step3-fresh-node", new ArrayList<>());
        StorageService storageService = node.getStorageService();

        for (String name : List.of("addRole4", "addUserProfile", "addChat4", "removeChat4", "addMailbox4", "removeMailbox4")) {
            Set<String> result = process(storageService, newRequest(name));
            assertTrue(result.contains("success"), name + ": " + result);
        }
        assertEquals(List.of("AuthorizedBondedRole", "UserProfile"), applicationData(node));
        node.shutdown();
    }

    @Test
    void freshOldNodeAcceptsItsRequestsAfterANewNodeRelayedThem() throws Exception {
        DataService node = freshNode("step3-fresh-node-relayed-by-new", new ArrayList<>());
        StorageService storageService = node.getStorageService();

        for (String name : oldRequests) {
            EnvelopePayloadMessage relayedCopy = parse(updated.getProperty("relayedByNewNode." + name));
            assertEquals(old.getProperty("pow." + name), proofOfWorkPayloadHash(relayedCopy), name);
            assertTrue(process(storageService, relayedCopy).contains("success"), name);
        }
        assertEquals(oldApplicationData(), applicationData(node));
        node.shutdown();
    }

    @Test
    void oldNodeRelaysTheRequestsOfTheNewVersion() throws Exception {
        Map<String, String> out = new TreeMap<>();
        for (String name : OldCompatSupport.namesWithPrefix(updated, "request.")) {
            EnvelopePayloadMessage message = newRequest(name);
            // A node of v2.1.15 serializes a request again when it relays, persists or serves it. It knows signature V2,
            // so the copy keeps it.
            byte[] relayed = message.serialize();
            out.put("relayed." + name, base64(relayed));
            out.put("pow." + name, proofOfWorkPayloadHash(message));
            out.put("cost." + name, String.valueOf(MessageCostPolicy.getCostFactor(message)));
        }
        OldCompatSupport.write(RELAYED_COPIES_FILE, out,
                "Requests of the new version as a node of v2.1.15 relays them (step 3 of the compatibility chain)");
    }

    private static List<String> oldApplicationData() {
        return List.of(old.getProperty("applicationData").split(","));
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
