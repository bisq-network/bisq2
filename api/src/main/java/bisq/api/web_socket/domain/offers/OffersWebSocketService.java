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
import bisq.api.dto.presentation.offerbook.OfferItemPresentationDtoFactory;
import bisq.api.offerbook.OfferVisibility;
import bisq.api.offerbook.RecordedOfferItems;
import bisq.api.offerbook.RecordedOfferItems.RecordedOffer;
import bisq.api.web_socket.domain.BaseWebSocketService;
import bisq.api.web_socket.subscription.ModificationType;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
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
import bisq.common.util.StringUtils;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bisq.api.web_socket.subscription.Topic.OFFERS;

@Slf4j
public class OffersWebSocketService extends BaseWebSocketService {
    private static final long RESEND_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(30);

    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
    private final Set<Pin> pins = new HashSet<>();
    // Held for each change of the record and each send, so that a pass cannot overtake the removal of its offer. A
    // send only hands the event to the subscriber's executor, which delivers the events in that order, so no network
    // I/O runs under it.
    private final Object offerUpdateLock = new Object();
    private final ScheduledExecutorService resendExecutor;
    private final AtomicBoolean isResendScheduled = new AtomicBoolean();
    private final LongSupplier nanoTime;
    // Written before isResendScheduled is reset, so that the revision which schedules the next pass reads it
    private volatile long lastPassStartNanos;
    private final RecordedOfferItems recordedOffers;
    // Messages whose item failed to build. Accessed only under offerUpdateLock.
    private final Set<BisqEasyOfferbookMessage> messagesWithFailedItem = new HashSet<>();
    // Messages whose visibility check failed. The pass checks outside the lock, so the set is concurrent.
    private final Set<BisqEasyOfferbookMessage> messagesWithFailedVisibility = ConcurrentHashMap.newKeySet();

    public OffersWebSocketService(SubscriberRepository subscriberRepository,
                                  ChatService chatService,
                                  UserService userService,
                                  BondedRolesService bondedRolesService,
                                  BisqEasyService bisqEasyService,
                                  RecordedOfferItems recordedOffers) {
        this(subscriberRepository,
                chatService,
                userService,
                bondedRolesService,
                bisqEasyService,
                recordedOffers,
                ExecutorFactory.newSingleThreadScheduledExecutor("OffersWebSocketService.resend"),
                System::nanoTime);
    }

    @VisibleForTesting
    OffersWebSocketService(SubscriberRepository subscriberRepository,
                           ChatService chatService,
                           UserService userService,
                           BondedRolesService bondedRolesService,
                           BisqEasyService bisqEasyService,
                           RecordedOfferItems recordedOffers,
                           ScheduledExecutorService resendExecutor,
                           LongSupplier nanoTime) {
        super(subscriberRepository, OFFERS);
        this.recordedOffers = recordedOffers;
        this.resendExecutor = resendExecutor;
        this.nanoTime = nanoTime;
        lastPassStartNanos = nanoTime.getAsLong() - RESEND_INTERVAL_NANOS;

        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
        marketPriceService = bondedRolesService.getMarketPriceService();
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
                    if (message.hasBisqEasyOffer()) {
                        updateOffer(channel, quoteCurrencyCode, message);
                        publishCount(quoteCurrencyCode);
                    }
                }

                // One count per market, also for a market without offers, as clients treat a market without a count
                // as unknown. A subscriber can be registered before the existing offers are recorded.
                @Override
                public void onAllAdded(Collection<? extends BisqEasyOfferbookMessage> messages) {
                    try {
                        messages.stream()
                                .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                                .forEach(message -> updateOffer(channel, quoteCurrencyCode, message));
                    } finally {
                        publishCount(quoteCurrencyCode);
                    }
                }

                @Override
                public void onRemoved(Object element) {
                    if (element instanceof BisqEasyOfferbookMessage message && message.hasBisqEasyOffer()) {
                        synchronized (offerUpdateLock) {
                            // No client holds an offer which was never recorded
                            recordedOffers.remove(message).ifPresent(recorded ->
                                    send(quoteCurrencyCode, recorded.item(), ModificationType.REMOVED));
                            messagesWithFailedItem.remove(message);
                            messagesWithFailedVisibility.remove(message);
                            recordedOffers.publishCount(quoteCurrencyCode);
                        }
                    }
                }

                @Override
                public void onCleared() {
                    throw new NotImplementedException("Clear channel.getChatMessages() is not expected to be used");
                }
            }));
        });
        // Market price, ignore list, ban list and reputation score changes advance the revision after the verdict
        // cache was invalidated.
        pins.add(bisqEasyOfferbookMessageService.getOfferValidityRevision()
                .addObserver(revision -> scheduleResend()));
        recordedOffers.setTracking(true);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        recordedOffers.setTracking(false);
        pins.forEach(Pin::unbind);
        pins.clear();
        resendExecutor.shutdownNow();
        return CompletableFuture.completedFuture(true);
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

    // The recorded offers, so that the snapshot and the events describe the same set. An offer which is not recorded
    // yet is sent by the pass which records it.
    private Optional<String> getJsonPayload(Stream<BisqEasyOfferbookChannel> channels) {
        ArrayList<OfferItemPresentationDto> payload = channels
                .flatMap(channel -> channel.getChatMessages().stream()
                        .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                        .map(recordedOffers::snapshotItem)
                        .filter(Optional::isPresent)
                        .map(Optional::get))
                .collect(Collectors.toCollection(ArrayList::new));
        return toJson(payload);
    }

    // Revisions come in bursts and each pass checks all offers, so passes start at most once per interval. The pass
    // runs on its own thread, not on the thread which changed the price or the score.
    private void scheduleResend() {
        if (isResendScheduled.compareAndSet(false, true)) {
            long delayNanos = Math.max(0, RESEND_INTERVAL_NANOS - (nanoTime.getAsLong() - lastPassStartNanos));
            try {
                resendExecutor.schedule(() -> {
                    lastPassStartNanos = nanoTime.getAsLong();
                    // Reset before the pass, so that a revision during the pass schedules another one
                    isResendScheduled.set(false);
                    try {
                        resendOffersWithChangedVisibility();
                    } catch (Exception e) {
                        log.error("Failed to resend offers with a changed visibility", e);
                    }
                }, delayNanos, TimeUnit.NANOSECONDS);
            } catch (RejectedExecutionException e) {
                // Shutdown stopped the executor during this revision; no pass is needed anymore
            }
        }
    }

    // One count per market, published also when an offer of the market fails
    private void resendOffersWithChangedVisibility() {
        bisqEasyOfferbookChannelService.getChannels().forEach(channel -> {
            String quoteCurrencyCode = channel.getMarket().getQuoteCurrencyCode();
            try {
                channel.getBisqEasyOfferbookMessagesWithOffer()
                        .forEach(message -> resendOffer(channel, quoteCurrencyCode, message));
            } finally {
                publishCount(quoteCurrencyCode);
            }
        });
    }

    // Checks outside the lock, as adding and removing messages wait for it. A visibility which a revision makes stale
    // during the check is corrected by the pass which that revision schedules.
    private void resendOffer(BisqEasyOfferbookChannel channel,
                             String quoteCurrencyCode,
                             BisqEasyOfferbookMessage message) {
        try {
            Optional<OfferVisibility> visibility = findVisibility(message);
            if (visibility.isEmpty()) {
                return;
            }
            synchronized (offerUpdateLock) {
                // Removed during the check; resending would revive it in clients
                if (!channel.getChatMessages().contains(message)) {
                    return;
                }
                applyVisibility(quoteCurrencyCode, message, visibility.get());
            }
        } catch (Exception e) {
            log.error("Failed to resend offer {}", offerIdForLog(message), e);
        }
    }

    // Checks under the lock, as additions are few. One failing offer must not stop the others of an onAllAdded.
    private void updateOffer(BisqEasyOfferbookChannel channel,
                             String quoteCurrencyCode,
                             BisqEasyOfferbookMessage message) {
        try {
            synchronized (offerUpdateLock) {
                // At initialize, onAllAdded runs without the channel store lock, so the removal of the message can be
                // handled first.
                if (!channel.getChatMessages().contains(message)) {
                    return;
                }
                findVisibility(message).ifPresent(visibility ->
                        applyVisibility(quoteCurrencyCode, message, visibility));
            }
        } catch (Exception e) {
            log.error("Failed to update offer {}", offerIdForLog(message), e);
        }
    }

    private void publishCount(String quoteCurrencyCode) {
        synchronized (offerUpdateLock) {
            recordedOffers.publishCount(quoteCurrencyCode);
        }
    }

    // The pass retries a failing check until it passes, so only the first failure is a warning
    private Optional<OfferVisibility> findVisibility(BisqEasyOfferbookMessage message) {
        try {
            Optional<OfferVisibility> visibility = OfferVisibility.find(bisqEasyOfferbookMessageService,
                    userProfileService,
                    userIdentityService,
                    message);
            messagesWithFailedVisibility.remove(message);
            return visibility;
        } catch (RuntimeException e) {
            String offerId = offerIdForLog(message);
            if (messagesWithFailedVisibility.add(message)) {
                log.warn("Could not check the visibility of offer {}: {}: {}",
                        offerId, e.getClass().getName(), StringUtils.sanitizeForLog(e.getMessage()));
            }
            log.debug("Could not check the visibility of offer {}", offerId, e);
            return Optional.empty();
        }
    }

    // Recorded before the subscriber lookup, so that a subscriber which registers meanwhile reads it in its snapshot.
    // Without an item, clients keep what they hold and the next pass tries again.
    private void applyVisibility(String quoteCurrencyCode,
                                 BisqEasyOfferbookMessage message,
                                 OfferVisibility visibility) {
        Optional<RecordedOffer> recorded = recordedOffers.find(message);
        if (!visibility.shown()) {
            recorded.ifPresent(r -> {
                recordedOffers.remove(message);
                send(quoteCurrencyCode, r.item(), ModificationType.REMOVED);
            });
            return;
        }
        // Clients replace an item with the same offer ID. An own offer which became valid is sent again, as its
        // item showed no amounts while the price was missing.
        boolean isRecorded = recorded
                .map(r -> r.valid() == visibility.valid()
                        && r.hasSellerSufficientReputation() == visibility.hasSellerSufficientReputation())
                .orElse(false);
        if (isRecorded) {
            return;
        }
        findItem(message, visibility.hasSellerSufficientReputation()).ifPresent(item -> {
            recordedOffers.put(message, new RecordedOffer(quoteCurrencyCode, visibility.valid(), item));
            send(quoteCurrencyCode, item, ModificationType.ADDED);
        });
    }

    private void send(String quoteCurrencyCode, OfferItemPresentationDto item, ModificationType modificationType) {
        try {
            List<Subscriber> subscribers = Stream.concat(
                    subscriberRepository.findSubscribers(topic, canonicalizeParameter(Optional.of(quoteCurrencyCode))).stream(),
                    subscriberRepository.findSubscribers(topic, Optional.empty()).stream()
            ).toList();
            if (subscribers.isEmpty()) {
                return;
            }
            // The payload is defined as a list to support batch data delivery at subscribe.
            Optional<String> json = toJson(new ArrayList<>(List.of(item)));
            if (json.isEmpty()) {
                return;
            }
            for (Subscriber subscriber : subscribers) {
                try {
                    send(json.get(), subscriber, modificationType);
                } catch (Exception e) {
                    // The payload stays out of the log, like in the other topics; it is in the trace log of the send
                    log.error("Failed to send offer update to subscriber={}, jsonLength={}",
                            subscriber.getSubscriberId(), json.get().length(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send OfferListItemDto update for quoteCurrencyCode={}", quoteCurrencyCode, e);
        }
    }

    // The pass retries a failed item until it can be built, so only the first failure is a warning
    private Optional<OfferItemPresentationDto> findItem(BisqEasyOfferbookMessage message,
                                                        boolean hasSellerSufficientReputation) {
        Optional<OfferItemPresentationDto> item;
        try {
            item = createOfferListItemDto(message, hasSellerSufficientReputation);
        } catch (RuntimeException e) {
            String offerId = offerIdForLog(message);
            if (messagesWithFailedItem.add(message)) {
                log.warn("Could not build the item of offer {}: {}: {}",
                        offerId, e.getClass().getName(), StringUtils.sanitizeForLog(e.getMessage()));
            }
            log.debug("Could not build the item of offer {}", offerId, e);
            return Optional.empty();
        }
        if (item.isEmpty()) {
            log.debug("Skipping websocket send for offer from authorUserProfileId={}: user profile not yet available",
                    message.getAuthorUserProfileId());
            return Optional.empty();
        }
        messagesWithFailedItem.remove(message);
        return item;
    }

    private Optional<OfferItemPresentationDto> createOfferListItemDto(BisqEasyOfferbookMessage message,
                                                                      boolean hasSellerSufficientReputation) {
        return OfferItemPresentationDtoFactory.create(userProfileService,
                userIdentityService,
                reputationService,
                marketPriceService,
                hasSellerSufficientReputation,
                message);
    }

    // The maker chooses the offer ID
    private static String offerIdForLog(BisqEasyOfferbookMessage message) {
        return StringUtils.sanitizeForLog(message.getBisqEasyOffer().orElseThrow().getId());
    }
}
