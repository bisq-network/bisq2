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

package network.misq.network.p2p.services.data.storage.mailbox;

import lombok.extern.slf4j.Slf4j;
import network.misq.common.ObjectSerializer;
import network.misq.common.util.OsUtils;
import network.misq.network.p2p.services.data.filter.FilterItem;
import network.misq.network.p2p.services.data.filter.ProtectedDataFilter;
import network.misq.network.p2p.services.data.inventory.Inventory;
import network.misq.network.p2p.services.data.storage.MapKey;
import network.misq.network.p2p.services.data.storage.auth.RemoveRequest;
import network.misq.network.p2p.services.data.storage.auth.Result;
import network.misq.security.DigestUtil;
import network.misq.security.HybridEncryption;
import network.misq.security.KeyGeneration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MailboxStoreTest {
    private final String appDirPath = OsUtils.getUserDataDir() + File.separator + "misq_StorageTest";

    @Test
    public void testAddAndRemoveMailboxMsg() throws GeneralSecurityException, IOException, InterruptedException {
        MockMailboxMessage message = new MockMailboxMessage("test" + UUID.randomUUID());
        MailboxDataStore store = new MailboxDataStore(appDirPath, message.getMetaData());
        KeyPair senderKeyPair = KeyGeneration.generateKeyPair();
        KeyPair receiverKeyPair = KeyGeneration.generateKeyPair();

        MailboxPayload payload = MailboxPayload.createMailboxPayload(message, senderKeyPair, receiverKeyPair.getPublic());
        ConcurrentHashMap<MapKey, MailboxRequest> map = store.getMap();
        int initialMapSize = map.size();
        byte[] hash = DigestUtil.hash(payload.serialize());
        int initialSeqNum = store.getSequenceNumber(hash);

        CountDownLatch addLatch = new CountDownLatch(1);
        CountDownLatch removeLatch = new CountDownLatch(1);
        store.addListener(new MailboxDataStore.Listener() {
            @Override
            public void onAdded(MailboxPayload mailboxPayload) {
                assertEquals(payload, mailboxPayload);
                try {
                    byte[] decrypted = HybridEncryption.decryptAndVerify(mailboxPayload.getConfidentialData(), receiverKeyPair);
                    Object decryptedMessage = ObjectSerializer.deserialize(decrypted);
                    MockMailboxMessage message2 = (MockMailboxMessage) decryptedMessage;
                    assertEquals(message, message2);
                    assertEquals(message, message2);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    fail();
                }
                addLatch.countDown();
            }

            @Override
            public void onRemoved(MailboxPayload mailboxPayload) {
                assertEquals(payload, mailboxPayload);
                try {
                    byte[] decrypted = HybridEncryption.decryptAndVerify(mailboxPayload.getConfidentialData(), receiverKeyPair);
                    Object decryptedMessage = ObjectSerializer.deserialize(decrypted);
                    MockMailboxMessage message2 = (MockMailboxMessage) decryptedMessage;
                    assertEquals(message, message2);
                    assertEquals(message, message2);
                } catch (GeneralSecurityException e) {
                    e.printStackTrace();
                    fail();
                }
                removeLatch.countDown();
            }
        });

        AddMailboxRequest request = AddMailboxRequest.from(store, payload, senderKeyPair, receiverKeyPair.getPublic());
        Result result = store.add(request);
        assertTrue(result.isSuccess());
        addLatch.await(1, TimeUnit.SECONDS);

        MapKey mapKey = new MapKey(hash);
        AddMailboxRequest addRequestFromMap = (AddMailboxRequest) map.get(mapKey);
        MailboxData dataFromMap = addRequestFromMap.getMailboxData();

        assertEquals(initialSeqNum + 1, dataFromMap.getSequenceNumber());

        MailboxPayload payloadFromMap = dataFromMap.getMailboxPayload();
        assertEquals(payloadFromMap, payload);

        // request inventory with old seqNum
        String dataType = payload.getMetaData().getFileName();
        Set<FilterItem> filterItems = new HashSet<>();
        filterItems.add(new FilterItem(mapKey.getHash(), initialSeqNum));
        ProtectedDataFilter filter = new ProtectedDataFilter(dataType, filterItems);
        Inventory inventory = store.getInventory(filter);
        // assertEquals(initialMapSize + 1, inventory.getEntries().size());

        // remove
        RemoveMailboxRequest removeMailboxRequest = RemoveMailboxRequest.from(payload, receiverKeyPair);

        Result removeDataResult = store.remove(removeMailboxRequest);
        removeLatch.await(1, TimeUnit.SECONDS);

        log.info(removeDataResult.toString());
        assertTrue(removeDataResult.isSuccess());

        RemoveRequest removeRequestFromMap = (RemoveRequest) map.get(mapKey);
        assertEquals(Integer.MAX_VALUE, removeRequestFromMap.getSequenceNumber());

        // we must not create a new sealed data as it would have a diff. secret key and so a diff hash...
        // If users re-publish mailbox messages they need to keep the original sealed data and re-use that instead
        // of creating new ones, as otherwise it would appear like a new mailbox msg.

        assertFalse(store.canAddMailboxMessage(payload));
        try {
            // calling getAddMailboxDataRequest without the previous canAddMailboxMessage check will throw
            AddMailboxRequest.from(store, payload, senderKeyPair, receiverKeyPair.getPublic());
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
