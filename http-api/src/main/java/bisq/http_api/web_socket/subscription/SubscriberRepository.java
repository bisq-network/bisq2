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

package bisq.http_api.web_socket.subscription;

import bisq.common.observable.map.ObservableHashMap;
import bisq.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SubscriberRepository {
    private final ObservableHashMap<Topic, Set<Subscriber>> subscribersByTopic = new ObservableHashMap<>();
    private final Object subscribersByTopicLock = new Object();

    public void onConnectionClosed(WebSocket webSocket) {
        findSubscribers(webSocket).forEach(this::remove);
    }

    public void add(SubscriptionRequest request, WebSocket webSocket) {
        Topic topic = request.getTopic();
        Optional<String> parameter = StringUtils.toOptional(request.getParameter());
        Subscriber subscriber = new Subscriber(topic, parameter, request.getRequestId(), webSocket, request.getWebSocketEventClassName());
        synchronized (subscribersByTopicLock) {
            Set<Subscriber> subscribers = subscribersByTopic.computeIfAbsent(topic, key -> new HashSet<>());
            subscribers.add(subscriber);
        }

        findSubscribers(webSocket);
    }

    public void remove(Subscriber subscriber) {
        remove(subscriber.getTopic(), subscriber.getSubscriberId());
    }

    public void remove(Topic topic, String subscriberId) {
        synchronized (subscribersByTopicLock) {
            Optional.ofNullable(subscribersByTopic.get(topic))
                    .ifPresent(subscribers -> {
                        subscribers.removeIf(subscriber ->
                                subscriber.getSubscriberId().equals(subscriberId));
                        if (subscribers.isEmpty()) {
                            subscribersByTopic.remove(topic);
                        }
                    });
        }
    }

    public Optional<Set<Subscriber>> findSubscribers(Topic topic) {
        synchronized (subscribersByTopicLock) {
            return Optional.ofNullable(subscribersByTopic.get(topic))
                    .filter(subscribers -> !subscribers.isEmpty());
        }
    }

    public Optional<Set<Subscriber>> findSubscribers(Topic topic, String parameter) {
        synchronized (subscribersByTopicLock) {
            return findSubscribers(topic)
                    .map(set -> set.stream()
                            .filter(subscriber -> subscriber.getParameter()
                                    .map(param -> param.equals(parameter))
                                    .orElse(true))
                            .collect(Collectors.toSet()));
        }
    }

    public Set<Subscriber> findSubscribers(WebSocket webSocket) {
        synchronized (subscribersByTopicLock) {
            return subscribersByTopic.values().stream()
                    .flatMap(set -> set.stream()
                            .filter(subscriber -> subscriber.getWebSocket().equals(webSocket)))
                    .collect(Collectors.toSet());
        }
    }
}
