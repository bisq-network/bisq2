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
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.keys.KeyBundleService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This service aggregates the message delivery status for all supported transports and provides
 * a map with the message ID as key and an observable MessageDeliveryStatus as value which holds the most
 * relevant delivery state of all transports.
 * This service depends on the ConfidentialMessageService is only enabled if the ServiceNode.Service.ACK and
 * the ServiceNode.Service.CONFIDENTIAL are set in the config.
 * <br/>
 */
@Slf4j
@Getter
public class MessageDeliveryStatusService implements PersistenceClient<MessageDeliveryStatusStore>, ConfidentialMessageService.Listener {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(30);

    private final MessageDeliveryStatusStore persistableStore = new MessageDeliveryStatusStore();
    private final Persistence<MessageDeliveryStatusStore> persistence;
    private final KeyBundleService keyBundleService;
    private final NetworkService networkService;
    private final Set<String> ackedMessageIds = new HashSet<>();

    public MessageDeliveryStatusService(PersistenceService persistenceService,
                                        KeyBundleService keyBundleService,
                                        NetworkService networkService) {
        this.keyBundleService = keyBundleService;
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
        Scheduler.run(this::checkPending)
                .host(this)
                .runnableName("checkPending")
                .after(1000);

        networkService.addConfidentialMessageListener(this);
    }

    public void shutdown() {
        networkService.removeConfidentialMessageListener(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // ConfidentialMessageService.Listener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(EnvelopePayloadMessage envelopePayloadMessage) {
        if (envelopePayloadMessage instanceof AckRequestingMessage) {
            processAckRequestingMessage((AckRequestingMessage) envelopePayloadMessage);
        } else if (envelopePayloadMessage instanceof AckMessage) {
            processAckMessage((AckMessage) envelopePayloadMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void applyMessageDeliveryStatus(String messageId, MessageDeliveryStatus status) {
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            if (messageDeliveryStatusByMessageId.containsKey(messageId)) {
                Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(messageId);
                // If we have already a received state we return.
                // This ensures that a later received failed state from one transport does not overwrite the received state from
                // another transport.
                if (observableStatus.get().isReceived()) {
                    return;
                }
                observableStatus.set(status);
            } else {
                persistableStore.getCreationDateByMessageId().putIfAbsent(messageId, System.currentTimeMillis());
                messageDeliveryStatusByMessageId.put(messageId, new Observable<>(status));
            }
            log.info("Persist MessageDeliveryStatus {} with message ID {}",
                    messageDeliveryStatusByMessageId.get(messageId).get(), messageId);
            persist();
        }
    }

    public ObservableHashMap<String, Observable<MessageDeliveryStatus>> getMessageDeliveryStatusByMessageId() {
        return persistableStore.getMessageDeliveryStatusByMessageId();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void processAckMessage(AckMessage ackMessage) {
        String messageId = ackMessage.getId();
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            if (messageDeliveryStatusByMessageId.containsKey(messageId)) {
                Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(messageId);
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
            } else {
                persistableStore.getCreationDateByMessageId().putIfAbsent(messageId, System.currentTimeMillis());
                messageDeliveryStatusByMessageId.put(messageId, new Observable<>(MessageDeliveryStatus.ACK_RECEIVED));
            }
            log.info("Received AckMessage for message with ID {} and set status to {}",
                    messageId, messageDeliveryStatusByMessageId.get(messageId).get());
            persist();
        }
    }

    private void processAckRequestingMessage(AckRequestingMessage message) {
        if (ackedMessageIds.contains(message.getId())) {
            log.warn("We received already that AckRequestingMessage and sent the AckMessage");
            return;
        }

        AckMessage ackMessage = new AckMessage(message.getId());
        NetworkId networkId = message.getReceiver();
        keyBundleService.findKeyPair(networkId.getPubKey().getKeyId())
                .ifPresent(keyPair -> {
                    log.info("Received a {} with message ID {}", message.getClass().getSimpleName(), message.getId());
                    NetworkIdWithKeyPair networkIdWithKeyPair = new NetworkIdWithKeyPair(networkId, keyPair);
                    networkService.confidentialSend(ackMessage, message.getSender(), networkIdWithKeyPair);
                    ackedMessageIds.add(message.getId());
                });
    }

    private void checkPending() {
        Set<Map.Entry<String, Observable<MessageDeliveryStatus>>> pendingItems = getMessageDeliveryStatusByMessageId().entrySet().stream()
                .filter(e -> e.getValue().get() == MessageDeliveryStatus.CONNECTING ||
                        e.getValue().get() == MessageDeliveryStatus.SENT ||
                        e.getValue().get() == MessageDeliveryStatus.TRY_ADD_TO_MAILBOX)
                .collect(Collectors.toSet());
        if (!pendingItems.isEmpty()) {
            log.warn("We have pending messages which have not been successfully sent. pendingItems={}", pendingItems);
            pendingItems.forEach(e -> getMessageDeliveryStatusByMessageId().get(e.getKey()).set(MessageDeliveryStatus.FAILED));
            persist();
        }
    }
}
