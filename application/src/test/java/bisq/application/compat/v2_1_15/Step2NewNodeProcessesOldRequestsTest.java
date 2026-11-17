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

package bisq.application.compat.v2_1_15;

import bisq.application.ResolverConfig;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.application.compat.CompatSupport;
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.application.DevMode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.MessageCostPolicy;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxDataStorageService;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.persistence.PersistenceService;
import bisq.security.SignatureUtil;
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
 * which a node of v2.1.15 created in step 1, and checks how nodes of this version process them before and after the
 * cutoff date of signature V1. v2.1.15 signs every request with signature V2, so the cutoff date changes nothing for
 * its requests. The step then creates the requests of an updated oracle node and an updated user and writes them for
 * step 3, in which a node of v2.1.15 processes them.
 * <p>
 * The expected results follow docs/specifications/network/storage-request-signatures.md (cutoff date),
 * storage-refresh-requests.md (admission and application of refresh requests), storage-ownership.md (owner of
 * authorized data) and message-cost.md.
 */
@EnabledIfSystemProperty(named = CompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step2NewNodeProcessesOldRequestsTest {
    private static final long BEFORE_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime() - 1;
    private static final long AT_CUTOFF = SIGNATURE_V1_CUTOFF_DATE.getTime();

    // The requests of step 1, in the order in which the node of v2.1.15 created and processed them
    private static List<String> oldRequests;
    // The refresh request of v2.1.15, and the add request which the refresh left in its store. A node which applied the
    // refresh request holds this add request already.
    private static final String REFRESH = "refreshUserProfile";
    private static final String REFRESHED_ADD = "refreshedAddUserProfile";

    private static Properties old;

    @BeforeAll
    static void readTheRequestsOfTheOldVersion() throws Exception {
        // As in step 1: the static oracle keys are the dev keys in dev mode
        DevMode.setDevMode(true);
        ResolverConfig.config();
        old = CompatSupport.read(OLD_REQUESTS_FILE);
        oldRequests = List.of(old.getProperty("order").split(","));
        assertEquals(Set.copyOf(oldRequests), Set.copyOf(CompatSupport.namesWithPrefix(old, "request.")),
                "The order of step 1 must name each of its requests");
        assertTrue(oldRequests.containsAll(List.of(REFRESH, REFRESHED_ADD)), oldRequests.toString());
    }

    @Test
    void proofOfWorkPayloadAndCostOfTheOldRequestsAreUnchanged() throws Exception {
        for (String name : oldRequests) {
            EnvelopePayloadMessage message = oldRequest(name);
            assertEquals(old.getProperty("pow." + name), proofOfWorkPayloadHash(message),
                    name + ": the proof of work payload differs, so the proof of work of v2.1.15 would fail");
            assertEquals(Double.parseDouble(old.getProperty("cost." + name)), MessageCostPolicy.getCostFactor(message), 1e-9,
                    name + ": the cost factor differs from the one which v2.1.15 pays");
        }
    }

    @Test
    void freshNodeAcceptsTheRequestsOfTheOldVersionBeforeTheCutoff() throws Exception {
        assertFreshNodeAcceptsTheRequestsOfTheOldVersion("step2-fresh-node-before-cutoff", BEFORE_CUTOFF);
    }

    @Test
    void freshNodeAcceptsTheRequestsOfTheOldVersionFromTheCutoffOn() throws Exception {
        // Rule 9 of storage-request-signatures.md rejects only requests without signature V2
        assertFreshNodeAcceptsTheRequestsOfTheOldVersion("step2-fresh-node-at-cutoff", AT_CUTOFF);
    }

    @Test
    void nodeLoadsTheStoresOfTheOldVersionBeforeTheCutoff() throws Exception {
        assertNodeLoadsTheStoresOfTheOldVersion("step2-loaded-node-before-cutoff", BEFORE_CUTOFF);
    }

    @Test
    void nodeLoadsTheStoresOfTheOldVersionFromTheCutoffOn() throws Exception {
        // The stores remove only the requests without signature V2 at load (rule 9 of storage-request-signatures.md)
        assertNodeLoadsTheStoresOfTheOldVersion("step2-loaded-node-at-cutoff", AT_CUTOFF);
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

        // v2.1.15 published the authorized data with its authorized key, the owner of the data. The publisher
        // migration of storage-ownership.md (rules 14 and 15) therefore finds nothing to migrate.
        List<String> storedEntries = describeStores(storageService);
        node.migratePublisherOfAuthorizedData(oracleIdentityKeyPair, oracleAuthorizedKeyPair);
        assertEquals(List.of(), events, "The migration must not change data which its owner published");
        assertEquals(storedEntries, describeStores(storageService));

        // The updated oracle publishes its role again (sequence number 4)
        AuthorizedBondedRole mediatorRole = AuthorizedBondedRole.fromProto(
                bisq.bonded_roles.protobuf.AuthorizedBondedRole.parseFrom(fromBase64(old.getProperty("data.mediatorRole"))));
        AuthenticatedDataStorageService roleStore = storageService.getOrCreateAuthenticatedDataStore("AuthorizedBondedRole").join();
        AuthorizedData authorizedRole = new AuthorizedData(mediatorRole,
                Optional.of(SignatureUtil.sign(mediatorRole.serializeForHash(), oracleAuthorizedKeyPair.getPrivate())),
                oracleAuthorizedKeyPair.getPublic());
        AddAuthenticatedDataRequest addRole4 = AddAuthenticatedDataRequest.from(roleStore, authorizedRole, oracleAuthorizedKeyPair);
        assertSuccess("addRole4", roleStore.add(addRole4));
        assertEquals(4, addRole4.getAuthenticatedSequentialData().getSequenceNumber());
        put(out, "addRole4", addRole4);

        // The updated user refreshes its profile, which v2.1.15 refreshed before (sequence number 3)
        UserProfile userProfile = UserProfile.fromProto(bisq.user.protobuf.UserProfile.parseFrom(fromBase64(old.getProperty("data.userProfile"))));
        AuthenticatedDataStorageService userProfileStore = storageService.getOrCreateAuthenticatedDataStore("UserProfile").join();
        DefaultAuthenticatedData userProfileData = new DefaultAuthenticatedData(userProfile);
        RefreshAuthenticatedDataRequest refresh = RefreshAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair);
        assertSuccess(REFRESH, userProfileStore.refresh(refresh));
        put(out, REFRESH, refresh);
        AddAuthenticatedDataRequest refreshedAdd = (AddAuthenticatedDataRequest) userProfileStore.getPersistableStore().getMap().values().iterator().next();
        assertTrue(refreshedAdd.isSignatureV2Present(), "The refreshed add request must have signature V2");
        put(out, REFRESHED_ADD, refreshedAdd);

        AddAuthenticatedDataRequest addUserProfile = AddAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair);
        assertSuccess("addUserProfile", userProfileStore.add(addUserProfile));
        put(out, "addUserProfile", addUserProfile);

        // The updated user removes chat3, which v2.1.15 published, and publishes and removes chat4
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

        // The updated receiver removes mailbox1, which v2.1.15 sent. The updated sender sends mailbox4.
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

        // A node of this version relays the requests of v2.1.15 with its own serialization. Step 3 checks that nodes of
        // v2.1.15 accept these copies.
        for (String name : oldRequests) {
            out.put("relayedByNewNode." + name, base64(oldRequest(name).serialize()));
        }

        node.shutdown();
        CompatSupport.write(NEW_REQUESTS_FILE, out, "Requests of an updated node (step 2 of the compatibility chain)");
    }

    private static void assertFreshNodeAcceptsTheRequestsOfTheOldVersion(String workDirName, long now) throws Exception {
        List<String> events = new ArrayList<>();
        DataService node = newNode(workDirName, now, events);
        Map<String, Set<String>> results = processOldRequests(node.getStorageService());

        for (String name : oldRequests) {
            if (!name.equals(REFRESHED_ADD)) {
                assertTrue(results.get(name).contains("success"), name + ": " + results.get(name));
            }
        }
        // The node applied the refresh request, so it holds the refreshed add request already (rules 6 and 9 of
        // storage-refresh-requests.md). The add request is valid, but it does not replace the equal stored request.
        assertFalse(results.get(REFRESHED_ADD).contains("success"), results.get(REFRESHED_ADD).toString());
        assertFalse(results.get(REFRESHED_ADD).contains("signatureInvalid"), results.get(REFRESHED_ADD).toString());

        assertEquals(oldStoredEntries(), describeStores(node.getStorageService()));
        // The authorized key of the oracle published the authorized data, so the applications get it (storage-ownership.md)
        assertEquals(oldApplicationData(), applicationData(node));
        assertTrue(events.containsAll(List.of("authorizedAdded:AuthorizedBondedRole", "authorizedAdded:AuthorizedTimestampData")),
                events.toString());
        node.shutdown();
    }

    private static void assertNodeLoadsTheStoresOfTheOldVersion(String workDirName, long now) throws Exception {
        PersistenceService persistenceService = new PersistenceService(copyOfOldDataDir(workDirName));
        DataService node = new DataService(persistenceService, () -> now);
        assertTrue(persistenceService.readAllPersisted().join());

        assertEquals(oldStoredEntries(), describeStores(node.getStorageService()), "Entries of v2.1.15 were lost at load");
        assertEquals(oldApplicationData(), applicationData(node));
        node.shutdown();
    }

    private static List<String> oldStoredEntries() {
        return List.of(old.getProperty("storedEntries").split(";"));
    }

    private static List<String> oldApplicationData() {
        return Arrays.asList(old.getProperty("applicationData").split(","));
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
        for (String name : oldRequests) {
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
