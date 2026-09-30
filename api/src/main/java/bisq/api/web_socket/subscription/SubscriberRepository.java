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

package bisq.api.web_socket.subscription;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.websockets.WebSocket;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class SubscriberRepository {
    private final Map<SubscriptionSpecifier, Set<Subscriber>> subscribersBySpecifier = new HashMap<>();
    private final Object lock = new Object();

    public void onConnectionClosed(WebSocket webSocket) {
        findSubscribers(webSocket).forEach(this::remove);
    }

    public Subscriber add(SubscriptionRequest request, Optional<String> canonicalParameter, WebSocket webSocket) {
        Topic topic = request.getTopic();
        Subscriber subscriber = new Subscriber(topic, canonicalParameter, request.getRequestId(), webSocket);
        synchronized (lock) {
            subscribersBySpecifier.computeIfAbsent(new SubscriptionSpecifier(topic, canonicalParameter), k -> new HashSet<>())
                    .add(subscriber);
        }
        return subscriber;
    }

    public void remove(Subscriber subscriber) {
        remove(subscriber.getTopic(), subscriber.getSubscriberId());
    }

    public void remove(Topic topic, String subscriberId) {
        synchronized (lock) {
            var iterator = subscribersBySpecifier.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().topic() == topic) {
                    boolean removed = entry.getValue().removeIf(s -> s.getSubscriberId().equals(subscriberId));
                    if (entry.getValue().isEmpty()) {
                        iterator.remove();
                    }
                    if (removed) {
                        break;
                    }
                }
            }
        }
    }

    public Set<Subscriber> findSubscribers(Topic topic, Optional<String> parameter) {
        synchronized (lock) {
            Set<Subscriber> result = subscribersBySpecifier.get(new SubscriptionSpecifier(topic, parameter));
            return result == null ? Set.of() : new HashSet<>(result);
        }
    }

    public Map<SubscriptionSpecifier, Set<Subscriber>> findSubscribers(Topic topic) {
        synchronized (lock) {
            return subscribersBySpecifier.entrySet().stream()
                    .filter(e -> e.getKey().topic() == topic && !e.getValue().isEmpty())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> new HashSet<>(e.getValue())
                    ));
        }
    }

    public Set<Subscriber> findSubscribers(WebSocket webSocket) {
        synchronized (lock) {
            return subscribersBySpecifier.values().stream()
                    .flatMap(Collection::stream)
                    .filter(subscriber -> subscriber.getWebSocket().equals(webSocket))
                    .collect(Collectors.toSet());
        }
    }
}
