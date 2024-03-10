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

package bisq.network.p2p.services.confidential.resend;

import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.map.HashMapObserver;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatus;
import bisq.network.p2p.services.confidential.ack.MessageDeliveryStatusService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class ResendMessageService implements PersistenceClient<ResendMessageStore> {
    private final ResendMessageStore persistableStore = new ResendMessageStore();
    private final Persistence<ResendMessageStore> persistence;
    private final NetworkService networkService;
    private final MessageDeliveryStatusService messageDeliveryStatusService;
    private final Map<String, Pin> messageDeliveryStatusPinByMessageId = new HashMap<>();
    private final Set<Pin> nodeStatePins = new HashSet<>();
    private Pin messageDeliveryStatusByMessageIdPin;

    public ResendMessageService(PersistenceService persistenceService,
                                NetworkService networkService,
                                MessageDeliveryStatusService messageDeliveryStatusService) {

        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.PRIVATE, persistableStore);
        this.networkService = networkService;
        this.messageDeliveryStatusService = messageDeliveryStatusService;
    }

    @Override
    public ResendMessageStore prunePersisted(ResendMessageStore persisted) {
        return new ResendMessageStore(persisted.getResendMessageDataByMessageId().entrySet().stream()
                .filter(e -> !e.getValue().isExpired())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public void initialize() {
        messageDeliveryStatusByMessageIdPin = messageDeliveryStatusService.getMessageDeliveryStatusByMessageId().addObserver(
                new HashMapObserver<>() {
                    @Override
                    public void put(String messageId, Observable<MessageDeliveryStatus> status) {
                        handleMessageDeliveryStatusUpdate(messageId, status);
                    }

                    @Override
                    public void putAll(Map<? extends String, ? extends Observable<MessageDeliveryStatus>> map) {
                        map.forEach((key, value) -> handleMessageDeliveryStatusUpdate(key, value));
                    }

                    @Override
                    public void remove(Object messageId) {
                    }

                    @Override
                    public void clear() {
                    }
                });

        networkService.getDefaultNodeStateByTransportType().forEach((key, value) -> {
            nodeStatePins.add(value.addObserver(state -> {
                if (state == Node.State.RUNNING) {
                    Scheduler.run(this::resendMessageAllFailedMessages).after(10, TimeUnit.SECONDS);
                }
            }));
        });
    }

    public void shutdown() {
        messageDeliveryStatusByMessageIdPin.unbind();
        messageDeliveryStatusPinByMessageId.values().forEach(Pin::unbind);
        messageDeliveryStatusPinByMessageId.clear();
        nodeStatePins.forEach(Pin::unbind);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void handleResendMessageData(ResendMessageData resendMessageData) {
        MessageDeliveryStatus messageDeliveryStatus = resendMessageData.getMessageDeliveryStatus();
        String messageId = resendMessageData.getId();
        synchronized (this) {
            switch (messageDeliveryStatus) {
                case CONNECTING:
                    persistableStore.getResendMessageDataByMessageId().put(messageId, resendMessageData);
                    persist();
                    break;
                case ACK_RECEIVED:
                case MAILBOX_MSG_RECEIVED:
                    persistableStore.getResendMessageDataByMessageId().remove(messageId);
                    persist();
                    break;
                case SENT:
                case TRY_ADD_TO_MAILBOX:
                case ADDED_TO_MAILBOX:
                case FAILED:
                    break;
            }
        }
        persist();
    }

    public void resendMessage(String messageId) {
        synchronized (this) {
            findResendMessageData(messageId).ifPresent(data -> {
                log.info("Resending message which previously failed");
                NetworkIdWithKeyPair senderNetworkIdWithKeyPair = new NetworkIdWithKeyPair(data.getSenderNetworkId(), data.getSenderKeyPair());
                networkService.confidentialSend(data.getEnvelopePayloadMessage(),
                        data.getReceiverNetworkId(),
                        senderNetworkIdWithKeyPair);
            });
        }
    }

    public boolean canResendMessage(String messageId) {
        return findResendMessageData(messageId).isPresent();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private void handleMessageDeliveryStatusUpdate(String messageId, Observable<MessageDeliveryStatus> messageDeliveryStatus) {
        findResendMessageData(messageId).ifPresent(resendMessageData -> {
            Optional.ofNullable(messageDeliveryStatusPinByMessageId.get(messageId)).ifPresent(Pin::unbind);
            Pin pin = messageDeliveryStatus.addObserver(status -> {
                synchronized (this) {
                    Map<String, ResendMessageData> resendMessageDataByMessageId = persistableStore.getResendMessageDataByMessageId();
                    if (MessageDeliveryStatus.ADDED_TO_MAILBOX == status) {
                        // We get the ADDED_TO_MAILBOX state once we have broadcast successfully after the
                        // TRY_ADD_TO_MAILBOX state was set. We update the existing data with the new status
                        Optional.ofNullable(resendMessageDataByMessageId.get(messageId))
                                .ifPresent(data -> {
                                    resendMessageDataByMessageId.put(messageId, ResendMessageData.from(data, status));
                                    persist();
                                });
                    } else if (MessageDeliveryStatus.ACK_RECEIVED == status ||
                            MessageDeliveryStatus.MAILBOX_MSG_RECEIVED == status) {
                        // The MAILBOX_MSG_RECEIVED or ACK_RECEIVED is set when an AckMessage is processed
                        resendMessageDataByMessageId.remove(resendMessageData.getId());
                        persist();
                        messageDeliveryStatusPinByMessageId.get(messageId).unbind();
                        messageDeliveryStatusPinByMessageId.remove(messageId);
                    }
                }
            });
            messageDeliveryStatusPinByMessageId.put(messageId, pin);
        });
    }

    private Optional<ResendMessageData> findResendMessageData(String messageId) {
        return persistableStore.getResendMessageDataByMessageId().entrySet().stream()
                .filter(entry -> entry.getKey().equals(messageId))
                .filter(entry -> !entry.getValue().isExpired())
                .map(Map.Entry::getValue)
                .filter(data -> data.getId().equals(messageId))
                .findAny();
    }

    private void resendMessageAllFailedMessages() {
        persistableStore.getResendMessageDataByMessageId().keySet().forEach(this::resendMessage);
    }
}
