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

import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkIdService;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.security.keys.KeyBundleService;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This service aggregates the message delivery status for all supported transports and provides
 * a map with the message ID as key and an observable MessageDeliveryStatus as value which holds the most
 * relevant delivery state of all transports.
 * It sends the AckMessage for each AckRequestingMessage which a ConfidentialMessageService decrypted, and applies the
 * received AckMessages to the delivery status of our own messages.
 * This service is only enabled if the ServiceNode.Service.ACK and the ServiceNode.Service.CONFIDENTIAL are set in the
 * config. See docs/specifications/network/message-acknowledgement.md.
 */
@Slf4j
@Getter
public class MessageDeliveryStatusService extends RateLimitedPersistenceClient<MessageDeliveryStatusStore> {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(30);
    // The set only prevents a second AckMessage for a message which arrives twice, for example over two transports.
    // A second AckMessage is harmless, so the set keeps only the most recent entries. 10 000 entries cover far more
    // messages than a node receives between two copies of one message, and need about 3 MB at most.
    @VisibleForTesting
    static final int MAX_ACKED_MESSAGES = 10_000;

    private final MessageDeliveryStatusStore persistableStore = new MessageDeliveryStatusStore();
    private final Persistence<MessageDeliveryStatusStore> persistence;
    private final KeyBundleService keyBundleService;
    private final NetworkIdService networkIdService;
    private final NetworkService networkService;
    // Insertion order is the order in which we sent the AckMessages. All access is guarded by the monitor of the set.
    @Getter(AccessLevel.NONE)
    private final Set<AckedMessage> ackedMessages = new LinkedHashSet<>();
    @Nullable
    private Scheduler checkPendingScheduler;
    private volatile boolean isShutdown;

    // Each of our identities acknowledges a message on its own, as a separate node would do. Otherwise a peer could
    // learn from the number of AckMessages whether two user profiles belong to the same node.
    private record AckedMessage(String receiverKeyId, String messageId) {
    }

    public MessageDeliveryStatusService(PersistenceService persistenceService,
                                        KeyBundleService keyBundleService,
                                        NetworkIdService networkIdService,
                                        NetworkService networkService) {
        this.keyBundleService = keyBundleService;
        this.networkIdService = networkIdService;
        this.networkService = networkService;

        // As ResendMessageService depends on our data store we use also here the PRIVATE directory.
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
    }

    @Override
    public MessageDeliveryStatusStore prunePersisted(MessageDeliveryStatusStore persisted) {
        long cutOffDate = System.currentTimeMillis() - MAX_AGE;
        Map<String, Long> creationDateByMessageId = new HashMap<>(persisted.getCreationDateByMessageId());
        Map<String, Observable<MessageDeliveryStatus>> prunedMessageDeliveryStatusByMessageId = persisted.getMessageDeliveryStatusByMessageId().entrySet().stream()
                .filter(e -> creationDateByMessageId.containsKey(e.getKey()) && creationDateByMessageId.get(e.getKey()) > cutOffDate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, Long> prunedCreationDateByMessageId = persisted.getCreationDateByMessageId().entrySet().stream()
                .filter(e -> creationDateByMessageId.containsKey(e.getKey()) && creationDateByMessageId.get(e.getKey()) > cutOffDate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new MessageDeliveryStatusStore(prunedMessageDeliveryStatusByMessageId, prunedCreationDateByMessageId);
    }

    public void initialize() {
        checkPendingScheduler = Scheduler.run(this::checkPending)
                .host(this)
                .runnableName("checkPending")
                .after(1000);
    }

    public void shutdown() {
        isShutdown = true;
        if (checkPendingScheduler != null) {
            checkPendingScheduler.stop();
            checkPendingScheduler = null;
        }
        synchronized (ackedMessages) {
            ackedMessages.clear();
        }
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    /**
     * The ConfidentialMessageService calls this method for each message which it decrypted.
     *
     * @param receiverKeyId The key ID of our key which decrypted the message. We send the AckMessage from our own
     *                      NetworkId for this key. The receiver which the message names is data of the peer, so we do
     *                      not use it as our identity.
     */
    public void onDecryptedMessage(EnvelopePayloadMessage envelopePayloadMessage, String receiverKeyId) {
        if (isShutdown) {
            return;
        }
        if (envelopePayloadMessage instanceof AckRequestingMessage ackRequestingMessage) {
            processAckRequestingMessage(ackRequestingMessage, receiverKeyId);
        } else if (envelopePayloadMessage instanceof AckMessage ackMessage) {
            processAckMessage(ackMessage);
        }
    }

    public void applyMessageDeliveryStatus(String ackRequestingMessageId, MessageDeliveryStatus status) {
        if (ackRequestingMessageId == null) {
            log.warn("ackRequestingMessageId must not be null");
            return;
        }
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            if (messageDeliveryStatusByMessageId.containsKey(ackRequestingMessageId)) {
                Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(ackRequestingMessageId);
                // If we have already a received state we return.
                // This ensures that a later received failed state from one transport does not overwrite the received state from
                // another transport.
                if (observableStatus.get().isReceived()) {
                    return;
                }
                observableStatus.set(status);
            } else {
                persistableStore.getCreationDateByMessageId().putIfAbsent(ackRequestingMessageId, System.currentTimeMillis());
                messageDeliveryStatusByMessageId.put(ackRequestingMessageId, new Observable<>(status));
            }
        }
        log.info("Persist MessageDeliveryStatus {} with ackRequestingMessageId {}",
                messageDeliveryStatusByMessageId.get(ackRequestingMessageId).get(), ackRequestingMessageId);
        persist();
    }

    public ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return persistableStore.getMessageDeliveryStatusByMessageId();
    }

    @VisibleForTesting
    int getNumAckedMessages() {
        synchronized (ackedMessages) {
            return ackedMessages.size();
        }
    }


    /* --------------------------------------------------------------------- */
    // Private
    /* --------------------------------------------------------------------- */

    private void processAckMessage(AckMessage ackMessage) {
        String messageId = ackMessage.getId();
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(messageId);
            if (observableStatus == null) {
                // We apply a status to our own message before we send it (see NetworkService.confidentialSend). An
                // AckMessage for an unknown ID is not for a message which we sent in the last 30 days, so it must not
                // create state.
                log.debug("We ignore an AckMessage for a message ID which we do not know");
                return;
            }

            // If we have already a received state we return.
            // This ensures that a later received failed state from one transport does not overwrite the received state from
            // another transport.
            if (observableStatus.get().isReceived()) {
                return;
            }

            if (observableStatus.get() == MessageDeliveryStatus.CONNECTING ||
                    observableStatus.get() == MessageDeliveryStatus.SENT) {
                observableStatus.set(MessageDeliveryStatus.ACK_RECEIVED);
            } else {
                // Covers ADDED_TO_MAILBOX, TRY_ADD_TO_MAILBOX and FAILED
                observableStatus.set(MessageDeliveryStatus.MAILBOX_MSG_RECEIVED);
            }
            log.info("Received AckMessage for message with ID {} and set status to {}",
                    messageId, observableStatus.get());
            persist();
        }
    }

    private void processAckRequestingMessage(AckRequestingMessage message, String receiverKeyId) {
        Optional<NetworkId> myNetworkId = networkIdService.findNetworkIdByKeyId(receiverKeyId);
        Optional<KeyPair> myKeyPair = keyBundleService.findKeyPair(receiverKeyId);
        if (myNetworkId.isEmpty() || myKeyPair.isEmpty()) {
            log.warn("We do not send an AckMessage for a {} because the key which decrypted it belongs to none of our " +
                    "network identities", message.getClass().getSimpleName());
            return;
        }

        String messageId = message.getAckRequestingMessageId();
        if (!addAckedMessage(new AckedMessage(receiverKeyId, messageId))) {
            log.debug("We have already sent the AckMessage for that {}", message.getClass().getSimpleName());
            return;
        }

        log.info("Received a {} with message ID {}", message.getClass().getSimpleName(), messageId);
        NetworkIdWithKeyPair myNetworkIdWithKeyPair = new NetworkIdWithKeyPair(myNetworkId.get(), myKeyPair.get());
        networkService.confidentialSend(new AckMessage(messageId), message.getSender(), myNetworkIdWithKeyPair);
    }

    // Returns false if we have sent the AckMessage for that message already
    private boolean addAckedMessage(AckedMessage ackedMessage) {
        synchronized (ackedMessages) {
            if (!ackedMessages.add(ackedMessage)) {
                return false;
            }
            if (ackedMessages.size() > MAX_ACKED_MESSAGES) {
                Iterator<AckedMessage> iterator = ackedMessages.iterator();
                iterator.next();
                iterator.remove();
            }
            return true;
        }
    }

    private void checkPending() {
        Set<Map.Entry<String, Observable<MessageDeliveryStatus>>> pendingItemsSnapshot;
        var map = getMessageDeliveryStatusByMessageId();
        synchronized (map) {
            pendingItemsSnapshot = map.entrySet().stream()
                    .filter(e -> e.getValue().get() == MessageDeliveryStatus.CONNECTING
                            || e.getValue().get() == MessageDeliveryStatus.SENT
                            || e.getValue().get() == MessageDeliveryStatus.TRY_ADD_TO_MAILBOX)
                    .collect(Collectors.toSet());
        }
        if (!pendingItemsSnapshot.isEmpty()) {
            log.warn("We have pending messages which have not been successfully sent. pendingItems={}", pendingItemsSnapshot);
            pendingItemsSnapshot.forEach(e -> map.get(e.getKey()).set(MessageDeliveryStatus.FAILED));
            persist();
        }
    }
}
