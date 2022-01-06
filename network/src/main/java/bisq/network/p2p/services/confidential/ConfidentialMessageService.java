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

package bisq.network.p2p.services.confidential;

import bisq.common.ObjectSerializer;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.NetworkUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.*;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import bisq.network.p2p.services.relay.RelayMessage;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class ConfidentialMessageService implements Node.Listener {
    public enum State {
        SENT,
        ADDED_TO_MAILBOX,
        FAILED
    }

    @Getter
    public static class Result {
        private final State state;
        private List<CompletableFuture<BroadcastResult>> mailboxFuture = new ArrayList<>();
        private Optional<String> errorMsg = Optional.empty();

        public Result(State state) {
            this.state = state;
        }

        public Result setMailboxFuture(List<CompletableFuture<BroadcastResult>> mailboxFuture) {
            this.mailboxFuture = mailboxFuture;
            return this;
        }

        public Result setErrorMsg(String errorMsg) {
            this.errorMsg = Optional.of(errorMsg);
            return this;
        }

        @Override
        public String toString() {
            return "[state=" + state + errorMsg.map(error -> ", errorMsg=" + error + "]").orElse("]");
        }
    }

    private final Set<Node.Listener> listeners = new CopyOnWriteArraySet<>();
    private final NodesById nodesById;
    private final KeyPairService keyPairService;
    private final Optional<DataService> dataService;

    public ConfidentialMessageService(NodesById nodesById, KeyPairService keyPairService, Optional<DataService> dataService) {
        this.nodesById = nodesById;
        this.keyPairService = keyPairService;
        this.dataService = dataService;

        nodesById.addNodeListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof ConfidentialMessage confidentialMessage) {
            if (confidentialMessage instanceof RelayMessage) {
                //todo
                // RelayMessage relayMessage = (RelayMessage) message;
                // Address targetAddress = relayMessage.getTargetAddress();
                // send(message, targetAddress);
            } else {
                ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
                keyPairService.findKeyPair(confidentialMessage.getKeyId()).ifPresent(receiversKeyPair -> {
                    ExecutorFactory.WORKER_POOL.submit(() -> {
                        try {
                            byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                            Serializable deserialized = ObjectSerializer.deserialize(decrypted);
                            if (deserialized instanceof Message decryptedMessage) {
                                NetworkService.DISPATCHER.submit(() ->
                                        listeners.forEach(listener -> listener.onMessage(decryptedMessage, connection, nodeId)));
                            } else {
                                log.warn("Deserialized data is not of type Message. deserialized.getClass()={}", deserialized.getClass());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            }
        }
    }

    @Override
    public void onConnection(Connection connection) {
        listeners.forEach(listener -> listener.onConnection(connection));
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
        listeners.forEach(listener -> listener.onDisconnect(connection, closeReason));
    }

    public Result send(Message message,
                       Address address,
                       PubKey receiverPubKey,
                       KeyPair senderKeyPair,
                       String senderNodeId) {
        Connection connection;
        try {
            nodesById.maybeInitializeServer(senderNodeId, NetworkUtils.findFreeSystemPort());
            connection = nodesById.getConnection(senderNodeId, address);
        } catch (Throwable throwable) {
            if (message instanceof MailboxMessage mailboxMessage) {
                ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
                return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                return new Result(State.FAILED).setErrorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
            }
        }
        return send(message, connection, receiverPubKey, senderKeyPair, senderNodeId);
    }

    public Result send(Message message,
                       Connection connection,
                       PubKey receiverPubKey,
                       KeyPair senderKeyPair,
                       String senderNodeId) {
        ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
        try {
            nodesById.maybeInitializeServer(senderNodeId, NetworkUtils.findFreeSystemPort());
            nodesById.send(senderNodeId, confidentialMessage, connection);
            return new Result(State.SENT);
        } catch (Throwable throwable) {
            if (message instanceof MailboxMessage mailboxMessage) {
                return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                return new Result(State.FAILED).setErrorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
            }
        }
    }

    private Result storeMailBoxMessage(MailboxMessage mailboxMessage,
                                       ConfidentialMessage confidentialMessage,
                                       PubKey receiverPubKey,
                                       KeyPair senderKeyPair) {
        if (dataService.isEmpty()) {
            log.warn("We cannot stored a mailboxMessage because the dataService is not present. mailboxMessage={}", mailboxMessage);
            return new Result(State.FAILED).setErrorMsg("We cannot stored a mailboxMessage because the dataService is not present.");
        }

        MailboxPayload mailboxPayload = new MailboxPayload(confidentialMessage, mailboxMessage.getMetaData());
        // We do not wait for the broadcast result as that can take a while. We pack the future into our result, 
        // so clients can react on it as they wish.
        List<CompletableFuture<BroadcastResult>> mailboxFuture = dataService.get().addMailboxPayloadAsync(mailboxPayload,
                        senderKeyPair,
                        receiverPubKey.publicKey())
                .join();
        return new Result(State.ADDED_TO_MAILBOX).setMailboxFuture(mailboxFuture);
    }


    public CompletableFuture<Void> shutdown() {
        nodesById.removeNodeListener(this);
        listeners.clear();
        return CompletableFuture.completedFuture(null);
    }

    public void addMessageListener(Node.Listener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(Node.Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private ConfidentialMessage getConfidentialMessage(Message message, PubKey receiverPubKey, KeyPair senderKeyPair) {
        try {
            ConfidentialData confidentialData = HybridEncryption.encryptAndSign(message.serialize(), receiverPubKey.publicKey(), senderKeyPair);
            return new ConfidentialMessage(confidentialData, receiverPubKey.keyId());
        } catch (GeneralSecurityException e) {
            log.error("HybridEncryption.encryptAndSign failed at getConfidentialMessage.", e);
            throw new RuntimeException(e);
        }
    }

/*
    private Set<Connection> getConnectionsWithSupportedNetwork(NetworkType networkType) {
        return peerGroup.getConnectedPeerByAddress().stream()
                .filter(peer -> peer.getCapability().supportedNetworkTypes().contains(networkType))
                .flatMap(peer -> node.findConnection(peer.getAddress()).stream())
                .collect(Collectors.toSet());
    }*/
}
