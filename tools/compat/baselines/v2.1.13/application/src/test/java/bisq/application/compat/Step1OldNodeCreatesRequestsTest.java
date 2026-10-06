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
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ack.AckMessage;
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
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.SignatureUtil;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.security.pow.ProofOfWork;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.data.AuthorizedTimestampData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static bisq.application.compat.OldCompatSupport.OLD_DATA_DIR;
import static bisq.application.compat.OldCompatSupport.OLD_REQUESTS_FILE;
import static bisq.application.compat.OldCompatSupport.applicationData;
import static bisq.application.compat.OldCompatSupport.base64;
import static bisq.application.compat.OldCompatSupport.describeStores;
import static bisq.application.compat.OldCompatSupport.exchangeDir;
import static bisq.application.compat.OldCompatSupport.flags;
import static bisq.application.compat.OldCompatSupport.proofOfWorkPayloadHash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step 1 of the compatibility chain, with the code of v2.1.13. A node of v2.1.13 creates the requests which an honest
 * node of this version creates, processes them with its own stores and persists the stores. The step writes the
 * requests, the keys, the stored entries and the persisted data directory into the exchange directory. Step 2 reads
 * them with the code of the new version.
 * <p>
 * The keys are: A, the network identity of an oracle node, which published authorized data in v2.1.13. B, the
 * authorized key of that oracle node. U, a user. R, the receiver of mailbox messages of U.
 * <p>
 * Nodes of the new version accept authorized data only from the static oracle keys. The step therefore runs in dev
 * mode, as a local test network does, and B is the first dev key of AuthorizedPubKeys.
 */
@EnabledIfSystemProperty(named = OldCompatSupport.EXCHANGE_DIR_PROPERTY, matches = ".+")
class Step1OldNodeCreatesRequestsTest {
    // The first dev key pair of the Javadoc of AuthorizedPubKeys, the same in both versions
    private static final String DEV_PRIVATE_KEY_1 = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY_1 = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";

    private final Map<String, String> out = new TreeMap<>();

    @Test
    void oldNodeCreatesAndStoresTheRequestsOfAnHonestNode() throws Exception {
        DevMode.setDevMode(true);
        ResolverConfig.config();
        Path dataDir = exchangeDir().resolve(OLD_DATA_DIR);
        OldCompatSupport.deleteRecursively(dataDir);

        KeyPair oracleIdentityKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair oracleAuthorizedKeyPair = new KeyPair(KeyGeneration.generatePublic(Hex.decode(DEV_PUBLIC_KEY_1)),
                KeyGeneration.generatePrivate(Hex.decode(DEV_PRIVATE_KEY_1)));
        KeyPair userKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        putKey("A", oracleIdentityKeyPair);
        putKey("B", oracleAuthorizedKeyPair);
        putKey("U", userKeyPair);
        putKey("R", receiverKeyPair);

        DataService dataService = new DataService(new PersistenceService(dataDir));
        StorageService storageService = dataService.getStorageService();

        // User profile of U
        NetworkId userNetworkId = networkId(userKeyPair, 1001);
        UserProfile userProfile = UserProfile.createNew("compat-user",
                new ProofOfWork(userNetworkId.getPubKey().getHash(), 0, null, 1, new byte[72], 0),
                0, userNetworkId, "", "");
        out.put("data.userProfile", base64(userProfile.serialize()));
        out.put("profileId", userProfile.getId());
        AuthenticatedDataStorageService userProfileStore = storageService.getOrCreateAuthenticatedDataStore("UserProfile").join();
        DefaultAuthenticatedData userProfileData = new DefaultAuthenticatedData(userProfile);
        put("addUserProfile", AddAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair), userProfileStore::add);

        // Public chat messages of U: chat1 stays, U removes chat2 with v2.1.13, and the updated U removes chat3 in step 2
        AuthenticatedDataStorageService chatStore = storageService.getOrCreateAuthenticatedDataStore("CommonPublicChatMessage").join();
        CommonPublicChatMessage chat2 = null;
        for (int i = 1; i <= 3; i++) {
            CommonPublicChatMessage chat = chat(userProfile.getId(), "message " + i + " of v2.1.13");
            if (i == 2) {
                chat2 = chat;
            }
            out.put("data.chat" + i, base64(chat.serialize()));
            put("addChat" + i, AddAuthenticatedDataRequest.from(chatStore, new DefaultAuthenticatedData(chat), userKeyPair), chatStore::add);
        }
        put("removeChat2", RemoveAuthenticatedDataRequest.from(chatStore, new DefaultAuthenticatedData(chat2), userKeyPair), chatStore::remove);

        // Authorized data of the oracle node. v2.1.13 publishes it with the network identity A, signed by B. An oracle
        // with static keys marks its data with staticPublicKeysProvided. The oracle republished the role twice, so its
        // sequence number is 3.
        AuthenticatedDataStorageService roleStore = storageService.getOrCreateAuthenticatedDataStore("AuthorizedBondedRole").join();
        AuthorizedBondedRole mediatorRole = new AuthorizedBondedRole("b".repeat(40),
                Hex.encode(KeyGeneration.generateDefaultEcKeyPair().getPublic().getEncoded()),
                BondedRoleType.MEDIATOR,
                "compat-mediator",
                "signature",
                Optional.empty(),
                networkId(KeyGeneration.generateDefaultEcKeyPair(), 1002),
                Optional.empty(),
                true);
        out.put("data.mediatorRole", base64(mediatorRole.serialize()));
        for (int i = 1; i <= 3; i++) {
            AuthorizedData authorizedRole = new AuthorizedData(mediatorRole,
                    Optional.of(SignatureUtil.sign(mediatorRole.serializeForHash(), oracleAuthorizedKeyPair.getPrivate())),
                    oracleAuthorizedKeyPair.getPublic());
            put("addRole" + i, AddAuthenticatedDataRequest.from(roleStore, authorizedRole, oracleIdentityKeyPair), roleStore::add);
        }

        AuthenticatedDataStorageService timestampStore = storageService.getOrCreateAuthenticatedDataStore("AuthorizedTimestampData").join();
        AuthorizedTimestampData timestampData = new AuthorizedTimestampData(userProfile.getId(), System.currentTimeMillis() - 1000, true);
        out.put("data.timestamp", base64(timestampData.serialize()));
        AuthorizedData authorizedTimestamp = new AuthorizedData(timestampData,
                Optional.of(SignatureUtil.sign(timestampData.serializeForHash(), oracleAuthorizedKeyPair.getPrivate())),
                oracleAuthorizedKeyPair.getPublic());
        put("addTimestamp", AddAuthenticatedDataRequest.from(timestampStore, authorizedTimestamp, oracleIdentityKeyPair), timestampStore::add);

        // Mailbox messages from U to R. R removes mailbox2 with v2.1.13, and the updated R removes mailbox1 in step 2.
        MailboxDataStorageService mailboxStore = storageService.getOrCreateMailboxDataStore("AckMessage").join();
        MailboxData mailbox2 = null;
        for (int i = 1; i <= 3; i++) {
            MailboxData mailboxData = mailboxData("compat-mailbox-" + i, userKeyPair, receiverKeyPair);
            if (i == 2) {
                mailbox2 = mailboxData;
            }
            put("addMailbox" + i, AddMailboxRequest.from(mailboxData, userKeyPair, receiverKeyPair.getPublic()), mailboxStore::add);
        }
        put("removeMailbox2", RemoveMailboxRequest.from(mailbox2, receiverKeyPair), mailboxStore::remove);

        // v2.1.13 refreshes the user profile of its user. The refresh changes the stored add request (see
        // docs/specifications/network/storage-refresh-requests.md, refreshed add requests of owners of versions up
        // to 2.1.13). Step 2 checks what nodes of the new version do with both.
        Thread.sleep(5);
        put("refreshUserProfile", RefreshAuthenticatedDataRequest.from(userProfileStore, userProfileData, userKeyPair), userProfileStore::refresh);
        AddAuthenticatedDataRequest refreshedAdd = (AddAuthenticatedDataRequest) userProfileStore.getPersistableStore()
                .getMap().values().iterator().next();
        put("refreshedAddUserProfile", refreshedAdd, null);

        for (var store : List.of(userProfileStore, chatStore, roleStore, timestampStore)) {
            store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
        }
        mailboxStore.getPersistence().persistAsync(mailboxStore.getPersistableStore().getClone()).join();

        List<String> storedEntries = describeStores(storageService);
        out.put("storedEntries", String.join(";", storedEntries));
        out.put("applicationData", String.join(",", applicationData(dataService)));
        dataService.shutdown();
        OldCompatSupport.write(OLD_REQUESTS_FILE, out, "Requests and stores of a node of v2.1.13 (step 1 of the compatibility chain)");

        // The requests are valid for v2.1.13 itself. Otherwise the later steps would test invalid requests.
        for (Map.Entry<String, String> entry : out.entrySet()) {
            if (entry.getKey().startsWith("result.")) {
                assertTrue(entry.getValue().contains("success"), entry.getKey() + "=" + entry.getValue());
            }
        }
        assertEquals(List.of(
                        "AckMessage|addMailbox|1",
                        "AckMessage|addMailbox|1",
                        "AckMessage|removeMailbox|" + Integer.MAX_VALUE,
                        "AuthorizedBondedRole|add|3",
                        "AuthorizedTimestampData|add|1",
                        "CommonPublicChatMessage|add|1",
                        "CommonPublicChatMessage|add|1",
                        "CommonPublicChatMessage|remove|2",
                        "UserProfile|add|2"),
                storedEntries);
    }

    private interface Processor<T> {
        DataStorageResult process(T request);
    }

    private <T extends EnvelopePayloadMessage> void put(String name, T request, Processor<? super T> processor) {
        out.put("request." + name, base64(request.serialize()));
        out.put("pow." + name, proofOfWorkPayloadHash(request));
        out.put("cost." + name, String.valueOf(request.getCostFactor()));
        if (processor != null) {
            out.put("result." + name, String.join(",", flags(processor.process(request))));
        }
    }

    private void putKey(String name, KeyPair keyPair) {
        out.put("key." + name + ".private", base64(keyPair.getPrivate().getEncoded()));
        out.put("key." + name + ".public", base64(keyPair.getPublic().getEncoded()));
    }

    private static NetworkId networkId(KeyPair keyPair, int port) {
        AddressByTransportTypeMap addressMap = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(port)));
        return new NetworkId(addressMap, new PubKey(keyPair.getPublic(), "compat-key"));
    }

    private static CommonPublicChatMessage chat(String profileId, String text) {
        return new CommonPublicChatMessage(ChatChannelDomain.DISCUSSION,
                "discussion.bisq",
                profileId,
                text,
                Optional.empty(),
                System.currentTimeMillis(),
                false);
    }

    private static MailboxData mailboxData(String id, KeyPair senderKeyPair, KeyPair receiverKeyPair) throws Exception {
        AckMessage ackMessage = new AckMessage(id);
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(ackMessage.serialize(),
                receiverKeyPair.getPublic(), senderKeyPair);
        // The constructor is private in v2.1.13. ConfidentialMessageService creates the message from the same data.
        Constructor<ConfidentialMessage> constructor = ConfidentialMessage.class.getDeclaredConstructor(ConfidentialData.class, String.class);
        constructor.setAccessible(true);
        ConfidentialMessage confidentialMessage = constructor.newInstance(confidentialData, "compat-receiver-key-id");
        return new MailboxData(ackMessage.getMetaData(), confidentialMessage);
    }
}
