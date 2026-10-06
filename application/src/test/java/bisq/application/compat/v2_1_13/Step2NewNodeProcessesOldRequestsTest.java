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
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.application.DevMode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.SignedRequest;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStorageService;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static bisq.application.compat.CompatSupport.NEW_REQUESTS_FILE;
import static bisq.application.compat.CompatSupport.OLD_DATA_DIR;
import static bisq.application.compat.CompatSupport.OLD_REQUESTS_FILE;
import static bisq.application.compat.CompatSupport.applicationData;
import static bisq.application.compat.CompatSupport.base64;
import static bisq.application.compat.CompatSupport.describeStores;
import static bisq.application.compat.CompatSupport.exchangeDir;
import static bisq.application.compat.CompatSupport.flags;
import static bisq.application.compat.CompatSupport.fromBase64;
import static bisq.application.compat.CompatSupport.keyPair;
import static bisq.application.compat.CompatSupport.mailboxData;
import static bisq.application.compat.CompatSupport.parse;
import static bisq.application.compat.CompatSupport.proofOfWorkPayloadHash;
import static bisq.application.compat.CompatSupport.recordingListener;
import static bisq.network.p2p.services.data.storage.SignedRequest.SIGNATURE_V1_CUTOFF_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 2 of the compatibility chain, with the code of this version. It reads the requests and the persisted stores
 * which a node of v2.1.13 created in step 1, and checks how nodes of this version process them before and after the
 * cutoff date of signature V1. It then creates the requests of an updated oracle node and an updated user and writes
 * them for step 3, in which a node of v2.1.13 processes them.
 * <p>
 * The expected results follow docs/specifications/network/storage-request-signatures.md (cutoff date, compatibility),
 * storage-refresh-requests.md (refresh requests of versions up to 2.1.13), storage-ownership.md (publisher migration)
 * and message-cost.md (compatibility).
 */
@EnabledIfSystemProperty(named = CompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step2NewNodeProcessesOldRequestsTest {
    private static final long BEFORE_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime() - 1;
    private static final long AT_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime();

    // The order in which the node of v2.1.13 created and processed the requests in step 1
    private static final List<String> OLD_REQUESTS = List.of(
            "addUserProfile",
            "addChat1", "addChat2", "addChat3", "removeChat2",
            "addRole1", "addRole2", "addRole3", "addTimestamp",
            "addMailbox1", "addMailbox2", "addMailbox3", "removeMailbox2",
            "refreshUserProfile", "refreshedAddUserProfile");
    // A node of v2.1.13 signs only the hash of the data in a refresh request, and its refreshed add request keeps the
    // signature of the previous add request (storage-refresh-requests.md, compatibility)
    private static final Set<String> OLD_REFRESH_REQUESTS = Set.of("refreshUserProfile", "refreshedAddUserProfile");
    private static final List<String> AUTHORIZED_TYPES = List.of("AuthorizedBondedRole", "AuthorizedTimestampData");

    private static Properties old;

    @BeforeAll
    static void readTheRequestsOfTheOldVersion() throws Exception {
        // As in step 1: the static oracle keys are the dev keys in dev mode
        DevMode.setDevMode(true);
        ResolverConfig.config();
        old = CompatSupport.read(OLD_REQUESTS_FILE);
        assertEquals(Set.copyOf(OLD_REQUESTS), Set.copyOf(CompatSupport.namesWithPrefix(old, "request.")),
                "Step 1 must create the requests which this step expects");
    }

    @Test
    void proofOfWorkPayloadAndCostOfTheOldRequestsAreUnchanged() throws Exception {
        for (String name : OLD_REQUESTS) {
            EnvelopePayloadMessage message = oldRequest(name);
            assertEquals(old.getProperty("pow." + name), proofOfWorkPayloadHash(message),
                    name + ": the proof of work payload differs, so the proof of work of v2.1.13 would fail");
            assertEquals(Double.parseDouble(old.getProperty("cost." + name)), MessageCostPolicy.getCostFactor(message), 1e-9,
                    name + ": the cost factor differs from the one which v2.1.13 pays");
        }
    }

    @Test
    void freshNodeAcceptsTheRequestsOfTheOldVersionBeforeTheCutoff() throws Exception {
        List<String> events = new ArrayList<>();
        DataService node = newNode("step2-fresh-node-before-cutoff", BEFORE_CUTOFF, events);
        Map<String, Set<String>> results = processOldRequests(node.getStorageService());

        for (String name : OLD_REQUESTS) {
            if (!OLD_REFRESH_REQUESTS.contains(name)) {
                assertTrue(results.get(name).contains("success"), name + ": " + results.get(name));
            }
        }
        // Not applied, but no severe failure (storage-refresh-requests.md, rule 4 and compatibility)
        assertEquals(Set.of("signatureV2Missing"), results.get("refreshUserProfile"));
        // Known limitation in storage-refresh-requests.md: the refreshed add request of an owner of v2.1.13 keeps the
        // signature of the previous add request
        assertTrue(results.get("refreshedAddUserProfile").contains("signatureInvalid"), results.get("refreshedAddUserProfile").toString());

        assertEquals(List.of(
                        "AckMessage|addMailbox|1",
                        "AckMessage|addMailbox|1",
                        "AckMessage|removeMailbox|" + Integer.MAX_VALUE,
                        "AuthorizedBondedRole|add|3",
                        "AuthorizedTimestampData|add|1",
                        "CommonPublicChatMessage|add|1",
                        "CommonPublicChatMessage|add|1",
                        "CommonPublicChatMessage|remove|2",
                        "UserProfile|add|1"),
                describeStores(node.getStorageService()));
        // The oracle of v2.1.13 published its authorized data with its network identity, which is not the owner of the
        // data. Stores keep and relay such entries, but applications get only data which the owner published
        // (storage-ownership.md).
        assertEquals(List.of("CommonPublicChatMessage", "CommonPublicChatMessage", "UserProfile"), applicationData(node));
        assertFalse(events.stream().anyMatch(event -> event.startsWith("authorized")), events.toString());
        node.shutdown();
    }

    @Test
    void freshNodeRejectsTheRequestsOfTheOldVersionFromTheCutoffOn() throws Exception {
        DataService node = newNode("step2-fresh-node-at-cutoff", AT_CUTOFF, new ArrayList<>());
        Map<String, Set<String>> results = processOldRequests(node.getStorageService());

        for (String name : OLD_REQUESTS) {
            assertTrue(results.get(name).contains("signatureV2Missing"), name + ": " + results.get(name));
            assertFalse(results.get(name).contains("success"), name + ": " + results.get(name));
        }
        assertEquals(List.of(), describeStores(node.getStorageService()));
        node.shutdown();
    }

    @Test
    void nodeLoadsTheStoresOfTheOldVersion() throws Exception {
        PersistenceService persistenceService = new PersistenceService(copyOfOldDataDir("step2-loaded-node-before-cutoff"));
        DataService node = new DataService(persistenceService, () -> BEFORE_CUTOFF);
        assertTrue(persistenceService.readAllPersisted().join());

        List<String> oldEntries = Arrays.asList(old.getProperty("storedEntries").split(";"));
        assertEquals(oldEntries, describeStores(node.getStorageService()), "Entries of v2.1.13 were lost at load");
        assertEquals(List.of("CommonPublicChatMessage", "CommonPublicChatMessage", "UserProfile"), applicationData(node));
        node.shutdown();
    }

    @Test
    void storesOfTheOldVersionKeepOnlyMailboxRemovalsWhenLoadedFromTheCutoffOn() throws Exception {
        PersistenceService persistenceService = new PersistenceService(copyOfOldDataDir("step2-loaded-node-at-cutoff"));
        DataService node = new DataService(persistenceService, () -> AT_CUTOFF);
        assertTrue(persistenceService.readAllPersisted().join());

        // Rule 9 of storage-request-signatures.md: the stores remove the requests without signature V2 at load. Only
        // the mailbox store keeps its removals until they expire.
        assertEquals(List.of("AckMessage|removeMailbox|" + Integer.MAX_VALUE), describeStores(node.getStorageService()));
        assertEquals(List.of(), applicationData(node));
        node.shutdown();
    }

    @Test
    void updatedOracleAndUserCreateTheirRequestsForTheOldVersion() throws Exception {
        KeyPair oracleIdentityKeyPair = keyPair(old, "A");
        KeyPair oracleAuthorizedKeyPair = keyPair(old, "B");
        KeyPair userKeyPair = keyPair(old, "U");
        KeyPair receiverKeyPair = keyPair(old, "R");
        Map<String, String> out = new TreeMap<>();

        List<String> events = new ArrayList<>();
        PersistenceService persistenceService = new PersistenceService(copyOfOldDataDir("step2-updated-node"));
        DataService node = new DataService(persistenceService, () -> BEFORE_CUTOFF);
        assertTrue(persistenceService.readAllPersisted().join());
        node.addListener(recordingListener(events));
        StorageService storageService = node.getStorageService();

        // The upgraded oracle node publishes the data of its earlier publisher again under its authorized key (rules 14
        // and 15 of storage-ownership.md). It removes the entry of the earlier publisher (sequence number + 1) and
        // publishes the data again (sequence number + 2), both with signature V2.
        node.migratePublisherOfAuthorizedData(oracleIdentityKeyPair, oracleAuthorizedKeyPair);
        assertEquals(List.of(
                        "AuthorizedBondedRole|add|5",
                        "AuthorizedBondedRole|remove|4",
                        "AuthorizedTimestampData|add|3",
                        "AuthorizedTimestampData|remove|2"),
                describeStores(storageService).stream()
                        .filter(entry -> AUTHORIZED_TYPES.contains(entry.substring(0, entry.indexOf('|'))))
                        .toList());
        assertEquals(List.of("authorizedAdded:AuthorizedBondedRole", "authorizedAdded:AuthorizedTimestampData"),
                events.stream().sorted().toList());
        for (String className : AUTHORIZED_TYPES) {
            AuthenticatedDataStorageService store = storageService.getOrCreateAuthenticatedDataStore(className).join();
            for (DataRequest request : store.getPersistableStore().getMap().values()) {
                assertTrue(((SignedRequest) request).isSignatureV2Present(), className + " " + request.getClass().getSimpleName());
                if (request instanceof AddAuthenticatedDataRequest add) {
                    assertTrue(add.getAuthenticatedSequentialData().getAuthenticatedData()
                            .isOwner(add.getAuthenticatedSequentialData().getPubKeyHash()), className + ": publisher is not the owner");
                    put(out, "migrationAdd." + className, add);
                } else {
                    put(out, "migrationRemove." + className, (EnvelopePayloadMessage) request);
                }
            }
        }
        events.clear();
        node.migratePublisherOfAuthorizedData(oracleIdentityKeyPair, oracleAuthorizedKeyPair);
        assertEquals(List.of(), events, "A second run of the migration must not change the data");

        // The updated user refreshes its profile, which the store holds as the refreshed add request of v2.1.13. A node
        // which holds a copy without signature V2 applies the refresh request of the owner (storage-refresh-requests.md).
        UserProfile userProfile = UserProfile.fromProto(bisq.user.protobuf.UserProfile.parseFrom(fromBase64(old.getProperty("data.userProfile"))));
        AuthenticatedDataStorageService userProfileStore = storageService.getOrCreateAuthenticatedDataStore("UserProfile").join();
        DefaultAuthenticatedData userProfileData = new DefaultAuthenticatedData(userProfile);
        RefreshAuthenticatedDataRequest refresh = RefreshAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair);
        assertSuccess("refreshUserProfile", userProfileStore.refresh(refresh));
        put(out, "refreshUserProfile", refresh);
        AddAuthenticatedDataRequest refreshedAdd = (AddAuthenticatedDataRequest) userProfileStore.getPersistableStore().getMap().values().iterator().next();
        assertTrue(refreshedAdd.isSignatureV2Present(), "The refreshed add request must have signature V2");
        put(out, "refreshedAddUserProfile", refreshedAdd);

        AddAuthenticatedDataRequest addUserProfile = AddAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair);
        assertSuccess("addUserProfile", userProfileStore.add(addUserProfile));
        put(out, "addUserProfile", addUserProfile);

        // The updated user removes chat3, which v2.1.13 published, and publishes and removes chat4
        AuthenticatedDataStorageService chatStore = storageService.getOrCreateAuthenticatedDataStore("CommonPublicChatMessage").join();
        CommonPublicChatMessage chat3 = (CommonPublicChatMessage) CommonPublicChatMessage.fromProto(
                bisq.chat.protobuf.ChatMessage.parseFrom(fromBase64(old.getProperty("data.chat3"))));
        RemoveAuthenticatedDataRequest removeChat3 = RemoveAuthenticatedDataRequest.from(chatStore, new DefaultAuthenticatedData(chat3), userKeyPair);
        assertSuccess("removeChat3", chatStore.remove(removeChat3));
        put(out, "removeChat3", removeChat3);
        CommonPublicChatMessage chat4 = new CommonPublicChatMessage(ChatChannelDomain.DISCUSSION, "discussion.bisq",
                userProfile.getId(), "message 4 of the new version", Optional.empty(), System.currentTimeMillis(), false);
        AddAuthenticatedDataRequest addChat4 = AddAuthenticatedDataRequest.from(chatStore, new DefaultAuthenticatedData(chat4), userKeyPair);
        assertSuccess("addChat4", chatStore.add(addChat4));
        put(out, "addChat4", addChat4);
        // Created after the add request, but not processed here, so that step 3 and step 4 can process both
        put(out, "removeChat4", RemoveAuthenticatedDataRequest.from(chatStore, new DefaultAuthenticatedData(chat4), userKeyPair));

        // The updated receiver removes mailbox1, which v2.1.13 sent. The updated sender sends mailbox4.
        MailboxDataStorageService mailboxStore = storageService.getOrCreateMailboxDataStore("AckMessage").join();
        AddMailboxRequest oldAddMailbox1 = (AddMailboxRequest) oldRequest("addMailbox1");
        RemoveMailboxRequest removeMailbox1 = RemoveMailboxRequest.from(oldAddMailbox1.getMailboxSequentialData().getMailboxData(), receiverKeyPair);
        assertSuccess("removeMailbox1", mailboxStore.remove(removeMailbox1));
        put(out, "removeMailbox1", removeMailbox1);
        MailboxData mailbox4 = mailboxData("compat-mailbox-4", userKeyPair, receiverKeyPair);
        AddMailboxRequest addMailbox4 = AddMailboxRequest.from(mailbox4, userKeyPair, receiverKeyPair.getPublic());
        assertSuccess("addMailbox4", mailboxStore.add(addMailbox4));
        put(out, "addMailbox4", addMailbox4);
        put(out, "removeMailbox4", RemoveMailboxRequest.from(mailbox4, receiverKeyPair));

        // A node of this version relays the requests of v2.1.13 with its own serialization. Step 3 checks that nodes of
        // v2.1.13 accept these copies.
        for (String name : OLD_REQUESTS) {
            out.put("relayedByNewNode." + name, base64(oldRequest(name).serialize()));
        }

        node.shutdown();
        CompatSupport.write(NEW_REQUESTS_FILE, out, "Requests of an updated node (step 2 of the compatibility chain)");
    }

    private static void put(Map<String, String> out, String name, EnvelopePayloadMessage request) {
        out.put("request." + name, base64(request.serialize()));
        out.put("pow." + name, proofOfWorkPayloadHash(request));
        out.put("costToPay." + name, String.valueOf(MessageCostPolicy.getCostFactorToPay(request)));
    }

    private static void assertSuccess(String name, DataStorageResult result) {
        assertTrue(result.isSuccess(), name + ": " + flags(result));
    }

    private static EnvelopePayloadMessage oldRequest(String name) throws Exception {
        return parse(old.getProperty("request." + name));
    }

    private static Map<String, Set<String>> processOldRequests(StorageService storageService) throws Exception {
        Map<String, Set<String>> results = new LinkedHashMap<>();
        for (String name : OLD_REQUESTS) {
            results.put(name, flags(CompatSupport.process(storageService, oldRequest(name))));
        }
        return results;
    }

    private static DataService newNode(String workDirName, long now, List<String> events) throws Exception {
        DataService node = new DataService(new PersistenceService(CompatSupport.newWorkDir(workDirName)), () -> now);
        node.addListener(recordingListener(events));
        return node;
    }

    private static Path copyOfOldDataDir(String workDirName) throws Exception {
        Path dir = CompatSupport.newWorkDir(workDirName);
        CompatSupport.copyRecursively(exchangeDir().resolve(OLD_DATA_DIR), dir);
        return dir;
    }
}
