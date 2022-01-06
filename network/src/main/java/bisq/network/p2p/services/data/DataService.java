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

package bisq.network.p2p.services.data;

import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.node.transport.Transport;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.broadcast.BroadcastResult;
import bisq.network.p2p.services.data.storage.Storage;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedPayload;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.MailboxPayload;
import lombok.Getter;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Single instance
 */
public class DataService implements Node.Listener {
    public interface Listener {
        void onNetworkDataAdded(NetworkPayload networkPayload);

        void onNetworkDataRemoved(NetworkPayload networkPayload);
    }

    @Getter
    private final Storage storage;
    private final Set<DataService.Listener> listeners = new CopyOnWriteArraySet<>();
    private final Map<Transport.Type, DataServicePerTransport> serviceByTransportType = new ConcurrentHashMap<>();

    public DataService(Storage storage) {
        this.storage = storage;

    }

    public void addService(Transport.Type transportType, Optional<DataServicePerTransport> optionalService) {
        optionalService.ifPresent(service -> {
            serviceByTransportType.put(transportType, service);
            service.addListener(this);
        });
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
        if (message instanceof BroadcastMessage broadcastMessage) {
            if (broadcastMessage.message() instanceof AddDataRequest addDataRequest) {
                storage.onAddRequest(addDataRequest)
                        .whenComplete((optionalData, throwable) -> {
                            optionalData.ifPresent(networkData -> {
                                listeners.forEach(listener -> listener.onNetworkDataAdded(networkData));
                                serviceByTransportType.values().forEach(e -> e.reBroadcast(addDataRequest));
                            });
                        });
            } else if (message instanceof RemoveDataRequest removeDataRequest) {
                // Message removedItem = storage.remove(removeDataRequest.getMapKey());
              /*  if (removedItem != null) {
                    dataListeners.forEach(listener -> listener.onDataRemoved(message));
                }*/
            }
        }
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }

    public Stream<AuthenticatedPayload> getAllAuthenticatedPayload() {
        return storage.getAllAuthenticatedPayload();
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addNetworkPayloadAsync(NetworkPayload networkPayload, KeyPair keyPair) {
        if (networkPayload instanceof AuthenticatedPayload authenticatedPayload) {
            return storage.getOrCreateAuthenticatedDataStore(authenticatedPayload.getMetaData())
                    .thenApply(store -> {
                        try {
                            AddAuthenticatedDataRequest addRequest = AddAuthenticatedDataRequest.from(store, authenticatedPayload, keyPair);
                            store.add(addRequest);
                            return serviceByTransportType.values().stream()
                                    .map(e -> e.broadcast(addRequest))
                                    .collect(Collectors.toList());
                        } catch (GeneralSecurityException e) {
                            e.printStackTrace();
                            throw new CompletionException(e);
                        }
                    });
        } else {
            return CompletableFuture.failedFuture(new IllegalArgumentException(""));
        }
    }

    public CompletableFuture<List<CompletableFuture<BroadcastResult>>> addMailboxPayloadAsync(MailboxPayload mailboxPayload,
                                                                                              KeyPair senderKeyPair,
                                                                                              PublicKey receiverPublicKey) {
        return storage.getOrCreateMailboxDataStore(mailboxPayload.getMetaData())
                .thenApply(store -> {
                    try {
                        AddMailboxRequest addRequest = AddMailboxRequest.from(store, mailboxPayload, senderKeyPair, receiverPublicKey);
                        store.add(addRequest);
                        return serviceByTransportType.values().stream()
                                .map(service -> service.broadcast(addRequest))
                                .collect(Collectors.toList());
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        throw new CompletionException(e);
                    }
                });
    }

    public void addListener(DataService.Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataService.Listener listener) {
        listeners.remove(listener);
    }

    public CompletableFuture<Void> shutdown() {
        listeners.clear();
        storage.shutdown();
        return CompletableFuture.completedFuture(null);
    }
}