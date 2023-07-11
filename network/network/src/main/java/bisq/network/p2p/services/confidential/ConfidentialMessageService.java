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

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.ExceptionUtil;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.*;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.KeyPairService;
import bisq.security.PubKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static bisq.network.NetworkService.DISPATCHER;
import static java.util.concurrent.CompletableFuture.*;

@Slf4j
public class ConfidentialMessageService implements Node.Listener, DataService.Listener {

    public enum State {
        SENT,
        ADDED_TO_MAILBOX,
        FAILED
    }

    @Getter
    public static class Result {
        private final State state;
        private DataService.BroadCastDataResult mailboxFuture = new DataService.BroadCastDataResult();
        private Optional<String> errorMsg = Optional.empty();

        public Result(State state) {
            this.state = state;
        }

        public Result setMailboxFuture(DataService.BroadCastDataResult mailboxFuture) {
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

    private final Set<MessageListener> listeners = new CopyOnWriteArraySet<>();
    private final NodesById nodesById;
    private final KeyPairService keyPairService;
    private final Optional<DataService> dataService;

    public ConfidentialMessageService(NodesById nodesById, KeyPairService keyPairService, Optional<DataService> dataService) {
        this.nodesById = nodesById;
        this.keyPairService = keyPairService;
        this.dataService = dataService;

        nodesById.addNodeListener(this);
        dataService.ifPresent(service -> service.addListener(this));
    }

    public CompletableFuture<Boolean> shutdown() {
        nodesById.removeNodeListener(this);
        dataService.ifPresent(service -> service.removeListener(this));
        listeners.clear();
        return completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage, Connection connection, String nodeId) {
        if (networkMessage instanceof ConfidentialMessage) {
            processConfidentialMessage((ConfidentialMessage) networkMessage);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // DataService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onAuthenticatedDataAdded(AuthenticatedData authenticatedData) {
    }

    @Override
    public void onMailboxDataAdded(MailboxData mailboxData) {
        ConfidentialMessage confidentialMessage = mailboxData.getConfidentialMessage();
        processConfidentialMessage(confidentialMessage)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        if (result) {
                            dataService.ifPresent(service -> {
                                // If we are successful the msg must be for us, so we have the key
                                KeyPair myKeyPair = keyPairService.findKeyPair(confidentialMessage.getReceiverKeyId()).orElseThrow();
                                service.removeMailboxData(mailboxData, myKeyPair);
                            });
                        } else {
                            log.debug("We are not the receiver of that mailbox message");
                        }
                    } else {
                        throwable.printStackTrace();
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Result send(NetworkMessage networkMessage,
                       Address address,
                       PubKey receiverPubKey,
                       KeyPair senderKeyPair,
                       String senderNodeId) {
        try {
            // Node gets initialized at higher level services
            nodesById.assertNodeIsInitialized(senderNodeId);
            Connection connection = nodesById.getConnection(senderNodeId, address);
            return send(networkMessage, connection, receiverPubKey, senderKeyPair, senderNodeId);
        } catch (Throwable throwable) {
            if (networkMessage instanceof MailboxMessage) {
                ConfidentialMessage confidentialMessage = getConfidentialMessage(networkMessage, receiverPubKey, senderKeyPair);
                return storeMailBoxMessage((MailboxMessage) networkMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending proto failed and proto is not type of MailboxMessage. proto={}", networkMessage);
                return new Result(State.FAILED).setErrorMsg("Sending proto failed and proto is not type of MailboxMessage. Exception=" + throwable);
            }
        }
    }

    private Result send(NetworkMessage networkMessage,
                        Connection connection,
                        PubKey receiverPubKey,
                        KeyPair senderKeyPair,
                        String senderNodeId) {
        ConfidentialMessage confidentialMessage = getConfidentialMessage(networkMessage, receiverPubKey, senderKeyPair);
        try {
            // Node gets initialized at higher level services
            nodesById.assertNodeIsInitialized(senderNodeId);
            nodesById.send(senderNodeId, confidentialMessage, connection);
            return new Result(State.SENT);
        } catch (Throwable throwable) {
            if (networkMessage instanceof MailboxMessage) {
                return storeMailBoxMessage((MailboxMessage) networkMessage, confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending proto failed and proto is not type of MailboxMessage. proto={}", networkMessage);
                return new Result(State.FAILED).setErrorMsg("Sending proto failed and proto is not type of MailboxMessage. Exception=" + throwable);
            }
        }
    }

    public void addMessageListener(MessageListener messageListener) {
        listeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        listeners.remove(messageListener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private Result storeMailBoxMessage(MailboxMessage mailboxMessage,
                                       ConfidentialMessage confidentialMessage,
                                       PubKey receiverPubKey,
                                       KeyPair senderKeyPair) {
        if (dataService.isEmpty()) {
            log.warn("We have not stored the mailboxMessage because the dataService is not present. mailboxMessage={}", mailboxMessage);
            return new Result(State.FAILED).setErrorMsg("We have not stored the mailboxMessage because the dataService is not present.");
        }

        MailboxData mailboxData = new MailboxData(confidentialMessage, mailboxMessage.getMetaData());
        // We do not wait for the broadcast result as that can take a while. We pack the future into our result, 
        // so clients can react on it as they wish.
        DataService.BroadCastDataResult mailboxFuture = dataService.get().addMailboxData(mailboxData,
                        senderKeyPair,
                        receiverPubKey.getPublicKey())
                .join();
        return new Result(State.ADDED_TO_MAILBOX).setMailboxFuture(mailboxFuture);
    }

    private ConfidentialMessage getConfidentialMessage(NetworkMessage networkMessage, PubKey receiverPubKey, KeyPair senderKeyPair) {
        try {
            ConfidentialData confidentialData = HybridEncryption.encryptAndSign(networkMessage.serialize(), receiverPubKey.getPublicKey(), senderKeyPair);
            return new ConfidentialMessage(confidentialData, receiverPubKey.getKeyId());
        } catch (GeneralSecurityException e) {
            log.error("HybridEncryption.encryptAndSign failed at getConfidentialMessage.", e);
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Boolean> processConfidentialMessage(ConfidentialMessage confidentialMessage) {
        return keyPairService.findKeyPair(confidentialMessage.getReceiverKeyId())
                .map(receiversKeyPair -> supplyAsync(() -> {
                    try {
                        ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
                        byte[] decryptedBytes = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                        bisq.network.protobuf.NetworkMessage decryptedProto = bisq.network.protobuf.NetworkMessage.parseFrom(decryptedBytes);
                        NetworkMessage decryptedNetworkMessage = NetworkMessage.fromProto(decryptedProto);
                        runAsync(() -> listeners.forEach(l -> l.onMessage(decryptedNetworkMessage)), DISPATCHER);
                        return true;
                    } catch (Exception e) {
                        log.error(ExceptionUtil.print(e));
                        throw new RuntimeException(e);
                    }
                }, ExecutorFactory.WORKER_POOL))
                .orElse(CompletableFuture.completedFuture(false)); // We don't have a key for that receiverKeyId
    }
}
