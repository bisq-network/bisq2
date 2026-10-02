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

package bisq.network.p2p.services.data.storage;

import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.TestDistributedData;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.TestConfidentialMessages;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Path;
import java.security.KeyPair;

import static bisq.network.p2p.services.data.storage.SignatureV1CutoffTimes.AT_CUTOFF;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

/**
 * A node creates its requests also from the cutoff date of signature V1 on (rule 7 of
 * docs/specifications/network/storage-request-signatures.md). The creator first builds the request without signature
 * V2, because the preimage of signature V2 is the canonical encoding of that request.
 * <p>
 * The constructors of the requests read the system time. The tests therefore replace the cutoff rule for their thread
 * with "signature V2 is required".
 */
class RequestCreationAfterCutoffTest {
    private final KeyPair ownerKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    @TempDir
    private Path tempDir;

    @BeforeAll
    static void registerMetaData() {
        StorableTypesRegistry.register(TestDistributedData.class, TestDistributedData.META_DATA);
        StorableTypesRegistry.register(AckMessage.class, AckMessage.META_DATA);
    }

    @Test
    void ownerCreatesAddAndRemoveRequestsOfAuthenticatedData() throws Exception {
        AuthenticatedDataStorageService store = new AuthenticatedDataStorageService(new PersistenceService(tempDir),
                new PruneExpiredEntriesService(), "owner", TestDistributedData.class.getSimpleName(), () -> AT_CUTOFF);
        AuthenticatedData data = new DefaultAuthenticatedData(new TestDistributedData("content",
                ownerKeyPair.getPublic()));
        try (MockedStatic<SignedRequest> signedRequest = mockStatic(SignedRequest.class, CALLS_REAL_METHODS)) {
            signedRequest.when(() -> SignedRequest.isSignatureV2Required(anyLong())).thenReturn(true);

            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, data, ownerKeyPair);
            assertTrue(store.add(addRequest).isSuccess());
            RemoveAuthenticatedDataRequest removeRequest = RemoveAuthenticatedDataRequest.from(store, data,
                    ownerKeyPair);
            assertTrue(store.remove(removeRequest).isSuccess());

            assertTrue(addRequest.isSignatureV2Present());
            assertFalse(addRequest.isSignatureInvalid(AT_CUTOFF));
            assertTrue(removeRequest.isSignatureV2Present());
            assertFalse(removeRequest.isSignatureInvalid(AT_CUTOFF));
        }
        store.getPersistence().persistAsync(store.getPersistableStore().getClone()).join();
    }

    @Test
    void senderAndReceiverCreateAddAndRemoveRequestsOfMailboxData() throws Exception {
        KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        MailboxData mailboxData = new MailboxData(AckMessage.META_DATA,
                TestConfidentialMessages.createConfidentialMessage(senderKeyPair));
        try (MockedStatic<SignedRequest> signedRequest = mockStatic(SignedRequest.class, CALLS_REAL_METHODS)) {
            signedRequest.when(() -> SignedRequest.isSignatureV2Required(anyLong())).thenReturn(true);

            AddMailboxRequest addRequest = AddMailboxRequest.from(mailboxData, senderKeyPair,
                    receiverKeyPair.getPublic());
            RemoveMailboxRequest removeRequest = RemoveMailboxRequest.from(mailboxData, receiverKeyPair);

            assertTrue(addRequest.isSignatureV2Present());
            assertFalse(addRequest.isSignatureInvalid(AT_CUTOFF));
            assertTrue(removeRequest.isSignatureV2Present());
            assertFalse(removeRequest.isSignatureInvalid(AT_CUTOFF));
        }
    }
}
