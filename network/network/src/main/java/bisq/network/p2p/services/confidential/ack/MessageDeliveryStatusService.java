package bisq.network.p2p.services.confidential.ack;

import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.security.KeyPairService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * This service aggregates the message delivery status for all supported transports and provides
 * a map with the message ID as key and an observable MessageDeliveryStatus as value which holds the most
 * relevant delivery state of all transports.
 * This service depends on the ConfidentialMessageService is only enabled if the ServiceNode.Service.ACK and
 * the ServiceNode.Service.CONFIDENTIAL are set in the config.
 */
@Slf4j
@Getter
public class MessageDeliveryStatusService implements PersistenceClient<MessageDeliveryStatusStore>, Service, MessageListener {
    private final MessageDeliveryStatusStore persistableStore = new MessageDeliveryStatusStore();
    private final Persistence<MessageDeliveryStatusStore> persistence;
    private final KeyPairService keyPairService;
    private final NetworkService networkService;

    public MessageDeliveryStatusService(PersistenceService persistenceService,
                                        KeyPairService keyPairService,
                                        NetworkService networkService) {
        this.keyPairService = keyPairService;
        this.networkService = networkService;

        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    public CompletableFuture<Boolean> initialize() {
        networkService.addMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        networkService.removeMessageListener(this);
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(NetworkMessage networkMessage) {
        if (networkMessage instanceof AckRequestingMessage) {
            processAckRequestingMessage((AckRequestingMessage) networkMessage);
        } else if (networkMessage instanceof AckMessage) {
            processAckMessage((AckMessage) networkMessage);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onMessageDeliveryStatusChanged(String messageId, MessageDeliveryStatus status) {
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = persistableStore.getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            if (messageDeliveryStatusByMessageId.containsKey(messageId)) {
                Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(messageId);
                // If we have already a received state we return.
                // This ensures that a later received failed state from one transport does not overwrite the received state from
                // another transport.
                if (observableStatus.get().isReceived()) {
                    return;
                }

                if (observableStatus.get() == MessageDeliveryStatus.ADDED_TO_MAILBOX && status.isReceived()) {
                    observableStatus.set(status);
                } else {
                    observableStatus.set(status);
                }
            } else {
                messageDeliveryStatusByMessageId.put(messageId, new Observable<>(status));
            }
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
        Map<String, Observable<MessageDeliveryStatus>> messageDeliveryStatusByMessageId = persistableStore.getMessageDeliveryStatusByMessageId();
        synchronized (messageDeliveryStatusByMessageId) {
            if (messageDeliveryStatusByMessageId.containsKey(messageId)) {
                Observable<MessageDeliveryStatus> observableStatus = messageDeliveryStatusByMessageId.get(messageId);
                // If we have already a received state we return.
                // This ensures that a later received failed state from one transport does not overwrite the received state from
                // another transport.
                if (observableStatus.get().isReceived()) {
                    return;
                }

                if (observableStatus.get() == MessageDeliveryStatus.ADDED_TO_MAILBOX) {
                    observableStatus.set(MessageDeliveryStatus.MAILBOX_MSG_RECEIVED);
                } else {
                    observableStatus.set(MessageDeliveryStatus.ARRIVED);
                }
            } else {
                messageDeliveryStatusByMessageId.put(messageId, new Observable<>(MessageDeliveryStatus.ARRIVED));
            }
            persist();
        }
    }

    private void processAckRequestingMessage(AckRequestingMessage message) {
        AckMessage ackMessage = new AckMessage(message.getId());
        keyPairService.findKeyPair(message.getReceiver().getPubKey().getKeyId())
                .ifPresent(keyPair -> {
                    NetworkIdWithKeyPair networkIdWithKeyPair = new NetworkIdWithKeyPair(message.getReceiver(), keyPair);
                    networkService.confidentialSend(ackMessage, message.getSender(), networkIdWithKeyPair);
                });
    }
}
