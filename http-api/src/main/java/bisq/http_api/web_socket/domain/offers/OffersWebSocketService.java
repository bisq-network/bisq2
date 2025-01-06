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

package bisq.http_api.web_socket.domain.offers;

import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.dto.offer.bisq_easy.OfferListItemDtoFactory;
import bisq.dto.offer.bisq_easy.OfferListItemDto;
import bisq.http_api.web_socket.domain.BaseWebSocketService;
import bisq.http_api.web_socket.subscription.ModificationType;
import bisq.http_api.web_socket.subscription.SubscriberRepository;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.http_api.web_socket.subscription.Topic.OFFERS;

@Slf4j
public class OffersWebSocketService extends BaseWebSocketService {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final Set<Pin> pins = new HashSet<>();

    public OffersWebSocketService(ObjectMapper objectMapper,
                                  SubscriberRepository subscriberRepository,
                                  ChatService chatService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService) {
        super(objectMapper, subscriberRepository, OFFERS);

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
        marketPriceService = bondedRolesService.getMarketPriceService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // The channels is a static list and does not change at runtime
        bisqEasyOfferbookChannelService.getChannels().forEach(channel -> {
            String quoteCurrencyCode = channel.getMarket().getQuoteCurrencyCode();
            pins.add(channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(BisqEasyOfferbookMessage message) {
                    if (message.hasBisqEasyOffer()) {
                        send(quoteCurrencyCode, message, ModificationType.ADDED);
                    }
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof BisqEasyOfferbookMessage message) {
                        if (message.hasBisqEasyOffer()) {
                            send(quoteCurrencyCode, message, ModificationType.REMOVED);
                        }
                    }
                }

                @Override
                public void clear() {
                    throw new NotImplementedException("Clear channel.getChatMessages() is not expected to be used");
                }
            }));
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        pins.forEach(Pin::unbind);
        pins.clear();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(bisqEasyOfferbookChannelService.getChannels().stream());
    }

    private Optional<String> getJsonPayload(Stream<BisqEasyOfferbookChannel> channels) {
        ArrayList<OfferListItemDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                                .map(message -> {
                                    try {
                                        return createOfferListItemDto(message);
                                    } catch (Exception e) {
                                        log.error("Failed to create OfferListItemDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    private void send(String quoteCurrencyCode,
                      BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                      ModificationType modificationType) {
        OfferListItemDto item = createOfferListItemDto(bisqEasyOfferbookMessage);
        // The payload is defined as a list to support batch data delivery at subscribe.
        ArrayList<OfferListItemDto> payload = new ArrayList<>(List.of(item));
        toJson(payload).ifPresent(json -> {
            subscriberRepository.findSubscribers(topic, quoteCurrencyCode)
                    .ifPresent(subscribers -> subscribers
                            .forEach(subscriber -> send(json, subscriber, modificationType)));
        });
    }

    private OfferListItemDto createOfferListItemDto(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        return OfferListItemDtoFactory.create(userProfileService,
                userIdentityService,
                reputationService,
                marketPriceService,
                bisqEasyOfferbookMessage);
    }
}
