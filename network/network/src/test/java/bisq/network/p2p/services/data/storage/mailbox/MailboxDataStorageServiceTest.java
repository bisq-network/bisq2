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

import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.DataStorageResult;
import bisq.network.p2p.services.data.storage.StorableTypesRegistry;
import bisq.network.p2p.services.data.storage.PruneExpiredEntriesService;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;

import static bisq.network.p2p.services.data.storage.mailbox.MailboxClassNameTest.createConfidentialMessage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A mailbox store holds the messages of one MailboxMessage type. The message is encrypted, so the store checks the
 * class name which MailboxData and RemoveMailboxRequest carry.
 */
class MailboxDataStorageServiceTest {
    // A store holds the data of one type. Its key is the class name of that type.
    private static final String STORE_KEY = AckMessage.class.getSimpleName();

    @TempDir
    Path tempDir;

    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private MailboxDataStorageService store;
    private MailboxData mailboxData;

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @BeforeEach
    void setUp() {
        store = new MailboxDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(),
                "mailbox",
                STORE_KEY);
        mailboxData = new MailboxData(AckMessage.META_DATA, createConfidentialMessage(senderKeyPair));
    }

    @Test
    void addAndRemoveRequestsOfStoreTypeAreAccepted() throws GeneralSecurityException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);

        DataStorageResult addResult = store.add(addRequest);
        DataStorageResult removeResult = store.remove(removeRequest);

        assertTrue(addResult.isSuccess(), addResult.toString());
        assertTrue(removeResult.isSuccess(), removeResult.toString());
        // The remove request carries only the hash of the mailbox data. It must find the entry of the add request.
        assertEquals(addRequest.getStorageKey(), removeRequest.getStorageKey());
        assertFalse(removeResult.isNoEntry(), removeResult.toString());
    }

    @Test
    void addRequestWithClassNameOfOtherTypeIsRejected() throws GeneralSecurityException {
        AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair, receiverKeyPair.getPublic());
        // A relay can change className, because the signature does not cover it
        bisq.network.protobuf.AddMailboxRequest.Builder builder = addRequest.toValueProto(false).toBuilder();
        builder.getMailboxSequentialDataBuilder().getMailboxDataBuilder().setClassName("OtherMailboxMessage");
        AddMailboxRequest requestWithOtherClassName = AddMailboxRequest.fromProto(builder.build());

        DataStorageResult result = store.add(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }

    @Test
    void removeRequestWithClassNameOfOtherTypeIsRejected() throws GeneralSecurityException {
        RemoveMailboxRequest removeRequest = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);
        RemoveMailboxRequest requestWithOtherClassName = RemoveMailboxRequest.fromProto(
                removeRequest.toValueProto(false).toBuilder()
                        .setClassName("OtherMailboxMessage")
                        .build());

        DataStorageResult result = store.remove(requestWithOtherClassName);

        assertFalse(result.isSuccess());
        assertTrue(result.isClassNameInvalid(), result.toString());
        assertTrue(store.getPersistableStore().getMap().isEmpty());
    }
}
