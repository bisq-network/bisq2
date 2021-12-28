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

package network.misq.network.p2p.services.confidential;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.ObjectSerializer;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.message.Message;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Connection;
import network.misq.network.p2p.node.Node;
import network.misq.network.p2p.node.NodesById;
import network.misq.network.p2p.services.data.broadcast.BroadcastResult;
import network.misq.network.p2p.services.data.DataService;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import network.misq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import network.misq.network.p2p.services.relay.RelayMessage;
import network.misq.security.ConfidentialData;
import network.misq.security.HybridEncryption;
import network.misq.security.KeyPairRepository;
import network.misq.security.PubKey;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.concurrent.CompletableFuture.runAsync;

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
        private Optional<CompletableFuture<BroadcastResult>> mailboxFuture = Optional.empty();
        private Optional<String> errorMsg = Optional.empty();

        public Result(State state) {
            this.state = state;
        }

        public Result mailboxFuture(CompletableFuture<BroadcastResult> mailboxFuture) {
            this.mailboxFuture = Optional.of(mailboxFuture);
            return this;
        }

        public Result errorMsg(String errorMsg) {
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
    private final KeyPairRepository keyPairRepository;
    private final Optional<DataService> dataService;

    public ConfidentialMessageService(NodesById nodesById, KeyPairRepository keyPairRepository, Optional<DataService> dataService) {
        this.nodesById = nodesById;
        this.keyPairRepository = keyPairRepository;
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
                keyPairRepository.findKeyPair(confidentialMessage.getKeyId()).ifPresent(receiversKeyPair -> {
                    runAsync(() -> {
                        try {
                            byte[] decrypted = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                            Serializable deserialized = ObjectSerializer.deserialize(decrypted);
                            if (deserialized instanceof Message decryptedMessage) {
                                runAsync(() -> listeners.forEach(listener -> listener.onMessage(decryptedMessage, connection, nodeId)),
                                        NetworkService.DISPATCHER);
                            } else {
                                log.warn("Deserialized data is not of type Message. deserialized.getClass()={}", deserialized.getClass());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, NetworkService.WORKER_POOL);
                });
            }
        }
    }

    public Result send(Message message,
                       Address address,
                       PubKey receiverPubKey,
                       KeyPair senderKeyPair,
                       String senderNodeId) {
        Connection connection;
        try {
            connection = nodesById.getConnection(senderNodeId, address);
        } catch (Throwable throwable) {
            if (message instanceof MailboxMessage mailboxMessage) {
                ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
                return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                return new Result(State.FAILED).errorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
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
            nodesById.send(senderNodeId, confidentialMessage, connection);
            return new Result(State.SENT);
        } catch (Throwable throwable) {
            if (message instanceof MailboxMessage mailboxMessage) {
                return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                return new Result(State.FAILED).errorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
            }
        }
    }

    public CompletableFuture<Result> sendAsync(Message message,
                                               Address address,
                                               PubKey receiverPubKey,
                                               KeyPair senderKeyPair,
                                               String senderNodeId) {
        nodesById.getConnectionAsync(senderNodeId, address)
                .handle((connection, throwable) -> {
                    if (throwable == null) {
                        return sendAsync(message, connection, receiverPubKey, senderKeyPair, senderNodeId);
                    } else {
                        if (message instanceof MailboxMessage mailboxMessage) {
                            ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
                            return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
                        } else {
                            log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                            return new Result(State.FAILED).errorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
                        }
                    }
                });

        return nodesById.getConnectionAsync(senderNodeId, address)
                .thenCompose(connection -> sendAsync(message, connection, receiverPubKey, senderKeyPair, senderNodeId))
                .handle((result, throwable) -> {
                    if (throwable == null) {
                        return result;
                    } else {
                        if (message instanceof MailboxMessage mailboxMessage) {
                            ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
                            return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
                        } else {
                            log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                            return new Result(State.FAILED).errorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
                        }
                    }
                });
    }


    public CompletableFuture<Result> sendAsync(Message message,
                                               Connection connection,
                                               PubKey receiverPubKey,
                                               KeyPair senderKeyPair,
                                               String senderNodeId) {
        ConfidentialMessage confidentialMessage = getConfidentialMessage(message, receiverPubKey, senderKeyPair);
        return nodesById.sendAsync(senderNodeId, confidentialMessage, connection)
                .handle((con, throwable) -> {
                    if (throwable == null) {
                        return new Result(State.SENT);
                    } else if (message instanceof MailboxMessage mailboxMessage) {
                        return storeMailBoxMessage(mailboxMessage, confidentialMessage, receiverPubKey, senderKeyPair);
                    } else {
                        log.warn("Sending message failed and message is not type of MailboxMessage. message={}", message);
                        return new Result(State.FAILED).errorMsg("Sending message failed and message is not type of MailboxMessage. Exception=" + throwable);
                    }
                });
    }

    private Result storeMailBoxMessage(MailboxMessage mailboxMessage,
                                       ConfidentialMessage confidentialMessage,
                                       PubKey receiverPubKey,
                                       KeyPair senderKeyPair) {
        if (dataService.isEmpty()) {
            log.warn("We cannot stored a mailboxMessage because the dataService is not present. mailboxMessage={}", mailboxMessage);
            return new Result(State.FAILED).errorMsg("We cannot stored a mailboxMessage because the dataService is not present.");
        }

        MailboxPayload mailboxPayload = new MailboxPayload(confidentialMessage, mailboxMessage.getMetaData());
        CompletableFuture<BroadcastResult> mailboxFuture = dataService.get().addMailboxPayload(mailboxPayload,
                senderKeyPair,
                receiverPubKey.publicKey());
        return new Result(State.ADDED_TO_MAILBOX).mailboxFuture(mailboxFuture);
    }

    public CompletableFuture<Connection> relay(Message message, NetworkId networkId, KeyPair senderKeyPair) {
       /*   Set<Connection> connections = getConnectionsWithSupportedNetwork(peerAddress.getNetworkType());
      Connection outboundConnection = CollectionUtil.getRandomElement(connections);
        if (outboundConnection != null) {
            //todo we need 2 diff. pub keys for encryption here
            // ConfidentialMessage inner = seal(message);
            // RelayMessage relayMessage = new RelayMessage(inner, peerAddress);
            // ConfidentialMessage confidentialMessage = seal(relayMessage);
            // return node.send(confidentialMessage, outboundConnection);
        }*/
        return CompletableFuture.failedFuture(new Exception("No connection supporting that network type found."));
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
