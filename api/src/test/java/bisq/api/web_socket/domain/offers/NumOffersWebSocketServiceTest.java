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

package bisq.api.web_socket.domain.offers;

import bisq.api.dto.presentation.offerbook.OfferItemPresentationDto;
import bisq.api.offerbook.RecordedOfferItems;
import bisq.api.offerbook.RecordedOfferItems.RecordedOffer;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.SubscriptionSpecifier;
import bisq.api.web_socket.subscription.Topic;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.json.JsonMapperProvider;
import bisq.user.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NumOffersWebSocketServiceTest {
    private final RecordedOfferItems recordedOffers = new RecordedOfferItems(
            mock(UserService.class, RETURNS_DEEP_STUBS), mock(BondedRolesService.class, RETURNS_DEEP_STUBS));
    private final List<String> sentEvents = new ArrayList<>();
    private final NumOffersWebSocketService service;

    NumOffersWebSocketServiceTest() {
        Subscriber subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(Topic.NUM_OFFERS);
        when(subscriber.getSubscriberId()).thenReturn("subscriber-id");
        when(subscriber.send(anyString())).thenAnswer(invocation -> {
            sentEvents.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(true);
        });
        SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
        when(subscriberRepository.findSubscribers(Topic.NUM_OFFERS))
                .thenReturn(Map.of(new SubscriptionSpecifier(Topic.NUM_OFFERS, Optional.empty()), Set.of(subscriber)));
        service = new NumOffersWebSocketService(subscriberRepository, recordedOffers);
    }

    // The OFFERS service records the offers and publishes their count per market
    @Test
    void countFollowsTheRecordedOffers() throws Exception {
        service.initialize().join();
        sentEvents.clear();

        recordedOffers.publishCount("USD");
        assertThat(sentEvents).hasSize(1);
        assertThat(sentCounts(sentEvents.get(0))).isEqualTo(Map.of("USD", 0));

        recordedOffers.put(offerMessage(), new RecordedOffer("USD", true, item()));
        recordedOffers.put(offerMessage(), new RecordedOffer("EUR", true, item()));
        recordedOffers.publishCount("USD");
        assertThat(sentEvents).hasSize(2);
        assertThat(sentCounts(sentEvents.get(1))).isEqualTo(Map.of("USD", 1));

        // Unchanged counts are not sent
        recordedOffers.publishCount("USD");
        assertThat(sentEvents).hasSize(2);

        recordedOffers.publishCount("EUR");
        assertThat(sentEvents).hasSize(3);
        assertThat(sentCounts(sentEvents.get(2))).isEqualTo(Map.of("USD", 1, "EUR", 1));
        assertThat(parseCounts(service.getJsonPayload().orElseThrow())).isEqualTo(Map.of("USD", 1, "EUR", 1));
    }

    @Test
    void nothingIsSentAfterShutdown() {
        service.initialize().join();
        service.shutdown().join();
        sentEvents.clear();

        recordedOffers.publishCount("USD");
        assertThat(sentEvents).isEmpty();
    }

    private static BisqEasyOfferbookMessage offerMessage() {
        return mock(BisqEasyOfferbookMessage.class);
    }

    private static OfferItemPresentationDto item() {
        return new OfferItemPresentationDto(null, false, null, null, null, null, null, null, null, null, null, true);
    }

    private static Map<String, Integer> sentCounts(String event) throws Exception {
        JsonNode envelope = JsonMapperProvider.get().readTree(event);
        assertThat(envelope.get("modificationType").asText()).isEqualTo("REPLACE");
        return parseCounts(envelope.get("payload").asText());
    }

    private static Map<String, Integer> parseCounts(String json) throws Exception {
        return JsonMapperProvider.get().readValue(json, new TypeReference<>() {
        });
    }
}
