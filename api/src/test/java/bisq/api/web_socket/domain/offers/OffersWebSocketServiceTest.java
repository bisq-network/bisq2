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
import bisq.api.offerbook.RecordedOfferItems;
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasyService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.json.JsonMapperProvider;
import bisq.common.market.MarketRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.user.UserService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class OffersWebSocketServiceTest {
    private final BisqEasyOfferbookChannel channel = new BisqEasyOfferbookChannel(MarketRepository.getUSDBitcoinMarket());
    private final Observable<Long> offerValidityRevision = new Observable<>(0L);
    private final Map<BisqEasyOffer, Boolean> verdictByOffer = new HashMap<>();
    private final Set<BisqEasyOffer> offersWithFailingVerdict = new HashSet<>();
    // Offers whose amounts cannot be converted at the current price, so they are invalid whatever their verdict
    private final Set<BisqEasyOffer> offersWithUnresolvableAmounts = new HashSet<>();
    private final Set<String> bannedAuthors = new HashSet<>();
    private final Set<String> authorsWithoutProfile = new HashSet<>();
    private final Set<BisqEasyOfferbookMessage> messagesWithFailingItem = new HashSet<>();
    private final Set<Subscriber> subscribers = new HashSet<>();
    private final List<String> sentEvents = new ArrayList<>();
    private final ManualScheduledExecutor resendExecutor = new ManualScheduledExecutor();
    private RecordedOfferItems recordedOffers;
    // Runs after the verdict check read the verdict and before it returns it
    private Runnable onVerdictCheck = () -> {
    };
    // Runs after the subscriber got an event
    private Runnable onSend = () -> {
    };
    // Runs while the service looks up the subscribers of all markets
    private Runnable onFindSubscribers = () -> {
    };
    // Runs before the factory fails to build an item
    private Runnable onFailingItem = () -> {
    };
    // Runs before the verdict check fails
    private Runnable onFailingVerdict = () -> {
    };
    private String itemFailureMessage = "item failed";
    private String verdictFailureMessage = "verdict failed";
    // The item carries it in formattedPrice, so the test can tell a fresh item from a recorded one
    private String itemPrice = "price-1";
    private int numVerdictChecks;
    private long nanoTime;
    private MockedStatic<OfferItemPresentationDtoFactory> factory;
    private OffersWebSocketService service;

    @BeforeEach
    void setUp() {
        ObservableSet<BisqEasyOfferbookChannel> channels = new ObservableSet<>();
        channels.add(channel);
        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getBisqEasyOfferbookChannelService().getChannels()).thenReturn(channels);

        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        when(messageService.getOfferValidityRevision()).thenReturn(offerValidityRevision);
        when(messageService.hasSellerSufficientReputation(any())).thenAnswer(invocation ->
                checkVerdict(invocation.<BisqEasyOfferbookMessage>getArgument(0).getBisqEasyOffer().orElseThrow()));
        // A sell offer is valid when its maker has sufficient reputation, like the real isValid
        when(messageService.isValid(any())).thenAnswer(invocation -> {
            BisqEasyOfferbookMessage message = invocation.getArgument(0);
            BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
            return !bannedAuthors.contains(message.getAuthorUserProfileId())
                    && !offersWithUnresolvableAmounts.contains(offer)
                    && checkVerdict(offer);
        });
        when(messageService.isNotBanned(any())).thenAnswer(invocation ->
                !bannedAuthors.contains(invocation.<BisqEasyOfferbookMessage>getArgument(0).getAuthorUserProfileId()));
        when(messageService.isNotIgnored(any())).thenReturn(true);
        BisqEasyService bisqEasyService = mock(BisqEasyService.class);
        when(bisqEasyService.getBisqEasyOfferbookMessageService()).thenReturn(messageService);

        Subscriber subscriber = mock(Subscriber.class);
        when(subscriber.getTopic()).thenReturn(Topic.OFFERS);
        when(subscriber.getSubscriberId()).thenReturn("subscriber-id");
        when(subscriber.send(anyString())).thenAnswer(invocation -> {
            sentEvents.add(invocation.getArgument(0));
            onSend.run();
            return CompletableFuture.completedFuture(true);
        });
        SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
        when(subscriberRepository.findSubscribers(Topic.OFFERS, Optional.of("USD"))).thenReturn(Set.of());
        subscribers.add(subscriber);
        when(subscriberRepository.findSubscribers(Topic.OFFERS, Optional.empty())).thenAnswer(invocation -> {
            onFindSubscribers.run();
            return Set.copyOf(subscribers);
        });

        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        UserProfileService userProfileService = mock(UserProfileService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        UserProfile userProfile = mock(UserProfile.class);
        when(userProfileService.findUserProfile(anyString())).thenAnswer(invocation ->
                authorsWithoutProfile.contains(invocation.<String>getArgument(0))
                        ? Optional.empty()
                        : Optional.of(userProfile));

        // The item carries the message ID in formattedDate, so the test can tell which message was sent. Like the
        // real factory, it builds no item while the author's profile is missing.
        factory = mockStatic(OfferItemPresentationDtoFactory.class);
        factory.when(() -> OfferItemPresentationDtoFactory.create(any(), any(), any(), any(), anyBoolean(), any()))
                .thenAnswer(invocation -> {
                    BisqEasyOfferbookMessage message = invocation.getArgument(5);
                    if (authorsWithoutProfile.contains(message.getAuthorUserProfileId())) {
                        return Optional.empty();
                    }
                    if (messagesWithFailingItem.contains(message)) {
                        onFailingItem.run();
                        throw new ArithmeticException(itemFailureMessage);
                    }
                    return Optional.of(new OfferItemPresentationDto(null, false, null,
                            message.getId(),
                            null, null, itemPrice, null, null, null, null,
                            invocation.<Boolean>getArgument(4)));
                });

        BondedRolesService bondedRolesService = mock(BondedRolesService.class, RETURNS_DEEP_STUBS);
        recordedOffers = new RecordedOfferItems(userService, bondedRolesService);
        service = new OffersWebSocketService(subscriberRepository,
                chatService,
                userService,
                bondedRolesService,
                bisqEasyService,
                recordedOffers,
                resendExecutor,
                () -> nanoTime);
    }

    @AfterEach
    void tearDown() {
        factory.close();
        resendExecutor.shutdownNow();
    }

    // The reputation check of the domain, counted and controlled by the test
    private boolean checkVerdict(BisqEasyOffer offer) {
        numVerdictChecks++;
        if (offersWithFailingVerdict.contains(offer)) {
            onFailingVerdict.run();
            throw new IllegalStateException(verdictFailureMessage);
        }
        Boolean verdict = verdictByOffer.get(offer);
        onVerdictCheck.run();
        return verdict;
    }

    @Test
    void firstRevisionRunsThePassRightAway() {
        service.initialize().join();

        // The revision at registration
        assertThat(resendExecutor.delaysInNanos).containsExactly(0L);
    }

    @Test
    void revisionsWithinThirtySecondsOfAPassJoinOnePassAtTheEndOfThatWindow() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        runScheduledPass();
        sentEvents.clear();

        nanoTime += TimeUnit.SECONDS.toNanos(10);
        setVerdict(message, false);
        offerValidityRevision.set(1L);
        offerValidityRevision.set(2L);
        offerValidityRevision.set(3L);
        assertThat(sentEvents).isEmpty();
        assertThat(resendExecutor.delaysInNanos).containsExactly(0L, TimeUnit.SECONDS.toNanos(20));

        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    @Test
    void revisionThirtySecondsAfterAPassRunsThePassRightAway() {
        service.initialize().join();
        runScheduledPass();

        nanoTime += TimeUnit.SECONDS.toNanos(30);
        offerValidityRevision.set(1L);
        assertThat(resendExecutor.delaysInNanos).containsExactly(0L, 0L);
    }

    @Test
    void revisionWhileInitializeChecksAVerdictSchedulesAPass() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        // The offer becomes invalid after initialize checked it
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            setVerdict(message, false);
            offerValidityRevision.set(1L);
        };
        service.initialize().join();
        sentEvents.clear();

        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    @Test
    void windowStartsWhenThePassStarts() {
        addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        // The pass takes 10 seconds
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            nanoTime += TimeUnit.SECONDS.toNanos(10);
        };
        runScheduledPass();

        offerValidityRevision.set(1L);
        assertThat(resendExecutor.delaysInNanos).containsExactly(0L, TimeUnit.SECONDS.toNanos(20));
    }

    @Test
    void revisionDuringThePassSchedulesAnotherPass() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        // The offer becomes invalid after the pass checked it
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            setVerdict(message, false);
            offerValidityRevision.set(1L);
        };

        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    // The channel service adds and removes messages while it holds its store lock
    @Test
    void addingAnOfferDoesNotWaitForThePassToCheckAVerdict() throws Exception {
        addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        // Assertions on the adder thread would be lost
        AtomicBoolean isAdded = new AtomicBoolean();
        // Read during the check, as the adder also finishes once the pass releases the lock
        AtomicBoolean isAdderDoneDuringTheCheck = new AtomicBoolean();
        BisqEasyOfferbookMessage added = sellOffer("message-b", "offer-b", true);
        // Without a profile, onAdded stops before the factory, whose static mock does not apply on the adder thread
        when(added.getAuthorUserProfileId()).thenReturn("adder-id");
        authorsWithoutProfile.add("adder-id");
        Thread adder = new Thread(() -> isAdded.set(channel.addChatMessage(added)));
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            adder.start();
            try {
                adder.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            isAdderDoneDuringTheCheck.set(!adder.isAlive());
        };

        runScheduledPass();
        assertThat(isAdderDoneDuringTheCheck.get()).isTrue();
        assertThat(isAdded.get()).isTrue();
    }

    // Removing from inside the check creates the order of a removal on another thread
    @Test
    void offerRemovedWhileThePassChecksItsVerdictIsNotResent() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            channel.removeChatMessage(message);
        };

        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    @Test
    void passScheduledBeforeShutdownSendsNothing() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        offerValidityRevision.set(1L);

        service.shutdown().join();
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    @Test
    void offerIsRemovedAndAddedOnlyWhenItsValidityFlips() throws Exception {
        BisqEasyOfferbookMessage messageA = addSellOffer("message-a", "offer-a", true);
        addSellOffer("message-b", "offer-b", true);
        service.initialize().join();
        sentEvents.clear();

        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        setVerdict(messageA, false);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");

        offerValidityRevision.set(3L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);

        setVerdict(messageA, true);
        offerValidityRevision.set(4L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", "message-a", true);
    }

    @Test
    void offerAddedAfterInitializeIsRemovedWhenItBecomesInvalid() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentType(sentEvents.get(1), "REMOVED", "message-a");
    }

    // Like the desktop offerbook, which hides sell offers whose maker lacks the reputation
    @Test
    void invalidOfferOfAnotherMakerIsNotSent() {
        service.initialize().join();
        addSellOffer("message-a", "offer-a", false);
        assertThat(sentEvents).isEmpty();
        assertThat(recordedOffers.size()).isZero();
    }

    @Test
    void offerThatBecomesValidIsAdded() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", false);
        service.initialize().join();
        assertThat(sentEvents).isEmpty();

        setVerdict(message, true);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);
    }

    // The app warns its user, so an own offer is sent also when it is invalid
    @Test
    void ownOfferWithInsufficientReputationIsSentWithFalse() throws Exception {
        service.initialize().join();
        addOwnSellOffer("message-a", "offer-a", false);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
    }

    @Test
    void ownOfferVerdictFlipSendsAdded() throws Exception {
        BisqEasyOfferbookMessage message = addOwnSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);

        setVerdict(message, true);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", "message-a", true);
    }

    // Without the price, the item of an own offer shows no amounts, so the item is sent again once it is valid
    @Test
    void ownOfferSentWithoutPriceIsResentWhenItBecomesValid() throws Exception {
        BisqEasyOfferbookMessage message = addOwnSellOffer("message-a", "offer-a", true);
        offersWithUnresolvableAmounts.add(message.getBisqEasyOffer().orElseThrow());
        service.initialize().join();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);

        offersWithUnresolvableAmounts.clear();
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", "message-a", true);

        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
    }

    // The ban check needs no profile; without the profile, no other check is made
    @Test
    void offerOfABannedMakerWithoutProfileIsRemoved() throws Exception {
        addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        authorsWithoutProfile.add("maker-id");
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        bannedAuthors.add("maker-id");
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    @Test
    void removedOfferIsNotResentWhenItsVerdictFlips() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        channel.removeChatMessage(message);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "REMOVED", "message-a", true);

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
    }

    // The maker chooses the offer ID, so two makers can publish offers with the same ID.
    @Test
    void messagesWithTheSameOfferIdKeepTheirOwnVisibility() throws Exception {
        BisqEasyOfferbookMessage messageA = addSellOffer("message-a", "offer-x", true);
        BisqEasyOfferbookMessage messageB = addSellOffer("message-b", "offer-x", false);
        service.initialize().join();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);
        sentEvents.clear();

        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(messageB);
        assertThat(sentEvents).isEmpty();
        setVerdict(messageA, false);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    // The author chooses the message ID, so two authors can publish messages with the same ID.
    @Test
    void messagesWithTheSameMessageIdKeepTheirOwnVisibility() throws Exception {
        addSellOffer("message-x", "offer-a", true);
        BisqEasyOfferbookMessage messageB = addSellOffer("message-x", "offer-b", false);
        service.initialize().join();
        sentEvents.clear();

        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(messageB);
        sentEvents.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    // Without the profile, the check would use a score of 0 and no item can be built, so clients keep the offer
    @Test
    void visibilityIsNotCheckedWhileTheMakerProfileIsMissing() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        authorsWithoutProfile.add("maker-id");
        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        authorsWithoutProfile.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    // The check ran with the profile, so its result holds. Without the profile no item is built, so a result which
    // shows the offer cannot record it either.
    @Test
    void visibilityCheckedWithTheProfileIsAppliedWhenTheProfileIsRemovedDuringTheCheck() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        onVerdictCheck = () -> {
            onVerdictCheck = () -> {
            };
            authorsWithoutProfile.add("maker-id");
        };
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");

        authorsWithoutProfile.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
    }

    @Test
    void offerAddedWhileItsMakerProfileIsMissingIsSentByThePassAfterTheProfileArrives() throws Exception {
        service.initialize().join();
        authorsWithoutProfile.add("maker-id");
        addSellOffer("message-a", "offer-a", true);
        assertThat(sentEvents).isEmpty();

        authorsWithoutProfile.clear();
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);
    }

    // Released apps parse the payload of a removal as a list of items, so the removal carries the item they hold
    @Test
    void offerThatBecomesInvalidIsRemovedWithItsLastItem() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        messagesWithFailingItem.add(message);
        itemPrice = "price-2";
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        JsonNode item = assertSentType(sentEvents.get(0), "REMOVED", "message-a");
        assertThat(item.get("formattedPrice").asText()).isEqualTo("price-1");
        assertThat(recordedOffers.size()).isZero();
    }

    @Test
    void ownOfferVerdictFlipWhoseItemFailsIsSentByTheNextPass() throws Exception {
        BisqEasyOfferbookMessage message = addOwnSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        messagesWithFailingItem.add(message);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        messagesWithFailingItem.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
    }

    @Test
    void offerWhoseItemFailedWhenAddedIsSentOnceItCanBeBuilt() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        messagesWithFailingItem.add(message);
        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();

        messagesWithFailingItem.clear();
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);
    }

    // A subscriber which registers after the lookup reads its snapshot after the record changed
    @Test
    void subscriberRegisteredDuringThePassDoesNotGetTheRemovedOfferInItsSnapshot() throws Exception {
        Set<Subscriber> laterSubscribers = Set.copyOf(subscribers);
        subscribers.clear();
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        setVerdict(message, false);
        AtomicReference<String> snapshot = new AtomicReference<>();
        onFindSubscribers = () -> {
            onFindSubscribers = () -> {
            };
            snapshot.set(service.getJsonPayload().orElseThrow());
        };
        offerValidityRevision.set(1L);
        runScheduledPass();

        assertThat(snapshot.get()).isEqualTo("[]");

        subscribers.addAll(laterSubscribers);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    // The pending pass compares with the recorded verdict, so the snapshot must send that one
    @Test
    void snapshotUsesTheRecordedVerdictWhileAnOwnOfferFlipIsPending() throws Exception {
        BisqEasyOfferbookMessage message = addOwnSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        setVerdict(message, false);
        messagesWithFailingItem.add(message);
        AtomicReference<String> snapshot = new AtomicReference<>();
        onFailingItem = () -> {
            onFailingItem = () -> {
            };
            messagesWithFailingItem.clear();
            snapshot.set(service.getJsonPayload().orElseThrow());
        };
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
        assertThat(snapshotItem(snapshot.get(), "message-a").get("hasSellerSufficientReputation").asBoolean()).isTrue();

        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
        assertThat(clientVerdict(snapshot.get(), "message-a")).isFalse();
    }

    // A fresh item carries the current amounts and reputation; the recorded one serves when none can be built
    @Test
    void snapshotUsesTheRecordedItemWhenTheItemFails() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        itemPrice = "price-2";

        messagesWithFailingItem.add(message);
        String snapshot = service.getJsonPayload().orElseThrow();
        assertThat(snapshotItem(snapshot, "message-a").get("formattedPrice").asText()).isEqualTo("price-1");

        messagesWithFailingItem.clear();
        snapshot = service.getJsonPayload().orElseThrow();
        assertThat(snapshotItem(snapshot, "message-a").get("formattedPrice").asText()).isEqualTo("price-2");

        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    @Test
    void snapshotUsesTheRecordedItemWhenTheMakerProfileIsMissing() throws Exception {
        addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        itemPrice = "price-2";

        authorsWithoutProfile.add("maker-id");
        String snapshot = service.getJsonPayload().orElseThrow();
        assertThat(snapshotItem(snapshot, "message-a").get("formattedPrice").asText()).isEqualTo("price-1");
    }


    // The pass retries a failing item until it can be built, so only the first failure is a warning
    @Test
    void failingItemLogsOneWarning() {
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        messagesWithFailingItem.add(message);
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            assertThat(channel.addChatMessage(message)).isTrue();
            offerValidityRevision.set(1L);
            runScheduledPass();
            offerValidityRevision.set(2L);
            runScheduledPass();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(sentEvents).isEmpty();
        assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getThrowableProxy()).isNull();
                    assertThat(event.getFormattedMessage()).contains("java.lang.ArithmeticException");
                });
    }

    // The exception message can carry text of the maker's offer, so a line break in it could forge a log line
    @Test
    void logMessagesOfAFailingItemHaveNoLineBreak() {
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        messagesWithFailingItem.add(message);
        itemFailureMessage = "item\r\nFORGED failed";
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            assertThat(channel.addChatMessage(message)).isTrue();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN).hasSize(1);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(logMessage -> assertThat(logMessage).doesNotContain("\n", "\r"));
    }


    // Removing the offer forgets its failure, so that the set does not grow with removed offers
    @Test
    void offerAddedAgainAfterItsRemovalWarnsAgainWhenItsItemFails() {
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        messagesWithFailingItem.add(message);
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            assertThat(channel.addChatMessage(message)).isTrue();
            channel.removeChatMessage(message);
            assertThat(channel.addChatMessage(message)).isTrue();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                .filteredOn(event -> event.getFormattedMessage().startsWith("Could not build the item"))
                .hasSize(2);
    }

    // The pass retries a failing check until it passes, so only the first failure is a warning
    @Test
    void failingVisibilityCheckLogsOneWarning() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            offerValidityRevision.set(1L);
            runScheduledPass();
            offerValidityRevision.set(2L);
            runScheduledPass();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(sentEvents).isEmpty();
        assertThat(appender.list).filteredOn(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getThrowableProxy()).isNull();
                    assertThat(event.getFormattedMessage()).contains("java.lang.IllegalStateException");
                });
    }

    // The maker chooses the offer ID, and the exception message can carry text of the offer, so a line break in
    // them could forge a log line
    @Test
    void logMessagesOfAFailingVisibilityCheckHaveNoLineBreak() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a\nFORGED offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        verdictFailureMessage = "verdict\r\nFORGED failed";
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        Level level = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN).hasSize(1);
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                .allSatisfy(logMessage -> assertThat(logMessage).doesNotContain("\n", "\r"));
    }

    // Removing the offer forgets its failure, so that the set does not grow with removed offers
    @Test
    void offerAddedAgainAfterItsRemovalWarnsAgainWhenItsVisibilityCheckFails() {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(channel.addChatMessage(message)).isTrue();
            channel.removeChatMessage(message);
            assertThat(channel.addChatMessage(message)).isTrue();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                .filteredOn(event -> event.getFormattedMessage().startsWith("Could not check the visibility"))
                .hasSize(2);
    }

    // A check which passed forgets the failure, so that a later failure warns again
    @Test
    void visibilityCheckWhichFailsAgainAfterItPassedWarnsAgain() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        BisqEasyOffer offer = message.getBisqEasyOffer().orElseThrow();
        offersWithFailingVerdict.add(offer);
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            offersWithFailingVerdict.clear();
            offerValidityRevision.set(1L);
            runScheduledPass();
            offersWithFailingVerdict.add(offer);
            offerValidityRevision.set(2L);
            runScheduledPass();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(sentEvents).hasSize(1);
        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                .filteredOn(event -> event.getFormattedMessage().startsWith("Could not check the visibility"))
                .hasSize(2);
    }

    // The pass checks outside the lock, so the offer can leave the channel during a check which fails. The failure of
    // a removed offer is no warning, and it is forgotten, so that the offer warns again when it is added again.
    @Test
    void offerRemovedDuringItsFailingCheckIsNotWarnedAbout() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        onFailingVerdict = () -> {
            onFailingVerdict = () -> {
            };
            channel.removeChatMessage(message);
        };
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            offerValidityRevision.set(1L);
            runScheduledPass();
            assertThat(sentEvents).hasSize(1);
            assertSentType(sentEvents.get(0), "REMOVED", "message-a");
            assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN).isEmpty();

            assertThat(channel.addChatMessage(message)).isTrue();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                .filteredOn(event -> event.getFormattedMessage().startsWith("Could not check the visibility"))
                .hasSize(1);
    }

    // A check which is skipped while the maker profile is missing did not pass, so the failure stays known
    @Test
    void failingCheckWarnsOnceWhenTheProfileIsMissingInBetween() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            service.initialize().join();
            authorsWithoutProfile.add("maker-id");
            offerValidityRevision.set(1L);
            runScheduledPass();
            authorsWithoutProfile.clear();
            offerValidityRevision.set(2L);
            runScheduledPass();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(sentEvents).isEmpty();
        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.WARN)
                .filteredOn(event -> event.getFormattedMessage().startsWith("Could not check the visibility"))
                .hasSize(1);
    }

    // Logs get pasted into bug reports, so the error names the subscriber and the size of the payload, not the offer
    @Test
    void failedSendLogsTheSubscriberAndTheJsonLengthWithoutThePayload() {
        onSend = () -> {
            throw new IllegalStateException("send failed");
        };
        Logger logger = (Logger) LoggerFactory.getLogger(OffersWebSocketService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            addSellOffer("message-a", "offer-a", true);
            service.initialize().join();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(sentEvents).hasSize(1);
        assertThat(appender.list).filteredOn(event -> event.getLevel() == Level.ERROR)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getFormattedMessage()).contains("subscriber-id");
                    assertThat(event.getFormattedMessage()).containsPattern("jsonLength=[1-9][0-9]*");
                    assertThat(event.getFormattedMessage()).doesNotContain("message-a", "formattedPrice");
                });
    }

    // The snapshot of the first subscriber carries the item which was recorded without subscribers
    @Test
    void offerRecordedWithoutSubscribersIsNotResentToALaterSubscriber() {
        Set<Subscriber> laterSubscribers = Set.copyOf(subscribers);
        subscribers.clear();
        addSellOffer("message-a", "offer-a", true);
        service.initialize().join();

        subscribers.addAll(laterSubscribers);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    @Test
    void failingCheckSkipsOnlyItsOfferAndIsRetriedAtTheNextRevision() throws Exception {
        addSellOffer("message-a", "offer-a", true);
        addSellOffer("message-b", "offer-b", true);
        // The offer which the service visits first fails, so that stopping at the failure would skip the other one
        List<BisqEasyOfferbookMessage> inIterationOrder = channel.getBisqEasyOfferbookMessagesWithOffer().toList();
        BisqEasyOfferbookMessage failing = inIterationOrder.get(0);
        BisqEasyOfferbookMessage other = inIterationOrder.get(1);
        offersWithFailingVerdict.add(failing.getBisqEasyOffer().orElseThrow());

        service.initialize().join();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", other.getId(), true);

        setVerdict(other, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentType(sentEvents.get(1), "REMOVED", other.getId());

        offersWithFailingVerdict.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(3);
        assertSentItem(sentEvents.get(2), "ADDED", failing.getId(), true);
    }

    // No client holds the offer. A check would put the removed offer into the reputation service's cache.
    @Test
    void removalOfAnUnrecordedOfferSendsNothing() {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();

        offersWithFailingVerdict.clear();
        int numVerdictChecksBeforeRemoval = numVerdictChecks;
        channel.removeChatMessage(message);
        assertThat(sentEvents).isEmpty();
        assertThat(numVerdictChecks).isEqualTo(numVerdictChecksBeforeRemoval);
    }

    @Test
    void failingVisibilityCheckKeepsTheRecord() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
        assertThat(recordedOffers.size()).isEqualTo(1);
        snapshotItem(service.getJsonPayload().orElseThrow(), "message-a");
    }

    // The pending pass compares with the cached verdict, so the snapshot must send that one
    @Test
    void snapshotTakenWhileAFlipIsPendingEndsWithTheCurrentVerdict() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        String snapshot = service.getJsonPayload().orElseThrow();
        setVerdict(message, true);
        offerValidityRevision.set(2L);
        runScheduledPass();

        assertThat(clientVerdict(snapshot, "message-a")).isTrue();
    }

    @Test
    void snapshotDoesNotHideAPendingFlipFromEarlierSubscribers() throws Exception {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        service.getJsonPayload().orElseThrow();
        runScheduledPass();

        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
    }

    // An offer which arrived before its maker's profile is recorded by the next pass
    @Test
    void snapshotContainsOnlyRecordedOffers() throws Exception {
        service.initialize().join();
        authorsWithoutProfile.add("maker-id");
        addSellOffer("message-a", "offer-a", true);
        authorsWithoutProfile.clear();
        assertThat(service.getJsonPayload().orElseThrow()).isEqualTo("[]");

        offerValidityRevision.set(1L);
        runScheduledPass();
        snapshotItem(service.getJsonPayload().orElseThrow(), "message-a");
    }

    @Test
    void removalWaitsForAResendInProgress() throws Exception {
        BisqEasyOfferbookMessage resent = addSellOffer("message-a", "offer-a", true);
        // Without a profile, the real factory builds no item. The static mock does not apply on the remover thread.
        BisqEasyOfferbookMessage removed = sellOffer("message-b", "offer-b", true);
        when(removed.getAuthorUserProfileId()).thenReturn("remover-id");
        authorsWithoutProfile.add("remover-id");
        assertThat(channel.addChatMessage(removed)).isTrue();
        service.initialize().join();
        sentEvents.clear();

        setVerdict(resent, false);
        Thread remover = new Thread(() -> channel.removeChatMessage(removed));
        AtomicReference<Thread.State> removerStateDuringTheResend = new AtomicReference<>();
        onSend = () -> {
            onSend = () -> {
            };
            remover.start();
            removerStateDuringTheResend.set(awaitBlockedOrTerminated(remover));
        };

        runScheduledPass();
        remover.join(2000);
        assertThat(removerStateDuringTheResend.get()).isEqualTo(Thread.State.BLOCKED);
        assertThat(remover.isAlive()).isFalse();
    }

    @Test
    void removedOfferLeavesNoRecordedItem() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        assertThat(recordedOffers.size()).isEqualTo(1);

        channel.removeChatMessage(message);
        assertThat(recordedOffers.size()).isZero();
    }


    // At initialize, onAllAdded runs without the channel store lock, so the removal can be handled first. An observer
    // which removes the message from inside onAdded creates the same order on one thread.
    @Test
    void offerRemovedBeforeItsAdditionIsHandledIsNotAdded() throws Exception {
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void onAdded(BisqEasyOfferbookMessage added) {
                if (added == message) {
                    channel.removeChatMessage(message);
                }
            }

            @Override
            public void onRemoved(Object element) {
            }

            @Override
            public void onCleared() {
            }
        });
        service.initialize().join();

        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();
        assertThat(recordedOffers.size()).isZero();
        assertThat(numVerdictChecks).isZero();
    }

    // Shutdown can stop the executor while the revision observer runs
    @Test
    void revisionAfterTheExecutorStoppedCausesNoObserverError() {
        service.initialize().join();
        runScheduledPass();
        resendExecutor.shutdownNow();

        Logger logger = (Logger) LoggerFactory.getLogger(Observable.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            offerValidityRevision.set(1L);
        } finally {
            logger.detachAppender(appender);
        }
        assertThat(appender.list).noneMatch(event -> event.getThrowableProxy() != null &&
                event.getThrowableProxy().getClassName().equals(RejectedExecutionException.class.getName()));
    }

    @Test
    void nothingIsResentAfterShutdown() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        service.shutdown().join();

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();
    }

    // Clients treat a market without a count as unknown
    @Test
    void marketWithoutOffersCountsZero() {
        service.initialize().join();
        assertThat(numOffers()).isEqualTo(Map.of("USD", 0));
    }

    // Like OFFERS, unlike the desktop offerbook
    @Test
    void countIncludesOwnInsufficientOffers() {
        service.initialize().join();
        addOwnSellOffer("message-a", "offer-a", false);
        addSellOffer("message-b", "offer-b", false);
        assertThat(numOffers()).isEqualTo(Map.of("USD", 1));
    }

    // Each count sends the whole map to every NUM_OFFERS subscriber
    @Test
    void passPublishesEachMarketCountOnce() {
        BisqEasyOfferbookMessage messageA = addSellOffer("message-a", "offer-a", true);
        BisqEasyOfferbookMessage messageB = addSellOffer("message-b", "offer-b", true);
        service.initialize().join();
        List<Integer> publishedCounts = new ArrayList<>();
        recordedOffers.getNumOffersByQuoteCurrencyCode().addObserver(() ->
                publishedCounts.add(recordedOffers.getNumOffersByQuoteCurrencyCode().get("USD")));
        publishedCounts.clear();

        setVerdict(messageA, false);
        setVerdict(messageB, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(publishedCounts).containsExactly(0);
    }

    // A subscriber can be registered before the existing offers are recorded
    @Test
    void initializePublishesEachMarketCountOnce() {
        addSellOffer("message-a", "offer-a", true);
        addSellOffer("message-b", "offer-b", true);
        addSellOffer("message-c", "offer-c", true);
        List<Integer> publishedCounts = new ArrayList<>();
        recordedOffers.getNumOffersByQuoteCurrencyCode().addObserver(() ->
                publishedCounts.add(recordedOffers.getNumOffersByQuoteCurrencyCode().get("USD")));
        publishedCounts.clear();

        service.initialize().join();
        assertThat(publishedCounts).containsExactly(3);
    }

    // The maker chooses the market of the offer, so the count follows the channel
    @Test
    void countFollowsTheChannelMarketNotTheOfferMarket() {
        service.initialize().join();
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        assertThat(numOffers()).isEqualTo(Map.of("USD", 1));

        channel.removeChatMessage(message);
        assertThat(numOffers()).isEqualTo(Map.of("USD", 0));
    }

    @Test
    void numOffersEqualsTheOffersSentPerMarket() throws Exception {
        addSellOffer("message-a", "offer-a", true);
        BisqEasyOfferbookMessage invalid = addSellOffer("message-b", "offer-b", false);
        BisqEasyOfferbookMessage own = addOwnSellOffer("message-c", "offer-c", false);
        service.initialize().join();
        assertThat(clientOffers()).containsExactlyInAnyOrder("message-a", "message-c");
        assertThat(numOffers()).isEqualTo(Map.of("USD", 2));

        setVerdict(invalid, true);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(clientOffers()).containsExactlyInAnyOrder("message-a", "message-b", "message-c");
        assertThat(numOffers()).isEqualTo(Map.of("USD", 3));

        // A replaced item is not counted twice
        setVerdict(own, true);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(clientOffers()).containsExactlyInAnyOrder("message-a", "message-b", "message-c");
        assertThat(numOffers()).isEqualTo(Map.of("USD", 3));

        channel.removeChatMessage(own);
        assertThat(clientOffers()).containsExactlyInAnyOrder("message-a", "message-b");
        assertThat(numOffers()).isEqualTo(Map.of("USD", 2));
    }

    private BisqEasyOfferbookMessage addSellOffer(String messageId, String offerId, boolean hasSellerSufficientReputation) {
        BisqEasyOfferbookMessage message = sellOffer(messageId, offerId, hasSellerSufficientReputation);
        assertThat(channel.addChatMessage(message)).isTrue();
        return message;
    }

    private BisqEasyOfferbookMessage sellOffer(String messageId, String offerId, boolean hasSellerSufficientReputation) {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getId()).thenReturn(offerId);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        verdictByOffer.put(offer, hasSellerSufficientReputation);
        BisqEasyOfferbookMessage message = mock(BisqEasyOfferbookMessage.class);
        when(message.getId()).thenReturn(messageId);
        when(message.getAuthorUserProfileId()).thenReturn("maker-id");
        when(message.hasBisqEasyOffer()).thenReturn(true);
        when(message.getBisqEasyOffer()).thenReturn(Optional.of(offer));
        // A channel holds only messages of supported types
        when(message.hasSupportedChatMessageType()).thenReturn(true);
        return message;
    }

    private BisqEasyOfferbookMessage addOwnSellOffer(String messageId, String offerId, boolean hasSellerSufficientReputation) {
        BisqEasyOfferbookMessage message = ownSellOffer(messageId, offerId, hasSellerSufficientReputation);
        assertThat(channel.addChatMessage(message)).isTrue();
        return message;
    }

    private BisqEasyOfferbookMessage ownSellOffer(String messageId, String offerId, boolean hasSellerSufficientReputation) {
        BisqEasyOfferbookMessage message = sellOffer(messageId, offerId, hasSellerSufficientReputation);
        when(message.isMyMessage(any())).thenReturn(true);
        return message;
    }

    private void setVerdict(BisqEasyOfferbookMessage message, boolean hasSellerSufficientReputation) {
        verdictByOffer.put(message.getBisqEasyOffer().orElseThrow(), hasSellerSufficientReputation);
    }

    private void runScheduledPass() {
        resendExecutor.runScheduledTasks();
    }

    private static Thread.State awaitBlockedOrTerminated(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Thread.State state = thread.getState();
        while (state != Thread.State.BLOCKED && state != Thread.State.TERMINATED && System.nanoTime() < deadline) {
            Thread.onSpinWait();
            state = thread.getState();
        }
        return state;
    }

    // Runs a scheduled task only when the test asks for it, so that no test waits for the delay
    private static class ManualScheduledExecutor extends ScheduledThreadPoolExecutor {
        private final List<Runnable> tasks = new ArrayList<>();
        private final List<Long> delaysInNanos = new ArrayList<>();

        ManualScheduledExecutor() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            if (isShutdown()) {
                throw new RejectedExecutionException("Executor is shut down");
            }
            tasks.add(command);
            delaysInNanos.add(unit.toNanos(delay));
            return null;
        }

        @Override
        public List<Runnable> shutdownNow() {
            tasks.clear();
            return super.shutdownNow();
        }

        // A task which a running task schedules waits for the next call
        void runScheduledTasks() {
            List<Runnable> dueTasks = new ArrayList<>(tasks);
            tasks.clear();
            dueTasks.forEach(Runnable::run);
        }
    }

    private static void assertSentItem(String event,
                                       String modificationType,
                                       String messageId,
                                       boolean hasSellerSufficientReputation) throws Exception {
        JsonNode item = assertSentType(event, modificationType, messageId);
        assertThat(item.get("hasSellerSufficientReputation").asBoolean()).isEqualTo(hasSellerSufficientReputation);
    }

    private Map<String, Integer> numOffers() {
        return new HashMap<>(recordedOffers.getNumOffersByQuoteCurrencyCode());
    }

    // The offers a subscriber holds after the events it received
    private Set<String> clientOffers() throws Exception {
        Set<String> offers = new HashSet<>();
        for (String event : sentEvents) {
            JsonNode envelope = JsonMapperProvider.get().readTree(event);
            for (JsonNode item : JsonMapperProvider.get().readTree(envelope.get("payload").asText())) {
                String messageId = item.get("formattedDate").asText();
                if (envelope.get("modificationType").asText().equals("REMOVED")) {
                    offers.remove(messageId);
                } else {
                    offers.add(messageId);
                }
            }
        }
        return offers;
    }

    private static JsonNode snapshotItem(String snapshot, String messageId) throws Exception {
        JsonNode snapshotItem = null;
        for (JsonNode item : JsonMapperProvider.get().readTree(snapshot)) {
            if (item.get("formattedDate").asText().equals(messageId)) {
                snapshotItem = item;
            }
        }
        assertThat(snapshotItem).isNotNull();
        return snapshotItem;
    }

    // The verdict the subscriber holds after its snapshot and the events since
    private boolean clientVerdict(String snapshot, String messageId) throws Exception {
        boolean verdict = snapshotItem(snapshot, messageId).get("hasSellerSufficientReputation").asBoolean();
        for (String event : sentEvents) {
            JsonNode item = assertSentType(event, "ADDED", messageId);
            verdict = item.get("hasSellerSufficientReputation").asBoolean();
        }
        return verdict;
    }

    private static JsonNode assertSentType(String event, String modificationType, String messageId) throws Exception {
        JsonNode envelope = JsonMapperProvider.get().readTree(event);
        assertThat(envelope.get("modificationType").asText()).isEqualTo(modificationType);
        JsonNode items = JsonMapperProvider.get().readTree(envelope.get("payload").asText());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("formattedDate").asText()).isEqualTo(messageId);
        return items.get(0);
    }
}
