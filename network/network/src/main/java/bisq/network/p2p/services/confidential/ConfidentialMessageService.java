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
import bisq.common.util.CompletableFutureUtils;
import bisq.network.common.Address;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.security.ConfidentialData;
import bisq.security.HybridEncryption;
import bisq.security.keys.KeyBundleService;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

import static bisq.network.NetworkService.DISPATCHER;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class ConfidentialMessageService implements Node.Listener, DataService.Listener {
    public interface Listener {
        void onMessage(EnvelopePayloadMessage envelopePayloadMessage);

        default void onConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PublicKey senderPublicKey) {
        }
    }

    private final NodesById nodesById;
    private final KeyBundleService keyBundleService;
    private final Optional<DataService> dataService;
    private final Optional<MessageDeliveryStatusService> messageDeliveryStatusService;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    public ConfidentialMessageService(NodesById nodesById,
                                      KeyBundleService keyBundleService,
                                      Optional<DataService> dataService,
                                      Optional<MessageDeliveryStatusService> messageDeliveryStatusService) {
        this.nodesById = nodesById;
        this.keyBundleService = keyBundleService;
        this.dataService = dataService;
        this.messageDeliveryStatusService = messageDeliveryStatusService;

        nodesById.addNodeListener(this);
        dataService.ifPresent(service -> service.addListener(this));
    }

    public void shutdown() {
        nodesById.removeNodeListener(this);
        dataService.ifPresent(service -> service.removeListener(this));
        listeners.clear();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Node.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof ConfidentialMessage) {
            processConfidentialMessage((ConfidentialMessage) envelopePayloadMessage);
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
    public void onMailboxDataAdded(MailboxData mailboxData) {
        ConfidentialMessage confidentialMessage = mailboxData.getConfidentialMessage();
        processConfidentialMessage(confidentialMessage)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        if (result) {
                            dataService.ifPresent(service -> {
                                // If we are successful the msg must be for us, so we have the key
                                KeyPair myKeyPair = keyBundleService.findKeyPair(confidentialMessage.getReceiverKeyId()).orElseThrow();
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

    public SendConfidentialMessageResult send(EnvelopePayloadMessage envelopePayloadMessage,
                                              Address address,
                                              PubKey receiverPubKey,
                                              KeyPair senderKeyPair,
                                              NetworkId senderNetworkId) {
        log.debug("Send message to {}", address);
        SendConfidentialMessageResult result = new SendConfidentialMessageResult(MessageDeliveryStatus.CONNECTING);
        onResult(envelopePayloadMessage, result);
        try {
            // Node gets initialized at higher level services
            nodesById.assertNodeIsInitialized(senderNetworkId);
            Connection connection = nodesById.getConnection(senderNetworkId, address);
            return send(envelopePayloadMessage, connection, receiverPubKey, senderKeyPair, senderNetworkId);
        } catch (Throwable throwable) {
            if (envelopePayloadMessage instanceof MailboxMessage) {
                log.info("Message could not be sent because of {}.\n" +
                        "We send the message as mailbox message.", throwable.getMessage());
                ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
                result = storeMailBoxMessage(((MailboxMessage) envelopePayloadMessage).getMetaData(),
                        confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending of networkMessage failed and networkMessage is not type of MailboxMessage. networkMessage={}", envelopePayloadMessage);
                result = new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED).setErrorMsg("Sending proto failed and proto is not type of MailboxMessage. Exception=" + throwable);
            }
            onResult(envelopePayloadMessage, result);
            return result;
        }
    }

    private SendConfidentialMessageResult send(EnvelopePayloadMessage envelopePayloadMessage,
                                               Connection connection,
                                               PubKey receiverPubKey,
                                               KeyPair senderKeyPair,
                                               NetworkId senderNetworkId) {
        log.debug("Send message to {}", connection);
        ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
        SendConfidentialMessageResult result;
        try {
            // Node gets initialized at higher level services
            nodesById.assertNodeIsInitialized(senderNetworkId);
            nodesById.send(senderNetworkId, confidentialMessage, connection);
            result = new SendConfidentialMessageResult(MessageDeliveryStatus.SENT);
            onResult(envelopePayloadMessage, result);
            return result;
        } catch (Throwable throwable) {
            if (envelopePayloadMessage instanceof MailboxMessage) {
                log.info("Message could not be sent because of {}.\n" +
                        "We send the message as mailbox message.", throwable.getMessage());
                result = storeMailBoxMessage(((MailboxMessage) envelopePayloadMessage).getMetaData(),
                        confidentialMessage, receiverPubKey, senderKeyPair);
            } else {
                log.warn("Sending message failed and message is not type of MailboxMessage. message={}", envelopePayloadMessage);
                result = new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED).setErrorMsg("Sending proto failed and proto is not type of MailboxMessage. Exception=" + throwable);
            }
            onResult(envelopePayloadMessage, result);
            return result;
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void onResult(EnvelopePayloadMessage envelopePayloadMessage, SendConfidentialMessageResult result) {
        if (envelopePayloadMessage instanceof AckRequestingMessage) {
            messageDeliveryStatusService.ifPresent(service -> {
                String messageId = ((AckRequestingMessage) envelopePayloadMessage).getId();
                service.onMessageSentStatus(messageId, result.getMessageDeliveryStatus());

                // If we tried to store in mailbox we check if at least one successful broadcast happened
                if (result.getMessageDeliveryStatus() == MessageDeliveryStatus.TRY_ADD_TO_MAILBOX) {
                    CompletableFutureUtils.anyOf(result.getMailboxFuture().orElseThrow())
                            .whenComplete((broadcastResult, throwable) -> {
                                if (throwable != null || broadcastResult.getNumSuccess() == 0) {
                                    service.onMessageSentStatus(messageId, MessageDeliveryStatus.FAILED);
                                } else {
                                    service.onMessageSentStatus(messageId, MessageDeliveryStatus.ADDED_TO_MAILBOX);
                                }
                            });
                }
            });
        }
    }

    private SendConfidentialMessageResult storeMailBoxMessage(MetaData metaData,
                                                              ConfidentialMessage confidentialMessage,
                                                              PubKey receiverPubKey,
                                                              KeyPair senderKeyPair) {
        if (dataService.isEmpty()) {
            log.warn("We have not stored the mailboxMessage because the dataService is not present.");
            return new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED).setErrorMsg("We have not stored the mailboxMessage because the dataService is not present.");
        }

        MailboxData mailboxData = new MailboxData(confidentialMessage, metaData);
        // We do not wait for the broadcast result as that can take a while. We pack the future into our result, 
        // so clients can react on it as they wish.
        BroadcastResult mailboxFuture = dataService.get().addMailboxData(mailboxData,
                        senderKeyPair,
                        receiverPubKey.getPublicKey())
                .join(); // TODO (refactor, low prio) async for creating the stores, could be made blocking
        return new SendConfidentialMessageResult(MessageDeliveryStatus.TRY_ADD_TO_MAILBOX).setMailboxFuture(mailboxFuture);
    }

    private ConfidentialMessage getConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage, PubKey receiverPubKey, KeyPair senderKeyPair) {
        try {
            ConfidentialData confidentialData = HybridEncryption.encryptAndSign(envelopePayloadMessage.serialize(), receiverPubKey.getPublicKey(), senderKeyPair);
            return new ConfidentialMessage(confidentialData, receiverPubKey.getKeyId());
        } catch (GeneralSecurityException e) {
            log.error("HybridEncryption.encryptAndSign failed at getConfidentialMessage.", e);
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Boolean> processConfidentialMessage(ConfidentialMessage confidentialMessage) {
        return keyBundleService.findKeyPair(confidentialMessage.getReceiverKeyId())
                .map(receiversKeyPair -> supplyAsync(() -> {
                    try {
                        log.info("Found a matching key for processing confidentialMessage");
                        ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
                        byte[] decryptedBytes = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                        bisq.network.protobuf.EnvelopePayloadMessage decryptedProto = bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(decryptedBytes);
                        EnvelopePayloadMessage decryptedEnvelopePayloadMessage = EnvelopePayloadMessage.fromProto(decryptedProto);
                        PublicKey senderPublicKey = KeyGeneration.generatePublic(confidentialData.getSenderPublicKey());
                        log.info("Decrypted confidentialMessage");
                        runAsync(() -> {
                            listeners.forEach(listener -> {
                                try {
                                    listener.onMessage(decryptedEnvelopePayloadMessage);
                                    listener.onConfidentialMessage(decryptedEnvelopePayloadMessage, senderPublicKey);
                                } catch (Exception e) {
                                    // Catch the exception to avoid to break the iteration. We want to continue to notify all listeners.
                                    log.error("listener.onMessage failed at listener {}", listener);
                                }
                            });
                        }, DISPATCHER);
                        return true;
                    } catch (Exception e) {
                        log.error("Error at decryption using receiversKeyId={}", confidentialMessage.getReceiverKeyId(), e);
                        throw new RuntimeException(e);
                    }
                }, ExecutorFactory.WORKER_POOL))
                .orElse(CompletableFuture.completedFuture(false)); // We don't have a key for that receiverKeyId
    }
}
