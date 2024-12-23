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


import bisq.common.application.Service;
import bisq.dto.account.protocol_type.TradeProtocolTypeDto;
import bisq.dto.offer.amount.spec.AmountSpecDto;
import bisq.dto.offer.bisq_easy.OfferListItemDto;
import bisq.dto.offer.options.OfferOptionDto;
import bisq.dto.offer.price.spec.PriceSpecDto;
import bisq.http_api.web_socket.subscription.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
public abstract class BaseWebSocketService implements Service {
    protected final ObjectMapper objectMapper;
    protected final SubscriberRepository subscriberRepository;
    protected final Topic topic;

    public BaseWebSocketService(ObjectMapper objectMapper,
                                SubscriberRepository subscriberRepository, Topic topic) {
        this.objectMapper = objectMapper;
        this.subscriberRepository = subscriberRepository;
        this.topic = topic;
    }

    abstract public Optional<String> getJsonPayload();

    //todo
    protected <T> Optional<String> toJson(T payload) {
        try {
            if (payload instanceof List<?> list && list.get(0) instanceof OfferListItemDto offerListItemDto) {
                try {
                    PriceSpecDto value = offerListItemDto.getBisqEasyOffer().priceSpec();
                    objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    log.error("Json serialisation failed", e);
                }
                try {
                    AmountSpecDto value = offerListItemDto.getBisqEasyOffer().amountSpec();
                    objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    log.error("Json serialisation failed", e);
                }
                try {
                    List<OfferOptionDto> value = offerListItemDto.getBisqEasyOffer().offerOptions();
                    objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    log.error("Json serialisation failed", e);
                }
                try {
                    List<TradeProtocolTypeDto> value = offerListItemDto.getBisqEasyOffer().protocolTypes();
                    objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    log.error("Json serialisation failed", e); //failed
                }
                try {
                    objectMapper.writeValueAsString(offerListItemDto.getBisqEasyOffer());
                } catch (JsonProcessingException e) {
                    log.error("Json serialisation failed", e); //failed
                }
            }
            return Optional.of(objectMapper.writeValueAsString(payload));
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
        subscriberRepository.findSubscribers(topic)
                .ifPresent(subscribers -> {
                    jsonPayload.ifPresent(json ->
                            send(json, subscribers, topic, modificationType));
                });
    }

    protected void send(String json,
                        Subscriber subscriber,
                        ModificationType modificationType) {
        WebSocketEvent.toJson(objectMapper,
                        subscriber.getTopic(),
                        subscriber.getSubscriberId(),
                        json,
                        modificationType,
                        subscriber.incrementAndGetSequenceNumber())
                .ifPresent(subscriber::send);
    }

    protected void send(String json,
                        Set<Subscriber> subscribers,
                        Topic topic,
                        ModificationType modificationType) {
        subscribers.forEach(subscriber ->
                send(json,
                        subscriber,
                        modificationType));
    }
}
