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

package bisq.network.p2p.services.confidential.ack;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.network.NetworkIdService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import com.google.protobuf.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageDeliveryStatusServiceTest {
    private static final String KEY_ID_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String KEY_ID_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    // A key of ours which belongs to no network identity, for example the key of the API access
    private static final String KEY_ID_WITHOUT_NETWORK_ID = "cccccccccccccccccccccccccccccccccccccccc";
    private static final String KEY_ID_PEER = "dddddddddddddddddddddddddddddddddddddddd";

    private final KeyPair keyPairA = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair keyPairB = KeyGeneration.generateDefaultEcKeyPair();
    private final NetworkId myNetworkIdA = networkId(1000, keyPairA, KEY_ID_A);
    private final NetworkId myNetworkIdB = networkId(1001, keyPairB, KEY_ID_B);
    private final NetworkId peerNetworkId = networkId(2000, KeyGeneration.generateDefaultEcKeyPair(), KEY_ID_PEER);

    @SuppressWarnings("rawtypes")
    private Persistence persistence;
    private NetworkService networkService;
    private MessageDeliveryStatusService service;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        KeyBundleService keyBundleService = mock(KeyBundleService.class);
        when(keyBundleService.findKeyPair(KEY_ID_A)).thenReturn(Optional.of(keyPairA));
        when(keyBundleService.findKeyPair(KEY_ID_B)).thenReturn(Optional.of(keyPairB));
        when(keyBundleService.findKeyPair(KEY_ID_WITHOUT_NETWORK_ID))
                .thenReturn(Optional.of(KeyGeneration.generateDefaultEcKeyPair()));

        NetworkIdService networkIdService = mock(NetworkIdService.class);
        when(networkIdService.findNetworkIdByKeyId(any())).thenReturn(Optional.empty());
        when(networkIdService.findNetworkIdByKeyId(KEY_ID_A)).thenReturn(Optional.of(myNetworkIdA));
        when(networkIdService.findNetworkIdByKeyId(KEY_ID_B)).thenReturn(Optional.of(myNetworkIdB));

        networkService = mock(NetworkService.class);
        when(networkService.confidentialSend(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        service = new MessageDeliveryStatusService(persistenceService, keyBundleService, networkIdService, networkService);
    }

    // The receiver which a message names is data of the peer. A node creates a network node for each NetworkId which
    // it sends from, so a peer must not be able to choose that NetworkId, for example with another port.
    @Test
    void ackIsSentFromOurNetworkIdAlsoIfTheMessageNamesAnotherAddress() {
        NetworkId receiverWithOurKeyAndOtherAddress = networkId(3000, keyPairA, KEY_ID_A);
        TestAckRequestingMessage message = new TestAckRequestingMessage("message1", peerNetworkId, receiverWithOurKeyAndOtherAddress);

        service.onDecryptedMessage(message, KEY_ID_A);

        NetworkIdWithKeyPair sender = verifyOneAckSent("message1");
        assertEquals(myNetworkIdA, sender.getNetworkId());
        assertSame(keyPairA, sender.getKeyPair());
    }

    // A message which our key A decrypted, but which names our identity B as receiver, gets the AckMessage from A.
    // Otherwise a peer could learn from the sender key of the AckMessage that A and B belong to the same node.
    @Test
    void ackIsSentFromTheIdentityWhoseKeyDecryptedTheMessage() {
        TestAckRequestingMessage message = new TestAckRequestingMessage("message1", peerNetworkId, myNetworkIdB);

        service.onDecryptedMessage(message, KEY_ID_A);

        NetworkIdWithKeyPair sender = verifyOneAckSent("message1");
        assertEquals(myNetworkIdA, sender.getNetworkId());
        assertSame(keyPairA, sender.getKeyPair());
    }

    @Test
    void noAckIfTheDecryptingKeyBelongsToNoNetworkIdentity() {
        NetworkId receiver = networkId(3000, KeyGeneration.generateDefaultEcKeyPair(), KEY_ID_WITHOUT_NETWORK_ID);
        TestAckRequestingMessage message = new TestAckRequestingMessage("message1", peerNetworkId, receiver);

        service.onDecryptedMessage(message, KEY_ID_WITHOUT_NETWORK_ID);

        verify(networkService, never()).confidentialSend(any(), any(), any());
    }

    @Test
    void secondCopyOfAMessageGetsNoSecondAck() {
        TestAckRequestingMessage message = new TestAckRequestingMessage("message1", peerNetworkId, myNetworkIdA);

        service.onDecryptedMessage(message, KEY_ID_A);
        service.onDecryptedMessage(message, KEY_ID_A);

        verifyOneAckSent("message1");
    }

    // Two separate nodes would both send an AckMessage, so two identities of one node do the same
    @Test
    void eachIdentityAcknowledgesAMessageWithTheSameId() {
        service.onDecryptedMessage(new TestAckRequestingMessage("message1", peerNetworkId, myNetworkIdA), KEY_ID_A);
        service.onDecryptedMessage(new TestAckRequestingMessage("message1", peerNetworkId, myNetworkIdB), KEY_ID_B);

        ArgumentCaptor<NetworkIdWithKeyPair> senders = ArgumentCaptor.forClass(NetworkIdWithKeyPair.class);
        verify(networkService, times(2)).confidentialSend(eq(new AckMessage("message1")), eq(peerNetworkId), senders.capture());
        assertEquals(List.of(myNetworkIdA, myNetworkIdB),
                senders.getAllValues().stream().map(NetworkIdWithKeyPair::getNetworkId).toList());
    }

    @Test
    void ackedMessagesAreBoundedAndTheOldestIsDropped() {
        int numMessages = MessageDeliveryStatusService.MAX_ACKED_MESSAGES + 5;
        for (int i = 0; i < numMessages; i++) {
            service.onDecryptedMessage(new TestAckRequestingMessage("message" + i, peerNetworkId, myNetworkIdA), KEY_ID_A);
        }

        assertEquals(MessageDeliveryStatusService.MAX_ACKED_MESSAGES, service.getNumAckedMessages());
        verify(networkService, times(numMessages)).confidentialSend(any(), any(), any());

        // The newest entry is still known, so its copy gets no second AckMessage
        service.onDecryptedMessage(new TestAckRequestingMessage("message" + (numMessages - 1), peerNetworkId, myNetworkIdA), KEY_ID_A);
        verify(networkService, times(numMessages)).confidentialSend(any(), any(), any());

        // The oldest entry was dropped, so its copy gets a second AckMessage, which is harmless
        service.onDecryptedMessage(new TestAckRequestingMessage("message0", peerNetworkId, myNetworkIdA), KEY_ID_A);
        verify(networkService, times(numMessages + 1)).confidentialSend(any(), any(), any());
        assertEquals(MessageDeliveryStatusService.MAX_ACKED_MESSAGES, service.getNumAckedMessages());
    }

    @Test
    void ackForAnUnknownMessageIdCreatesNoEntry() {
        service.onDecryptedMessage(new AckMessage("unknown"), KEY_ID_A);

        assertTrue(service.getMessageDeliveryStatusByMessageId().isEmpty());
        assertTrue(service.getPersistableStore().getCreationDateByMessageId().isEmpty());
        verify(persistence, never()).persistAsync(any());
    }

    @Test
    void ackForOurMessageSetsTheReceivedStatus() {
        service.applyMessageDeliveryStatus("sent", MessageDeliveryStatus.SENT);
        service.applyMessageDeliveryStatus("inMailbox", MessageDeliveryStatus.ADDED_TO_MAILBOX);

        service.onDecryptedMessage(new AckMessage("sent"), KEY_ID_A);
        service.onDecryptedMessage(new AckMessage("inMailbox"), KEY_ID_A);

        Map<String, MessageDeliveryStatus> statusByMessageId = Map.of(
                "sent", service.getMessageDeliveryStatusByMessageId().get("sent").get(),
                "inMailbox", service.getMessageDeliveryStatusByMessageId().get("inMailbox").get());
        assertEquals(Map.of("sent", MessageDeliveryStatus.ACK_RECEIVED,
                "inMailbox", MessageDeliveryStatus.MAILBOX_MSG_RECEIVED), statusByMessageId);
    }

    private NetworkIdWithKeyPair verifyOneAckSent(String messageId) {
        ArgumentCaptor<NetworkIdWithKeyPair> sender = ArgumentCaptor.forClass(NetworkIdWithKeyPair.class);
        verify(networkService).confidentialSend(eq(new AckMessage(messageId)), eq(peerNetworkId), sender.capture());
        return sender.getValue();
    }

    private static NetworkId networkId(int port, KeyPair keyPair, String keyId) {
        Address address = Address.from("127.0.0.1", port);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(keyPair.getPublic(), keyId));
    }

    private record TestAckRequestingMessage(String ackRequestingMessageId,
                                            NetworkId sender,
                                            NetworkId receiver) implements AckRequestingMessage {
        @Override
        public String getAckRequestingMessageId() {
            return ackRequestingMessageId;
        }

        @Override
        public NetworkId getSender() {
            return sender;
        }

        @Override
        public NetworkId getReceiver() {
            return receiver;
        }

        @Override
        public void verify() {
        }

        @Override
        public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Message.Builder getValueBuilder(boolean serializeForHash) {
            throw new UnsupportedOperationException();
        }
    }
}
