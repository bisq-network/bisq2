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

import bisq.common.data.ByteArray;
import bisq.common.util.OsUtils;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.data.storage.Result;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.persistence.PersistenceService;
import bisq.security.ConfidentialData;
import bisq.security.DigestUtil;
import bisq.security.HybridEncryption;
import bisq.security.KeyGeneration;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static bisq.network.p2p.services.data.storage.StorageService.StoreType.MAILBOX_DATA_STORE;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MailboxStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "bisq_StorageTest";

    // @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException, IOException, InterruptedException {
        MockMailboxMessage message = new MockMailboxMessage("test" + UUID.randomUUID());
        PersistenceService persistenceService = new PersistenceService(appDirPath);
        MailboxDataStorageService store = new MailboxDataStorageService(persistenceService,
                MAILBOX_DATA_STORE.getStoreName(),
                message.getMetaData().getFileName());
        store.readPersisted().join();
        KeyPair senderKeyPair = KeyGeneration.generateKeyPair();
        KeyPair receiverKeyPair = KeyGeneration.generateKeyPair();

        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message.serialize(), receiverKeyPair.getPublic(), senderKeyPair);
        ConfidentialMessage confidentialMessage = new ConfidentialMessage(confidentialData, "DEFAULT");
        MailboxData payload = new MailboxData(confidentialMessage, message.getMetaData());

        Map<ByteArray, MailboxRequest> map = store.getPersistableStore().getClone().getMap();
        int initialMapSize = map.size();
        byte[] hash = DigestUtil.hash(payload.serialize());
        int initialSeqNum = store.getSequenceNumber(hash);

        CountDownLatch addLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        store.addListener(new MailboxDataStorageService.Listener() {
            @Override
            public void onAdded(MailboxData mailboxData) {
                assertEquals(payload, mailboxData);
                try {
                    ConfidentialData confidentialData = mailboxData.getConfidentialMessage().getConfidentialData();
                    byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, receiverKeyPair);
                    //todo use proto serialize
                  /*  Object decryptedMessage = ObjectSerializer.deserialize(decrypted);
                    MockMailboxMessage message2 = (MockMailboxMessage) decryptedMessage;
                    assertEquals(message, message2);
                    assertEquals(message, message2);*/
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    fail();
                }
                addLatch.countDown();
            }

            @Override
            public void onRemoved(MailboxData mailboxData) {
                assertEquals(payload, mailboxData);
                try {
                    ConfidentialData confidentialData = mailboxData.getConfidentialMessage().getConfidentialData();
                    byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, receiverKeyPair);
                    //todo use proto serialize
                  /*  Object decryptedMessage = ObjectSerializer.deserialize(decrypted);
                    MockMailboxMessage message2 = (MockMailboxMessage) decryptedMessage;
                    assertEquals(message, message2);
                    assertEquals(message, message2);*/
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    fail();
                }
                removeLatch.countDown();
            }
        });

        AddMailboxRequest request = AddMailboxRequest.from(payload, senderKeyPair, receiverKeyPair.getPublic());
        Result result = store.add(request);
        assertTrue(result.isSuccess());
        addLatch.await(1, TimeUnit.SECONDS);

        ByteArray byteArray = new ByteArray(hash);
        AddMailboxRequest addRequestFromMap = (AddMailboxRequest) map.get(byteArray);
        MailboxSequentialData dataFromMap = addRequestFromMap.getMailboxSequentialData();

        assertEquals(initialSeqNum + 1, dataFromMap.getSequenceNumber());

        MailboxData payloadFromMap = dataFromMap.getMailboxData();
        assertEquals(payloadFromMap, payload);

        // request inventory with old seqNum
        String dataType = payload.getMetaData().getFileName();
        //  Set<FilterItem> filterItems = new HashSet<>();
        //  filterItems.add(new FilterItem(byteArray.getBytes(), initialSeqNum));
        // ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        // Inventory inventory = store.getInventoryList(filter);
        // assertEquals(initialMapSize + 1, inventory.getEntries().size());

        // remove
        RemoveMailboxRequest removeMailboxRequest = RemoveMailboxRequest.from(payload, receiverKeyPair);

        Result removeDataResult = store.remove(removeMailboxRequest);
        removeLatch.await(1, TimeUnit.SECONDS);

        log.info(removeDataResult.toString());
        assertTrue(removeDataResult.isSuccess());

        RemoveAuthenticatedDataRequest removeAuthenticatedDataRequestFromMap = (RemoveAuthenticatedDataRequest) map.get(byteArray);
        assertEquals(Integer.MAX_VALUE, removeAuthenticatedDataRequestFromMap.getSequenceNumber());

        // we must not create a new sealed data as it would have a diff. secret key and so a diff hash...
        // If users re-publish mailbox messages they need to keep the original sealed data and re-use that instead
        // of creating new ones, as otherwise it would appear like a new mailbox msg.

        assertFalse(store.canAddMailboxMessage(payload));
        try {
            // calling getAddMailboxDataRequest without the previous canAddMailboxMessage check will throw
            AddMailboxRequest.from(payload, senderKeyPair, receiverKeyPair.getPublic());
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        // using the old request again would fail as seq number is not allowing it
        Result result2 = store.add(request);
        assertFalse(result2.isSuccess());
        assertTrue(result2.isSequenceNrInvalid());
    }
}
