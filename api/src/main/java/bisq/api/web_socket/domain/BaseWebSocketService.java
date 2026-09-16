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

package bisq.api.web_socket.domain;


import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionRequest;
import bisq.api.web_socket.subscription.Topic;
import bisq.api.web_socket.subscription.WebSocketEvent;
import bisq.common.application.Service;
import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public abstract class BaseWebSocketService implements Service {
    protected final SubscriberRepository subscriberRepository;
    protected final Topic topic;

    public BaseWebSocketService(SubscriberRepository subscriberRepository,
                                Topic topic) {
        this.subscriberRepository = subscriberRepository;
        this.topic = topic;
    }

    abstract public Optional<String> getJsonPayload();

    /**
     * Returns the payload for the given subscription parameter. Defaults to the parameter-less
     * payload.
     */
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        return getJsonPayload();
    }

    /**
     * Returns a canonical form of the subscription parameter for use as a bucket key.
     * Subclasses that perform case-insensitive or defaulted parameter matching should override
     * this so that logically identical parameters map to a single bucket.
     */
    public Optional<String> canonicalizeParameter(Optional<String> parameter) {
        return parameter;
    }

    public void validate(SubscriptionRequest request) {
    }

    /** The subscribers of this service's topic, flattened across subscription parameters. */
    protected List<Subscriber> findSubscribers() {
        return subscriberRepository.findSubscribers(topic).values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    //todo
    protected <T> Optional<String> toJson(T payload) {
        try {
            return Optional.of(JsonMapperProvider.get().writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Json serialisation failed", e);
        }
        return Optional.empty();
    }

    protected void send(Set<Subscriber> subscribers,
                        Optional<String> jsonPayload,
                        Topic topic,
                        ModificationType modificationType) {
        jsonPayload.ifPresent(json ->
                send(json, subscribers, topic, modificationType));
    }

    protected void send(Optional<String> jsonPayload,
                        Topic topic,
                        ModificationType modificationType) {
        jsonPayload.ifPresent(json ->
                subscriberRepository.findSubscribers(topic).values().stream()
                        .flatMap(Collection::stream)
                        .forEach(subscriber -> send(json, subscriber, modificationType)));
    }

    protected void send(String json,
                        Set<Subscriber> subscribers,
                        Topic topic,
                        ModificationType modificationType) {
        subscribers.forEach(subscriber -> send(json, subscriber, modificationType));
    }

    protected void send(String json,
                        Subscriber subscriber,
                        ModificationType modificationType) {
        // Hoisted only so the log line below can name it; the counter was already consumed here
        // unconditionally, including when the serialisation below fails.
        int sequenceNumber = subscriber.incrementAndGetSequenceNumber();
        // Split by level on purpose. INFO says a send happened and what shape it had — the sequence
        // number is what makes a gap or a reorder in the client's stream visible at all, and the size
        // is what tells you a send went out truncated. The payload itself is TRACE rather than DEBUG:
        // every topic's bodies pass through here, and since private chat that includes DM text and both
        // peers' profiles. Both are on this machine in cleartext anyway, so the point is not secrecy
        // from the host — it is that logs get pasted into bug reports and issue trackers, which data
        // dirs do not, and DEBUG is enabled far more casually than TRACE.
        log.info("Sending json with modificationType {} to subscriber {} on topic {}. sequenceNumber={}, jsonLength={}",
                modificationType, subscriber.getSubscriberId(), subscriber.getTopic(), sequenceNumber, json.length());
        log.trace("Payload sent to subscriber {}: {}", subscriber.getSubscriberId(), json);

        WebSocketEvent.toJson(
                        subscriber.getTopic(),
                        subscriber.getSubscriberId(),
                        json,
                        modificationType,
                        sequenceNumber)
                .ifPresent(subscriber::send);
    }
}
