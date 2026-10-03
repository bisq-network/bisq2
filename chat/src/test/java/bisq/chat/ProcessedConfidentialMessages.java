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

package bisq.chat;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.NetworkExecutors;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.NetworkMessageResolver;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyBundleService;
import bisq.user.identity.UserIdentity;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lets a real ConfidentialMessageService decrypt chat messages before a chat service starts, as the network layer does
 * with mailbox messages which arrive early at the start of the app. The NetworkService mock passes them to a service
 * which registers, as the NetworkService does. Uses the static NetworkExecutors, so test classes which use it are
 * isolated.
 */
final class ProcessedConfidentialMessages {
    private static final long TIMEOUT_SECONDS = 10;

    record ReceivedMessage(EnvelopePayloadMessage message, UserIdentity sender) {
    }

    private ProcessedConfidentialMessages() {
    }

    static void addToNetworkService(NetworkService networkService,
                                    UserIdentity receiver,
                                    List<ReceivedMessage> receivedMessages) throws Exception {
        ConfidentialMessageService confidentialMessageService = decrypt(receiver, receivedMessages);
        when(networkService.getConfidentialMessageServices()).thenReturn(Set.of(confidentialMessageService));
        doCallRealMethod().when(networkService).addConfidentialMessageListenerAndDeliverProcessedMessages(any());
        // The service nodes add the listener to the confidential message services as well
        doAnswer(invocation -> {
            confidentialMessageService.addListener(invocation.getArgument(0));
            return null;
        }).when(networkService).addConfidentialMessageListener(any());
    }

    private static ConfidentialMessageService decrypt(UserIdentity receiver,
                                                      List<ReceivedMessage> receivedMessages) throws Exception {
        NetworkMessageResolver.addResolver("chat.ChatMessage", ChatMessage.class, ChatMessage.getNetworkMessageResolver());
        NetworkIdWithKeyPair receiverNetworkIdWithKeyPair = receiver.getNetworkIdWithKeyPair();
        KeyPair receiverKeyPair = receiverNetworkIdWithKeyPair.getKeyPair();
        String receiverKeyId = receiverNetworkIdWithKeyPair.getNetworkId().getPubKey().getKeyId();
        KeyBundleService keyBundleService = mock(KeyBundleService.class);
        when(keyBundleService.findKeyPair(receiverKeyId)).thenReturn(Optional.of(receiverKeyPair));
        ConfidentialMessageService confidentialMessageService = new ConfidentialMessageService(mock(NodesById.class),
                keyBundleService,
                Optional.empty(),
                Optional.empty());

        // The service decrypts in a thread of its own. A listener which counts the decrypted messages shows when it
        // is done. The listeners are notified on the NetworkExecutors.
        NetworkExecutors.shutdown();
        NetworkExecutors.initialize(2);
        try {
            CountDownLatch decrypted = new CountDownLatch(receivedMessages.size());
            ConfidentialMessageService.Listener counter = message -> decrypted.countDown();
            confidentialMessageService.addListener(counter);
            for (ReceivedMessage receivedMessage : receivedMessages) {
                confidentialMessageService.onMessage(encrypt(receivedMessage, receiverKeyPair, receiverKeyId),
                        mock(Connection.class),
                        mock(NetworkId.class));
            }
            assertTrue(decrypted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "The messages were not decrypted in time");
            confidentialMessageService.removeListener(counter);
        } finally {
            NetworkExecutors.shutdown();
        }
        return confidentialMessageService;
    }

    private static ConfidentialMessage encrypt(ReceivedMessage receivedMessage,
                                               KeyPair receiverKeyPair,
                                               String receiverKeyId)
            throws GeneralSecurityException, UnresolvableProtobufException {
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(receivedMessage.message().serialize(),
                receiverKeyPair.getPublic(),
                receivedMessage.sender().getNetworkIdWithKeyPair().getKeyPair());
        return ConfidentialMessage.fromProto(bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(confidentialData.toProto(false))
                .setReceiverKeyId(receiverKeyId)
                .build());
    }
}
