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

import bisq.common.network.Address;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.CompletableFutureUtils;
import bisq.network.NetworkExecutors;
import bisq.network.StartupReplay;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.ReceiverPublicKeyProvidingPayload;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.NodesById;
import bisq.network.p2p.services.confidential.ack.AckRequestingMessage;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.network.p2p.services.confidential.resend.ResendMessageData;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Slf4j
public class ConfidentialMessageService implements Node.Listener, DataService.Listener {
    private static final ExecutorService EXECUTOR = ExecutorFactory.newSingleThreadExecutor("ConfidentialMessageService");

    /**
     * A decrypted message.
     *
     * @param receiverKeyId The key ID of our key which decrypted the message. The message is processed once for each
     *                      of our keys, so that our identities behave like separate nodes.
     */
    public record EnvelopePayloadMessageAndPublicKey(EnvelopePayloadMessage envelopePayloadMessage,
                                                     PublicKey senderPublicKey,
                                                     String receiverKeyId) {
    }

    // A listener receives only messages which this service decrypted with a key of the node, after it checked the
    // sender key and the receiver key which the message names. The chat, trade and support services rely on this
    // (see docs/specifications/network/confidential-messages.md).
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
    private final Set<EnvelopePayloadMessageAndPublicKey> processedEnvelopePayloadMessagesAndPublicKeys = new CopyOnWriteArraySet<>();
    // Held while a decrypted message is added to the processed messages and passed to the listeners, and while
    // addListenerAndDeliverProcessedMessages adds a listener and copies the processed messages. So such a listener
    // receives each processed message exactly once: in the copy or as a new message.
    private final Object processedMessagesLock = new Object();
    private volatile boolean isShutdownInProgress;

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
        if (isShutdownInProgress) {
            return;
        }

        ExecutorFactory.shutdownAndAwaitTermination(EXECUTOR, 100);

        isShutdownInProgress = true;
        nodesById.removeNodeListener(this);
        dataService.ifPresent(service -> service.removeListener(this));
        listeners.clear();
        processedEnvelopePayloadMessagesAndPublicKeys.clear();
    }


    /* --------------------------------------------------------------------- */
    // Node.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage, Connection connection, NetworkId networkId) {
        if (envelopePayloadMessage instanceof ConfidentialMessage confidentialMessage) {
            processConfidentialMessageAsync(confidentialMessage);
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }


    /* --------------------------------------------------------------------- */
    // DataService.Listener
    /* --------------------------------------------------------------------- */

    @Override
    public void onMailboxDataAdded(MailboxData mailboxData) {
        ConfidentialMessage confidentialMessage = mailboxData.getConfidentialMessage();
        processConfidentialMessageAsync(confidentialMessage)
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
                        log.error("Error at onMailboxDataAdded", throwable);
                    }
                });
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    // We get called from NetworkService on the NetworkNodeExecutor thread
    public SendConfidentialMessageResult send(EnvelopePayloadMessage envelopePayloadMessage,
                                              NetworkId receiverNetworkId,
                                              Address address,
                                              PubKey receiverPubKey,
                                              KeyPair senderKeyPair,
                                              NetworkId senderNetworkId) {
        String receiverAddress = receiverNetworkId.getAddresses();
        long start = System.currentTimeMillis();
        try {
            // In case the node is not yet initialized, we send the message as mailbox messsage for
            // faster delivery. We do not try to create the connection here, as we would expect the node to be
            // initialized first.
            Optional<Node> node = nodesById.findNode(senderNetworkId);
            if (node.isEmpty() || !node.get().isInitialized()) {
                return storeInMailbox(envelopePayloadMessage,
                        receiverPubKey,
                        senderKeyPair,
                        "Node is not created or not initialized yet.",
                        receiverAddress,
                        start);
            }

            // In case the connection is not yet created, we send the message as mailbox messsage for
            // faster delivery.
            Optional<Connection> connection = node.get().findConnection(address);
            if (connection.isEmpty()) {
                // We call getConnection to trigger the creation of the connection but ignore
                // the result with potential exceptions (e.g. if peer is offline).
                nodesById.getOrCreateConnectionAsync(senderNetworkId, address);

                return storeInMailbox(envelopePayloadMessage,
                        receiverPubKey,
                        senderKeyPair,
                        "Connection is not created yet.",
                        receiverAddress,
                        start);
            }

            // Blocking call
            return trySendIfPeerNotOffline(envelopePayloadMessage,
                    address,
                    receiverPubKey,
                    senderKeyPair,
                    senderNetworkId,
                    start,
                    receiverAddress,
                    connection.get());
        } catch (Exception exception) {
            return storeInMailbox(envelopePayloadMessage,
                    receiverPubKey,
                    senderKeyPair,
                    exception.getMessage(),
                    receiverAddress,
                    start);
        }
    }

    private SendConfidentialMessageResult trySendIfPeerNotOffline(EnvelopePayloadMessage envelopePayloadMessage,
                                                                  Address address,
                                                                  PubKey receiverPubKey,
                                                                  KeyPair senderKeyPair,
                                                                  NetworkId senderNetworkId,
                                                                  long start,
                                                                  String receiverAddress,
                                                                  Connection connection) {
        CompletableFuture<Boolean> isPeerOfflineFuture = isPeerOnlineAsync(address, senderNetworkId, start)
                .handle((isPeerOnline, throwable) -> {
                    if (throwable != null) return null;
                    return (isPeerOnline == null || !isPeerOnline) ? true : null;
                })
                .thenCompose(isPeerOffline -> {
                    if (isPeerOffline == null) return new CompletableFuture<>(); // Never complete
                    return CompletableFuture.completedFuture(true);
                });

        CompletableFuture<Optional<SendConfidentialMessageResult>> sendMessageFuture = trySendAsync(envelopePayloadMessage, receiverPubKey, senderKeyPair, senderNetworkId, start,
                receiverAddress, isPeerOfflineFuture, connection)
                .exceptionally(e -> Optional.empty());

        try {
            // The connection timeout is 120 seconds, we add a bit more here as it should never get triggered anyway.
            // We get called from NetworkService on the NetworkNodeExecutor thread, thus the blocking join() call is ok.
            CompletableFuture.anyOf(isPeerOfflineFuture, sendMessageFuture)
                    .orTimeout(150, TimeUnit.SECONDS)
                    .whenComplete((ignore, throwable) -> {
                        if (throwable instanceof TimeoutException timeoutException) {
                            log.error("Neither isPeerOffline resulted in a true result nor we got a connection created in 150 seconds. receiverAddress={}", receiverAddress);
                        }
                    })
                    .join();

            boolean peerDetectedOffline = Boolean.TRUE.equals(isPeerOfflineFuture.getNow(false));
            if (peerDetectedOffline) {
                // We got the result that the peer's onion service is not published in the tor network, thus it is likely that the peer is offline.
                // It could be though the case that the connection creation running in parallel succeeds, and even we continue with sending a mailbox message
                // the normal message sending succeeded. The peer would then get the message received 2 times, which does not cause harm.
                // The result we return to the caller though contains the mailbox result. When the peer gets the ACK message the delivery state gets cleaned up.
                throw new RuntimeException("peerDetectedOffline. receiverAddress=" + receiverAddress);
            } else if (sendMessageFuture.isDone()) {
                Optional<SendConfidentialMessageResult> result = sendMessageFuture.join();
                if (result.isPresent()) {
                    // The countDownLatch was triggered by the getConnectionFuture's result.
                    // It can be either sentResult or storeMailBoxMessageResult
                    SendConfidentialMessageResult messageResult = result.get();
                    handleResult(envelopePayloadMessage, messageResult);
                    return messageResult;
                } else {
                    throw new RuntimeException("Could not send message. Result from trySendAsync was empty.");
                }
            } else {
                throw new RuntimeException("Unexpected case: Peer not detected offline but no result from trySendAsync available.");
            }
        } finally {
            // Make sure that the isPeerOfflineFuture is terminated in case we got a never completing future.
            isPeerOfflineFuture.cancel(true);
        }
    }

    private CompletableFuture<Boolean> isPeerOnlineAsync(Address address,
                                                         NetworkId senderNetworkId,
                                                         long start) {
        return nodesById.isPeerOnlineAsync(senderNetworkId, address)
                .whenComplete((isPeerOnline, throwable) -> {
                    // Can take about 3-5 sec.
                    if (throwable != null) {
                        log.error("Requesting online status of peer failed", throwable);
                        return;
                    }

                    if (isPeerOnline == null || !isPeerOnline) {
                        log.info("Peer is detected as offline. We store the message as mailbox message. Request for isPeerOnline completed after {} ms",
                                System.currentTimeMillis() - start);
                    } else {
                        log.info("Peer is not detected offline. We try to send the message. Request for isPeerOnline completed after {} ms",
                                System.currentTimeMillis() - start);
                    }
                });
    }

    private CompletableFuture<Optional<SendConfidentialMessageResult>> trySendAsync(EnvelopePayloadMessage envelopePayloadMessage,
                                                                                    PubKey receiverPubKey,
                                                                                    KeyPair senderKeyPair,
                                                                                    NetworkId senderNetworkId,
                                                                                    long start,
                                                                                    String receiverAddress,
                                                                                    CompletableFuture<Boolean> isPeerOfflineFuture,
                                                                                    Connection connection) {
        // The work for encryption is left to the caller thread.
        ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
        return nodesById.sendAsync(senderNetworkId, confidentialMessage, connection)
                .handle((con, throwable) -> {
                    if (throwable == null) {
                        log.info("Sent message to {} after {} ms", receiverAddress, System.currentTimeMillis() - start);
                        SendConfidentialMessageResult sentResult = new SendConfidentialMessageResult(MessageDeliveryStatus.SENT);
                        boolean detectedOffLine = isPeerOfflineFuture.isDone() && isPeerOfflineFuture.join();
                        if (detectedOffLine) {
                            log.info("We had detected that the peer is offline, but we succeeded to create a connection and send the message. receiverAddress={}", receiverAddress);
                        }
                        return Optional.of(sentResult);
                    } else if (!isShutdownInProgress) {
                        log.info("Creating confidentialMessage failed");
                        var exception = throwable instanceof Exception e ? e : new RuntimeException(throwable);
                        return maybeStoreInMailbox(envelopePayloadMessage, receiverPubKey, senderKeyPair, exception, isPeerOfflineFuture, confidentialMessage, receiverAddress, start);
                    } else {
                        return Optional.empty();
                    }
                });
    }


    private Optional<SendConfidentialMessageResult> maybeStoreInMailbox(EnvelopePayloadMessage envelopePayloadMessage,
                                                                        PubKey receiverPubKey,
                                                                        KeyPair senderKeyPair,
                                                                        Exception exception,
                                                                        CompletableFuture<Boolean> isPeerOfflineFuture,
                                                                        ConfidentialMessage confidentialMessage,
                                                                        String receiverAddress,
                                                                        long start) {
        if (isShutdownInProgress) {
            // We have stored in mailbox when shutdown started. The pending message can be ignored.
            return Optional.of(new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED));
        }
        boolean detectedOffLine = isPeerOfflineFuture.isDone() && isPeerOfflineFuture.join();
        if (detectedOffLine) {
            log.info("We had detected that the peer is offline, but we succeeded to create a connection but failed sending the message. " +
                    "As we already stored the message to mailbox from the offline detection we ignore that case. receiverAddress={}", receiverAddress);
            return Optional.empty();
        } else {
            SendConfidentialMessageResult result = storeInMailbox(envelopePayloadMessage,
                    receiverPubKey,
                    senderKeyPair,
                    exception.getMessage(),
                    confidentialMessage);
            log.info("Stored message to mailbox {} after {} ms", receiverAddress, System.currentTimeMillis() - start);
            return Optional.of(result);
        }
    }

    public Optional<SendConfidentialMessageResult> flushPendingMessagesToMailboxAtShutdown(ResendMessageData pendingMessage,
                                                                                           KeyPair senderKeyPair) {
        EnvelopePayloadMessage envelopePayloadMessage = pendingMessage.getEnvelopePayloadMessage();
        if (envelopePayloadMessage instanceof MailboxMessage) {
            PubKey receiverPubKey = pendingMessage.getReceiverNetworkId().getPubKey();
            ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
            MetaData metaData = ((MailboxMessage) envelopePayloadMessage).getMetaData();
            Optional<SendConfidentialMessageResult> sendConfidentialMessageResult = Optional.of(storeMailBoxMessage(metaData, confidentialMessage, receiverPubKey, senderKeyPair));
            sendConfidentialMessageResult.ifPresent(result -> handleResult(envelopePayloadMessage, result));
            return sendConfidentialMessageResult;
        } else {
            return Optional.empty();
        }
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Adds the listener and passes to it the messages which this service processed before, in the order in which it
     * processed them. Each message goes to onMessage and to onConfidentialMessage, as a new message does. Together with
     * the new messages, the listener receives each processed message exactly once. The processed messages are passed
     * in the thread of the caller. Each call is processed on its own, as in StartupReplay: an exception of the listener
     * is logged, and the other calls still happen. If the listener was added before, nothing happens.
     * See docs/specifications/network/startup-replay.md.
     */
    public void addListenerAndDeliverProcessedMessages(Listener listener) {
        List<EnvelopePayloadMessageAndPublicKey> processedMessages;
        synchronized (processedMessagesLock) {
            if (!listeners.add(listener)) {
                return;
            }
            // A list: a hash-based copy would call hashCode of each received message, which can throw
            processedMessages = new ArrayList<>(processedEnvelopePayloadMessagesAndPublicKeys);
        }
        processedMessages.forEach(processedMessage -> deliverProcessedMessage(listener, processedMessage));
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    // Services which start use NetworkService.addConfidentialMessageListenerAndDeliverProcessedMessages instead of
    // this copy. The copy is hash-based, so a message whose hashCode throws makes it fail. A message which the node
    // processes between this copy and the registration of a listener is lost for the listener or reaches it twice.
    public Set<EnvelopePayloadMessage> getProcessedEnvelopePayloadMessages() {
        return processedEnvelopePayloadMessagesAndPublicKeys.stream()
                .map(EnvelopePayloadMessageAndPublicKey::envelopePayloadMessage)
                .collect(Collectors.toUnmodifiableSet());
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private SendConfidentialMessageResult storeInMailbox(EnvelopePayloadMessage envelopePayloadMessage,
                                                         PubKey receiverPubKey,
                                                         KeyPair senderKeyPair,
                                                         String reason,
                                                         String receiverAddress,
                                                         long start) {
        if (isShutdownInProgress) {
            // We have stored in mailbox when shutdown started. The pending message can be ignored.
            return new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED);
        }
        // If peer is detected offline, or we got a ConnectionException we store in mailbox
        ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
        SendConfidentialMessageResult result = storeInMailbox(envelopePayloadMessage, receiverPubKey, senderKeyPair, reason, confidentialMessage);
        log.info("Stored message to {} in mailbox after {} ms", receiverAddress, System.currentTimeMillis() - start);

        handleResult(envelopePayloadMessage, result);
        return result;
    }

    private SendConfidentialMessageResult storeInMailbox(EnvelopePayloadMessage envelopePayloadMessage,
                                                         PubKey receiverPubKey,
                                                         KeyPair senderKeyPair,
                                                         String reason,
                                                         ConfidentialMessage confidentialMessage) {
        SendConfidentialMessageResult result;
        if (isShutdownInProgress) {
            log.warn("We started already the shutdown process when storeInMailbox is called and ignore that message. Message {}", envelopePayloadMessage);
            return new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED);
        }
        if (envelopePayloadMessage instanceof MailboxMessage) {
            log.info("Message could not be sent because of {}.\n" +
                    "We send the message as mailbox message.", reason);
            result = storeMailBoxMessage(((MailboxMessage) envelopePayloadMessage).getMetaData(),
                    confidentialMessage, receiverPubKey, senderKeyPair);
        } else {
            log.warn("Sending message failed and message is not type of MailboxMessage. message={}", envelopePayloadMessage);
            result = new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED).setErrorMsg("Sending message failed and message is not type of MailboxMessage. Reason=" + reason);
        }
        return result;
    }

    private void handleResult(EnvelopePayloadMessage envelopePayloadMessage, SendConfidentialMessageResult result) {
        if (envelopePayloadMessage instanceof AckRequestingMessage) {
            messageDeliveryStatusService.ifPresent(service -> {
                String ackRequestingMessageId = ((AckRequestingMessage) envelopePayloadMessage).getAckRequestingMessageId();
                service.applyMessageDeliveryStatus(ackRequestingMessageId, result.getMessageDeliveryStatus());

                // If we tried to store in mailbox we check if at least one successful broadcast happened
                if (result.getMessageDeliveryStatus() == MessageDeliveryStatus.TRY_ADD_TO_MAILBOX) {
                    CompletableFutureUtils.anyOf(result.getMailboxFuture().orElseThrow())
                            .whenComplete((broadcastResult, throwable) -> {
                                if (throwable != null || broadcastResult.getNumSuccess() == 0) {
                                    log.warn("mailboxFuture completed and resulted in MessageDeliveryStatus.FAILED");
                                    service.applyMessageDeliveryStatus(ackRequestingMessageId, MessageDeliveryStatus.FAILED);
                                } else {
                                    log.info("mailboxFuture completed and resulted in MessageDeliveryStatus.ADDED_TO_MAILBOX");
                                    service.applyMessageDeliveryStatus(ackRequestingMessageId, MessageDeliveryStatus.ADDED_TO_MAILBOX);
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

        MailboxData mailboxData = new MailboxData(metaData, confidentialMessage);
        // We do not wait for the broadcast result as that can take a while. We pack the future into our result,
        // so clients can react on it as they wish.
        PublicKey publicKey = receiverPubKey.getPublicKey();

        // TODO (refactor, low prio) async for creating the stores, could be made blocking
        BroadcastResult mailboxFuture = dataService.get().addMailboxData(mailboxData, senderKeyPair, publicKey).join();
        log.info("Try to store confidentialMessage to mailbox. ReceiverKeyId={}", confidentialMessage.getReceiverKeyId());
        return new SendConfidentialMessageResult(MessageDeliveryStatus.TRY_ADD_TO_MAILBOX).setMailboxFuture(mailboxFuture);
    }

    private ConfidentialMessage getConfidentialMessage(EnvelopePayloadMessage envelopePayloadMessage,
                                                       PubKey receiverPubKey,
                                                       KeyPair senderKeyPair) {
        try {
            ConfidentialData confidentialData = HybridEncryption.encryptAndSign(envelopePayloadMessage.serialize(), receiverPubKey.getPublicKey(), senderKeyPair);
            return new ConfidentialMessage(confidentialData, receiverPubKey.getKeyId());
        } catch (GeneralSecurityException e) {
            log.error("HybridEncryption.encryptAndSign failed at getConfidentialMessage.", e);
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<Boolean> processConfidentialMessageAsync(ConfidentialMessage confidentialMessage) {
        return keyBundleService.findKeyPair(confidentialMessage.getReceiverKeyId())
                .map(receiversKeyPair -> supplyAsync(() -> process(confidentialMessage, receiversKeyPair), EXECUTOR))
                .orElse(CompletableFuture.completedFuture(false)); // We don't have a key for that receiverKeyId
    }

    private Boolean process(ConfidentialMessage confidentialMessage, KeyPair receiversKeyPair) {
        try {
            log.info("Found a matching key for processing confidentialMessage. ReceiverKeyId={}", confidentialMessage.getReceiverKeyId());
            ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
            byte[] decryptedBytes = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
            bisq.network.protobuf.EnvelopePayloadMessage decryptedProto = bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(decryptedBytes);
            EnvelopePayloadMessage decryptedEnvelopePayloadMessage = EnvelopePayloadMessage.fromProto(decryptedProto);

            // For backward compatibility we send 2 versions of mailbox data, thus we will receive each
            // mailbox data 2 times. We do not want that client code need to deal with duplications,
            // thus we filter here out the duplicated message.
            byte[] encodedSenderPublicKey = confidentialData.getSenderPublicKey();

            if (decryptedEnvelopePayloadMessage instanceof SenderPublicKeyProvidingPayload pubKeyProvidingMessage) {
                verifySenderPublicKeyBinding(pubKeyProvidingMessage, encodedSenderPublicKey);
            }
            if (decryptedEnvelopePayloadMessage instanceof ReceiverPublicKeyProvidingPayload pubKeyProvidingMessage) {
                verifyReceiverPublicKeyBinding(pubKeyProvidingMessage, receiversKeyPair.getPublic());
            }

            PublicKey senderPublicKey = KeyGeneration.generatePublic(encodedSenderPublicKey);
            // The decryption succeeded, so the key of this key ID is ours and the message was encrypted for it
            String receiverKeyId = confidentialMessage.getReceiverKeyId();
            addAndNotifyListeners(
                    new EnvelopePayloadMessageAndPublicKey(decryptedEnvelopePayloadMessage, senderPublicKey, receiverKeyId));
            return true;
        } catch (Exception e) {
            log.error("Error at decryption using receiversKeyId={}", confidentialMessage.getReceiverKeyId(), e);
            throw new RuntimeException(e);
        }
    }

    private void addAndNotifyListeners(EnvelopePayloadMessageAndPublicKey processedMessage) {
        synchronized (processedMessagesLock) {
            boolean wasNotPresent = processedEnvelopePayloadMessagesAndPublicKeys.add(processedMessage);
            if (wasNotPresent) {
                EnvelopePayloadMessage message = processedMessage.envelopePayloadMessage();
                PublicKey senderPublicKey = processedMessage.senderPublicKey();
                log.info("Decrypted confidentialMessage. decryptedEnvelopePayloadMessage={}", message.getClass().getSimpleName());
                messageDeliveryStatusService.ifPresent(service -> NetworkExecutors.getNotifyExecutor()
                        .submit(() -> service.onDecryptedMessage(message, processedMessage.receiverKeyId())));
                listeners.forEach(listener -> {
                    NetworkExecutors.getNotifyExecutor().submit(() -> listener.onMessage(message));
                    NetworkExecutors.getNotifyExecutor().submit(() -> listener.onConfidentialMessage(message, senderPublicKey));
                });
            }
        }
    }

    // A new message reaches each of the two methods in a task of its own, so each call is processed on its own here as
    // well. StartupReplay logs a failed call with the type of the message, as for the other replayed data.
    private static void deliverProcessedMessage(Listener listener, EnvelopePayloadMessageAndPublicKey processedMessage) {
        EnvelopePayloadMessage message = processedMessage.envelopePayloadMessage();
        PublicKey senderPublicKey = processedMessage.senderPublicKey();
        StartupReplay.forEach(Stream.of(message), listener::onMessage);
        StartupReplay.forEach(Stream.of(message),
                replayedMessage -> listener.onConfidentialMessage(replayedMessage, senderPublicKey));
    }

    static void verifySenderPublicKeyBinding(SenderPublicKeyProvidingPayload pubKeyProvidingMessage,
                                             byte[] encodedSenderPublicKey) {
        checkArgument(Arrays.equals(pubKeyProvidingMessage.getSenderPublicKey().getEncoded(), encodedSenderPublicKey),
                "Public key of decrypted pubKeyProvidingMessage and senderPublicKey do not match.");
    }

    static void verifyReceiverPublicKeyBinding(ReceiverPublicKeyProvidingPayload pubKeyProvidingMessage,
                                               PublicKey receiverPublicKey) {
        checkArgument(Arrays.equals(pubKeyProvidingMessage.getReceiverPublicKey().getEncoded(), receiverPublicKey.getEncoded()),
                "Public key of decrypted pubKeyProvidingMessage and receiverPublicKey do not match.");
    }
}
