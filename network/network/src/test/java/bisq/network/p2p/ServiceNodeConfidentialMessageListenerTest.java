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

package bisq.network.p2p;

import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.ClearNetAddressType;
import bisq.common.platform.MemoryReportService;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.NetworkExecutors;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.authorization.AuthorizationService;
import bisq.network.p2p.node.transport.ClearNetTransportService;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.confidential.ack.AckMessage;
import bisq.network.p2p.services.data.inventory.InventoryService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The listeners of confidential messages receive only the messages which the ConfidentialMessageService decrypted and
 * whose keys it checked. The chat, trade and support services rely on this. The nodes do not pass their messages to
 * these listeners directly (see docs/specifications/network/confidential-messages.md).
 */
class ServiceNodeConfidentialMessageListenerTest {
    private static final String RECEIVER_KEY_ID = "receiverKeyId";
    private static final AckMessage ACK_MESSAGE = new AckMessage("ackRequestingMessageId");

    @TempDir
    private Path dataDir;
    private final KeyPair receiverKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private final KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
    private ServiceNode serviceNode;

    // The ConfidentialMessageService decrypts on its own thread and notifies its listeners on this executor
    @BeforeAll
    static void initializeNotifyExecutor() {
        NetworkExecutors.initialize(8);
    }

    @AfterAll
    static void shutdownNotifyExecutor() {
        NetworkExecutors.shutdown();
    }

    @BeforeEach
    void setUp() {
        KeyBundleService keyBundleService = mock(KeyBundleService.class);
        when(keyBundleService.findKeyPair(RECEIVER_KEY_ID)).thenReturn(Optional.of(receiverKeyPair));
        ClearNetTransportService.Config transportConfig = new ClearNetTransportService.Config(dataDir,
                -1,
                10_000,
                0,
                0,
                10_000,
                ClearNetAddressType.LOCAL_HOST);
        PersistenceService persistenceService = mock(PersistenceService.class);
        // The PeerGroupService registers a shutdown hook which reads its persistence
        doReturn(mock(Persistence.class)).when(persistenceService)
                .getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any());
        Node.Config nodeConfig = new Node.Config(TransportType.CLEAR,
                Set.of(TransportType.CLEAR),
                Set.of(),
                transportConfig,
                10_000,
                0,
                0,
                12);
        serviceNode = new ServiceNode(new ServiceNode.Config(Set.of(ServiceNode.SupportedService.CONFIDENTIAL)),
                nodeConfig,
                mock(PeerGroupManager.Config.class),
                mock(InventoryService.Config.class),
                keyBundleService,
                persistenceService,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                mock(AuthorizationService.class),
                new HashSet<>(),
                TransportType.CLEAR,
                mock(MemoryReportService.class));
    }

    // A node passes every received message to the listeners of NodesById, on the calling thread. A peer can send an
    // AckMessage, a chat message or a trade message without a ConfidentialMessage.
    @Test
    void messageOfNodeDoesNotReachTheListeners() {
        ConfidentialMessageService.Listener listener = mock(ConfidentialMessageService.Listener.class);
        serviceNode.addConfidentialMessageListener(listener);
        serviceNode.createConfidentialMessageService();

        serviceNode.getNodesById().onMessage(ACK_MESSAGE, mock(Connection.class), mock(NetworkId.class));

        verifyNoInteractions(listener);
    }

    // A service can add its listener before the ServiceNode created the ConfidentialMessageService
    @Test
    void decryptedMessageReachesListenersWhichWereAddedBeforeAndAfterTheServiceWasCreated()
            throws GeneralSecurityException, UnresolvableProtobufException {
        ConfidentialMessageService.Listener addedBefore = mock(ConfidentialMessageService.Listener.class);
        ConfidentialMessageService.Listener addedAfter = mock(ConfidentialMessageService.Listener.class);
        serviceNode.addConfidentialMessageListener(addedBefore);
        serviceNode.createConfidentialMessageService();
        serviceNode.addConfidentialMessageListener(addedAfter);

        serviceNode.getNodesById().onMessage(createConfidentialMessage(ACK_MESSAGE),
                mock(Connection.class),
                mock(NetworkId.class));

        for (ConfidentialMessageService.Listener listener : List.of(addedBefore, addedAfter)) {
            verify(listener, timeout(10_000)).onMessage(ACK_MESSAGE);
            verify(listener, timeout(10_000)).onConfidentialMessage(eq(ACK_MESSAGE),
                    argThat(senderPublicKey -> Arrays.equals(senderPublicKey.getEncoded(),
                            senderKeyPair.getPublic().getEncoded())));
        }
    }

    private ConfidentialMessage createConfidentialMessage(EnvelopePayloadMessage payload)
            throws GeneralSecurityException, UnresolvableProtobufException {
        ConfidentialData confidentialData = HybridEncryption.encryptAndSign(payload.serialize(),
                receiverKeyPair.getPublic(),
                senderKeyPair);
        return ConfidentialMessage.fromProto(bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(confidentialData.toProto(false))
                .setReceiverKeyId(RECEIVER_KEY_ID)
                .build());
    }
}
