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

package bisq.http_api.web_socket.domain;


import bisq.common.observable.Pin;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.http_api.web_socket.subscription.Topic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Utility for an observable value in a flat structure.
 * It uses the `ModificationType.REPLACE`, so we deliver the full data source at any change.
 *
 * @param <T> The type fo the observable.
 * @param <R> The type of the payloadEncoded to be sent to the client.
 */
@Slf4j
public abstract class SimpleObservableWebSocketService<T, R> extends BaseWebSocketService {
    protected Pin pin;

    public SimpleObservableWebSocketService(ObjectMapper objectMapper,
                                            SubscriberRepository subscriberRepository, Topic topic) {
        super(objectMapper, subscriberRepository, topic);
    }

    abstract protected Pin setupObserver();

    abstract protected R toPayload(T observable);

    abstract protected T getObservable();

    @Override
    public CompletableFuture<Boolean> initialize() {
        pin = setupObserver();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        pin.unbind();
        return CompletableFuture.completedFuture(true);
    }

    protected void onChange() {
        subscriberRepository.findSubscribers(topic)
                .ifPresent(subscribers ->
                        send(subscribers, getJsonPayload(), topic, ModificationType.REPLACE));
    }

    public Optional<String> getJsonPayload() {
        return toJson(toPayload(getObservable()));
    }

    protected void send(R payload,
                        Topic topic,
                        ModificationType modificationType) {
        subscriberRepository.findSubscribers(topic)
                .ifPresent(subscribers -> {
                    toJson(payload).ifPresent(json ->
                            send(json, subscribers, topic, modificationType));
                });
    }
}
