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
import bisq.common.threading.ThreadName;
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.network.NetworkService.*;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.*;

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
    private final Set<EnvelopePayloadMessage> processedEnvelopePayloadMessages = new HashSet<>();
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
        isShutdownInProgress = true;
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
                        log.error("Error at onMailboxDataAdded", throwable);
                    }
                });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public SendConfidentialMessageResult send(EnvelopePayloadMessage envelopePayloadMessage,
                                              NetworkId receiverNetworkId,
                                              Address address,
                                              PubKey receiverPubKey,
                                              KeyPair senderKeyPair,
                                              NetworkId senderNetworkId) {
        SendConfidentialMessageResult result;
        String receiverAddress = receiverNetworkId.getAddresses();
        long start = System.currentTimeMillis();

        // Node gets initialized at higher level services
        nodesById.assertNodeIsInitialized(senderNetworkId);

        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicBoolean peerDetectedOffline = new AtomicBoolean();
            runAsync(() -> {
                ThreadName.set(this, "isPeerOnline");
                // Takes about 3-5 sec.
                boolean isPeerOnline = nodesById.isPeerOnline(senderNetworkId, address);
                if (!isPeerOnline) {
                    log.info("Peer is detected as offline. We store the message as mailbox message. Request for isPeerOnline completed after {} ms",
                            System.currentTimeMillis() - start);
                    peerDetectedOffline.set(true);
                    if (countDownLatch.getCount() > 0) {
                        countDownLatch.countDown();
                    }
                } else {
                    log.info("Peer is not detected offline. We wait for the connection creation has been successful and try to send the message. " +
                                    "Request for isPeerOnline completed after {} ms",
                            System.currentTimeMillis() - start);
                }
            }, NETWORK_IO_POOL);

            AtomicReference<SendConfidentialMessageResult> altResult = new AtomicReference<>();
            runAsync(() -> {
                ThreadName.set(this, "send");
                try {
                    Connection connection = nodesById.getConnection(senderNetworkId, address);
                    log.info("Creating connection to {} took {} ms", receiverAddress, System.currentTimeMillis() - start);
                    // We got a valid connection and try to send the message. If send fails we store in mailbox in case envelopePayloadMessage is a MailboxMessage
                    ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
                    try {
                        nodesById.send(senderNetworkId, confidentialMessage, connection);
                        log.info("Sent message to {} after {} ms", receiverAddress, System.currentTimeMillis() - start);
                        SendConfidentialMessageResult sentResult = new SendConfidentialMessageResult(MessageDeliveryStatus.SENT);
                        altResult.set(sentResult);

                        if (countDownLatch.getCount() == 0) {
                            log.info("We had detected that the peer is offline, but we succeeded to create a connection and send the message. receiverAddress={}", receiverAddress);
                        }
                    } catch (Exception exception) {
                        if (isShutdownInProgress) {
                            // We have stored in mailbox when shutdown started. The pending message can be ignored.
                            altResult.set(new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED));
                        }
                        if (countDownLatch.getCount() == 1) {
                            SendConfidentialMessageResult storeMailBoxMessageResult = storeInMailbox(envelopePayloadMessage, receiverPubKey, senderKeyPair, exception, confidentialMessage);
                            log.info("Stored message to mailbox {} after {} ms", receiverAddress, System.currentTimeMillis() - start);
                            altResult.set(storeMailBoxMessageResult);
                        } else {
                            log.info("We had detected that the peer is offline, but we succeeded to create a connection but failed sending the message. " +
                                    "As we already stored the message to mailbox from the offline detection we ignore that case. receiverAddress={}", receiverAddress);
                        }
                    }
                } catch (Exception exception) {
                    if (!isShutdownInProgress) {
                        log.info("Creating connection to {} failed. peerDetectedOffline={}", receiverAddress, peerDetectedOffline.get());
                    }
                }
                if (countDownLatch.getCount() > 0) {
                    countDownLatch.countDown();
                }
            }, NETWORK_IO_POOL);

            // The connection timeout is 120 seconds, we add a bit more here as it should never get triggered anyway.
            boolean notTimedOut = countDownLatch.await(150, TimeUnit.SECONDS);
            checkArgument(notTimedOut, "Neither isPeerOffline resulted in a true result nor we got a connection created in 150 seconds. receiverAddress=" + receiverAddress);

            if (peerDetectedOffline.get()) {
                // We got the result that the peer's onionservice is not published in the tor network, thus it is likely that the peer is offline.
                // It could be though the case that the connection creation running in parallel succeeds, and even we continue with sending a mailbox message
                // the normal message sending succeeded. The peer would then get the message receive 2 times, which does not cause harm.
                // The result we return to the caller though contains the mailbox result. When the peer gets the ACK message the delivery state gets cleaned up.
                throw new RuntimeException("peerDetectedOffline. receiverAddress=" + receiverAddress);
            } else if (altResult.get() != null) {
                // The countDownLatch was triggered by the getConnectionFuture's result.
                // It can be either sentResult or storeMailBoxMessageResult
                result = altResult.get();
            } else {
                throw new RuntimeException("Could not create connection. receiverAddress=" + receiverAddress);
            }
        } catch (Exception exception) {
            if (isShutdownInProgress) {
                // We have stored in mailbox when shutdown started. The pending message can be ignored.
                return new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED);
            }
            // If peer is detected offline, or we got a ConnectionException we store in mailbox
            ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
            result = storeInMailbox(envelopePayloadMessage, receiverPubKey, senderKeyPair, exception, confidentialMessage);
            log.info("Stored message to {} in mailbox after {} ms", receiverAddress, System.currentTimeMillis() - start);
        }

        handleResult(envelopePayloadMessage, result);
        return result;

    }

    public Optional<SendConfidentialMessageResult> flushPendingMessagesToMailboxAtShutdown(ResendMessageData pendingMessage,
                                                                                           KeyPair senderKeyPair) {
        EnvelopePayloadMessage envelopePayloadMessage = pendingMessage.getEnvelopePayloadMessage();
        if (envelopePayloadMessage instanceof MailboxMessage) {
            PubKey receiverPubKey = pendingMessage.getReceiverNetworkId().getPubKey();
            ConfidentialMessage confidentialMessage = getConfidentialMessage(envelopePayloadMessage, receiverPubKey, senderKeyPair);
            Optional<SendConfidentialMessageResult> sendConfidentialMessageResult = Optional.of(storeMailBoxMessage(((MailboxMessage) envelopePayloadMessage).getMetaData(),
                    confidentialMessage, receiverPubKey, senderKeyPair));
            sendConfidentialMessageResult.ifPresent(result -> handleResult(envelopePayloadMessage, result));
            return sendConfidentialMessageResult;
        } else {
            return Optional.empty();
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

    private SendConfidentialMessageResult storeInMailbox(EnvelopePayloadMessage envelopePayloadMessage,
                                                         PubKey receiverPubKey,
                                                         KeyPair senderKeyPair,
                                                         Exception exception,
                                                         ConfidentialMessage confidentialMessage) {
        SendConfidentialMessageResult result;
        if (isShutdownInProgress) {
            log.warn("We started already the shutdown process when storeInMailbox is called and ignore that message. Message {}", envelopePayloadMessage);
            return new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED);
        }
        if (envelopePayloadMessage instanceof MailboxMessage) {
            log.info("Message could not be sent because of {}.\n" +
                    "We send the message as mailbox message.", exception.getMessage());
            result = storeMailBoxMessage(((MailboxMessage) envelopePayloadMessage).getMetaData(),
                    confidentialMessage, receiverPubKey, senderKeyPair);
        } else {
            log.warn("Sending message failed and message is not type of MailboxMessage. message={}", envelopePayloadMessage);
            result = new SendConfidentialMessageResult(MessageDeliveryStatus.FAILED).setErrorMsg("Sending proto failed and proto is not type of MailboxMessage. Exception=" + exception);
        }
        return result;
    }

    private void handleResult(EnvelopePayloadMessage envelopePayloadMessage, SendConfidentialMessageResult result) {
        if (envelopePayloadMessage instanceof AckRequestingMessage) {
            messageDeliveryStatusService.ifPresent(service -> {
                String messageId = ((AckRequestingMessage) envelopePayloadMessage).getId();
                service.applyMessageDeliveryStatus(messageId, result.getMessageDeliveryStatus());

                // If we tried to store in mailbox we check if at least one successful broadcast happened
                if (result.getMessageDeliveryStatus() == MessageDeliveryStatus.TRY_ADD_TO_MAILBOX) {
                    CompletableFutureUtils.anyOf(result.getMailboxFuture().orElseThrow())
                            .whenComplete((broadcastResult, throwable) -> {
                                if (throwable != null || broadcastResult.getNumSuccess() == 0) {
                                    log.warn("mailboxFuture completed and resulted in MessageDeliveryStatus.FAILED");
                                    service.applyMessageDeliveryStatus(messageId, MessageDeliveryStatus.FAILED);
                                } else {
                                    log.info("mailboxFuture completed and resulted in MessageDeliveryStatus.ADDED_TO_MAILBOX");
                                    service.applyMessageDeliveryStatus(messageId, MessageDeliveryStatus.ADDED_TO_MAILBOX);
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

        // Send also with version 0 for backward compatibility
        MailboxData oldVersion = MailboxData.cloneWithVersion0(mailboxData);
        dataService.get().addMailboxData(oldVersion, senderKeyPair, publicKey).join();

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

    private CompletableFuture<Boolean> processConfidentialMessage(ConfidentialMessage confidentialMessage) {
        return keyBundleService.findKeyPair(confidentialMessage.getReceiverKeyId())
                .map(receiversKeyPair -> supplyAsync(() -> {
                    try {
                        log.info("Found a matching key for processing confidentialMessage");
                        ConfidentialData confidentialData = confidentialMessage.getConfidentialData();
                        byte[] decryptedBytes = HybridEncryption.decryptAndVerify(confidentialData, receiversKeyPair);
                        bisq.network.protobuf.EnvelopePayloadMessage decryptedProto = bisq.network.protobuf.EnvelopePayloadMessage.parseFrom(decryptedBytes);
                        EnvelopePayloadMessage decryptedEnvelopePayloadMessage = EnvelopePayloadMessage.fromProto(decryptedProto);

                        // For backward compatibility we send 2 versions of mailbox data, thus we will receive each
                        // mailbox data 2 times. We do not want that client code need to deal with duplications,
                        // thus we filter here out the duplicated message and.
                        boolean wasNotPresent = processedEnvelopePayloadMessages.add(decryptedEnvelopePayloadMessage);
                        if (wasNotPresent) {
                            PublicKey senderPublicKey = KeyGeneration.generatePublic(confidentialData.getSenderPublicKey());
                            log.info("Decrypted confidentialMessage");
                            runAsync(() -> listeners.forEach(listener -> {
                                try {
                                    listener.onMessage(decryptedEnvelopePayloadMessage);
                                    listener.onConfidentialMessage(decryptedEnvelopePayloadMessage, senderPublicKey);
                                } catch (Exception e) {
                                    log.error("Calling onMessage(decryptedEnvelopePayloadMessage, senderPublicKey) at messageListener {} failed", listener, e);
                                }
                            }), DISPATCHER);
                        }
                        return true;
                    } catch (Exception e) {
                        log.error("Error at decryption using receiversKeyId={}", confidentialMessage.getReceiverKeyId(), e);
                        throw new RuntimeException(e);
                    }
                }, ExecutorFactory.WORKER_POOL))
                .orElse(CompletableFuture.completedFuture(false)); // We don't have a key for that receiverKeyId
    }
}
