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

import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.threading.ExecutorFactory;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDto;
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDtoFactory;
import bisq.api.dto.presentation.offerbook.SellerReputationVerdict;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.OFFERS;

@Slf4j
public class OffersWebSocketService extends BaseWebSocketService {
    private static final long RESEND_DELAY_SECONDS = 30;

    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final BisqEasySellersReputationBasedTradeAmountService sellersReputationService;
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
    private final Set<Pin> pins = new HashSet<>();
    // Held for each send of an offer, so that a resend cannot overtake the removal of its offer. A send only hands the
    // event to the subscriber's executor, which delivers the events in that order, so no network I/O runs under it.
    private final Object offerUpdateLock = new Object();
    private final ScheduledExecutorService resendExecutor;
    private final AtomicBoolean isResendScheduled = new AtomicBoolean();
    // Verdicts the subscribers hold. Keyed like the channel's message set, as authors choose the IDs.
    // Written only under offerUpdateLock.
    private final Map<BisqEasyOfferbookMessage, Boolean> sellerReputationVerdictByMessage = new ConcurrentHashMap<>();

    public OffersWebSocketService(SubscriberRepository subscriberRepository,
                                  ChatService chatService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService,
                                  BisqEasyService bisqEasyService) {
        this(subscriberRepository,
                chatService,
                userService,
                bondedRolesService,
                bisqEasyService,
                ExecutorFactory.newSingleThreadScheduledExecutor("OffersWebSocketService.resend"));
    }

    @VisibleForTesting
    OffersWebSocketService(SubscriberRepository subscriberRepository,
                           ChatService chatService,
                           UserService userService,
                           BondedRolesService bondedRolesService,
                           BisqEasyService bisqEasyService,
                           ScheduledExecutorService resendExecutor) {
        super(subscriberRepository, OFFERS);
        this.resendExecutor = resendExecutor;

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
        marketPriceService = bondedRolesService.getMarketPriceService();
        sellersReputationService = bisqEasyService.getBisqEasySellersReputationBasedTradeAmountService();
        bisqEasyOfferbookMessageService = bisqEasyService.getBisqEasyOfferbookMessageService();
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        // The channels is a static list and does not change at runtime
        bisqEasyOfferbookChannelService.getChannels().forEach(channel -> {
            String quoteCurrencyCode = channel.getMarket().getQuoteCurrencyCode();
            pins.add(channel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void onAdded(BisqEasyOfferbookMessage message) {
                    message.getBisqEasyOffer().ifPresent(offer -> {
                        synchronized (offerUpdateLock) {
                            // At initialize, onAllAdded runs without the channel store lock, so the removal of the
                            // message can be handled first.
                            if (!channel.getChatMessages().contains(message) || !hasAuthorProfile(message)) {
                                return;
                            }
                            findSellerReputationVerdict(offer).ifPresent(verdict -> {
                                sellerReputationVerdictByMessage.put(message, verdict);
                                send(quoteCurrencyCode, message, ModificationType.ADDED, verdict);
                            });
                        }
                    });
                }

                @Override
                public void onRemoved(Object element) {
                    if (element instanceof BisqEasyOfferbookMessage message) {
                        if (message.hasBisqEasyOffer()) {
                            synchronized (offerUpdateLock) {
                                // Clients ignore the verdict of a removed item
                                boolean verdict = Objects.requireNonNullElse(
                                        sellerReputationVerdictByMessage.remove(message), false);
                                send(quoteCurrencyCode, message, ModificationType.REMOVED, verdict);
                            }
                        }
                    }
                }

                @Override
                public void onCleared() {
                    throw new NotImplementedException("Clear channel.getChatMessages() is not expected to be used");
                }
            }));
        });
        // Market price and reputation score changes advance the revision after the verdict cache was invalidated.
        pins.add(bisqEasyOfferbookMessageService.getOfferValidityRevision()
                .addObserver(revision -> scheduleResend()));
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        pins.forEach(Pin::unbind);
        pins.clear();
        resendExecutor.shutdownNow();
        return CompletableFuture.completedFuture(true);
    }

    @VisibleForTesting
    int getNumSellerReputationVerdicts() {
        return sellerReputationVerdictByMessage.size();
    }

    @Override
    public Optional<String> canonicalizeParameter(Optional<String> parameter) {
        return parameter.map(p -> p.trim().toUpperCase(Locale.ROOT)).filter(p -> !p.isEmpty());
    }

    @Override
    public Optional<String> getJsonPayload() {
        return getJsonPayload(bisqEasyOfferbookChannelService.getChannels().stream());
    }

    @Override
    public Optional<String> getJsonPayload(Optional<String> parameter) {
        Stream<BisqEasyOfferbookChannel> channels = bisqEasyOfferbookChannelService.getChannels().stream();
        if (parameter.isPresent()) {
            String quoteCurrencyCode = parameter.get();
            channels = channels.filter(channel ->
                    channel.getMarket().getQuoteCurrencyCode().equalsIgnoreCase(quoteCurrencyCode));
        }
        return getJsonPayload(channels);
    }

    private Optional<String> getJsonPayload(Stream<BisqEasyOfferbookChannel> channels) {
        ArrayList<OfferItemPresentationDto> payload = channels
                .flatMap(channel ->
                        channel.getChatMessages().stream()
                                .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                                .map(message -> {
                                    try {
                                        // What the other subscribers hold; a pending pass corrects all
                                        return Optional.ofNullable(sellerReputationVerdictByMessage.get(message))
                                                .or(() -> findSellerReputationVerdict(
                                                        message.getBisqEasyOffer().orElseThrow()))
                                                .flatMap(verdict -> createOfferListItemDto(message, verdict))
                                                .orElse(null);
                                    } catch (Exception e) {
                                        log.error("Failed to create OfferListItemDto", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    // Revisions come in bursts and each pass checks all offers, so one delayed pass serves a burst. It runs on its own
    // thread, not on the thread which changed the price or the score.
    private void scheduleResend() {
        if (isResendScheduled.compareAndSet(false, true)) {
            try {
                resendExecutor.schedule(() -> {
                    // Reset before the pass, so that a revision during the pass schedules another one
                    isResendScheduled.set(false);
                    try {
                        resendOffersWithChangedSellerReputationVerdict();
                    } catch (Exception e) {
                        log.error("Failed to resend offers with a changed seller reputation verdict", e);
                    }
                }, RESEND_DELAY_SECONDS, TimeUnit.SECONDS);
            } catch (RejectedExecutionException e) {
                // Shutdown stopped the executor during this revision; no pass is needed anymore
            }
        }
    }

    // Checks outside the lock, as adding and removing messages wait for it. A verdict which a revision makes stale
    // during the check is corrected by the pass which that revision schedules.
    private void resendOffersWithChangedSellerReputationVerdict() {
        bisqEasyOfferbookChannelService.getChannels().forEach(channel -> {
            String quoteCurrencyCode = channel.getMarket().getQuoteCurrencyCode();
            channel.getBisqEasyOfferbookMessagesWithOffer()
                    .filter(this::hasAuthorProfile)
                    .forEach(message ->
                            findSellerReputationVerdict(message.getBisqEasyOffer().orElseThrow()).ifPresent(verdict -> {
                                synchronized (offerUpdateLock) {
                                    // Removed during the check; resending would revive it in clients
                                    if (!channel.getChatMessages().contains(message)) {
                                        return;
                                    }
                                    // Clients replace an item with the same offer ID; REPLACE clears the market
                                    if (!Objects.equals(sellerReputationVerdictByMessage.put(message, verdict), verdict)) {
                                        send(quoteCurrencyCode, message, ModificationType.ADDED, verdict);
                                    }
                                }
                            }));
        });
    }

    // The item shows the author's profile, so no item is sent without it. The verdict would then use a score of 0, and
    // clients keep the verdict which they received before.
    private boolean hasAuthorProfile(BisqEasyOfferbookMessage message) {
        return userProfileService.findUserProfile(message.getAuthorUserProfileId()).isPresent();
    }

    // One offer whose check fails must not stop the others
    private Optional<Boolean> findSellerReputationVerdict(BisqEasyOffer offer) {
        return SellerReputationVerdict.find(sellersReputationService, offer);
    }

    private void send(String quoteCurrencyCode,
                      BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                      ModificationType modificationType,
                      boolean hasSellerSufficientReputation) {
        try {
            List<Subscriber> subscribers = Stream.concat(
                    subscriberRepository.findSubscribers(topic, canonicalizeParameter(Optional.of(quoteCurrencyCode))).stream(),
                    subscriberRepository.findSubscribers(topic, Optional.empty()).stream()
            ).toList();
            if (subscribers.isEmpty()) {
                return;
            }
            createOfferListItemDto(bisqEasyOfferbookMessage, hasSellerSufficientReputation).ifPresentOrElse(item -> {
                // The payload is defined as a list to support batch data delivery at subscribe.
                ArrayList<OfferItemPresentationDto> payload = new ArrayList<>(List.of(item));
                toJson(payload).ifPresent(json -> {
                    for (Subscriber subscriber : subscribers) {
                        try {
                            send(json, subscriber, modificationType);
                        } catch (Exception e) {
                            log.error("Failed to send offer update to subscriber={}, json={}", subscriber, json, e);
                        }
                    }
                });
            }, () -> log.debug("Skipping websocket send for offer from authorUserProfileId={}: user profile not yet available",
                    bisqEasyOfferbookMessage.getAuthorUserProfileId()));
        } catch (Exception e) {
            log.error("Failed to send OfferListItemDto update for quoteCurrencyCode={}", quoteCurrencyCode, e);
        }
    }

    private Optional<OfferItemPresentationDto> createOfferListItemDto(BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                                                                      boolean hasSellerSufficientReputation) {
        return OfferItemPresentationDtoFactory.create(userProfileService,
                userIdentityService,
                reputationService,
                marketPriceService,
                hasSellerSufficientReputation,
                bisqEasyOfferbookMessage);
    }
}
