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
import bisq.api.web_socket.subscription.Subscriber;
import bisq.api.web_socket.subscription.SubscriberRepository;
import bisq.api.web_socket.subscription.Topic;
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bisq_easy.BisqEasySellersReputationBasedTradeAmountService;
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
    private final Set<String> authorsWithoutProfile = new HashSet<>();
    private final Set<Subscriber> subscribers = new HashSet<>();
    private final List<String> sentEvents = new ArrayList<>();
    private final ManualScheduledExecutor resendExecutor = new ManualScheduledExecutor();
    // Runs after the verdict check read the verdict and before it returns it
    private Runnable onVerdictCheck = () -> {
    };
    // Runs after the subscriber got an event
    private Runnable onSend = () -> {
    };
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

        BisqEasySellersReputationBasedTradeAmountService sellersReputationService =
                mock(BisqEasySellersReputationBasedTradeAmountService.class);
        when(sellersReputationService.hasSellerSufficientReputation(any(BisqEasyOffer.class)))
                .thenAnswer(invocation -> {
                    BisqEasyOffer offer = invocation.getArgument(0);
                    numVerdictChecks++;
                    if (offersWithFailingVerdict.contains(offer)) {
                        throw new IllegalStateException("verdict failed");
                    }
                    Boolean verdict = verdictByOffer.get(offer);
                    onVerdictCheck.run();
                    return verdict;
                });
        BisqEasyOfferbookMessageService messageService = mock(BisqEasyOfferbookMessageService.class);
        when(messageService.getOfferValidityRevision()).thenReturn(offerValidityRevision);
        BisqEasyService bisqEasyService = mock(BisqEasyService.class);
        when(bisqEasyService.getBisqEasySellersReputationBasedTradeAmountService()).thenReturn(sellersReputationService);
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
        when(subscriberRepository.findSubscribers(Topic.OFFERS, Optional.empty())).thenAnswer(invocation -> Set.copyOf(subscribers));

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
                    return Optional.of(new OfferItemPresentationDto(null, false, null,
                            message.getId(),
                            null, null, null, null, null, null, null,
                            invocation.<Boolean>getArgument(4)));
                });

        service = new OffersWebSocketService(subscriberRepository,
                chatService,
                userService,
                mock(BondedRolesService.class, RETURNS_DEEP_STUBS),
                bisqEasyService,
                resendExecutor,
                () -> nanoTime);
    }

    @AfterEach
    void tearDown() {
        factory.close();
        resendExecutor.shutdownNow();
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
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
        // The verdict flips after initialize read it
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
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
        // The verdict flips after the pass read it
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
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
    void offerIsResentOnlyWhenItsReputationVerdictFlips() throws Exception {
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);

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
    void offerAddedAfterInitializeIsResentWhenItsVerdictFlips() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);

        setVerdict(message, false);
        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", "message-a", false);
    }

    @Test
    void offerAddedWithInsufficientReputationIsSentWithFalse() throws Exception {
        service.initialize().join();
        addSellOffer("message-a", "offer-a", false);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
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
    void messagesWithTheSameOfferIdKeepTheirOwnVerdicts() throws Exception {
        BisqEasyOfferbookMessage messageA = addSellOffer("message-a", "offer-x", true);
        BisqEasyOfferbookMessage messageB = addSellOffer("message-b", "offer-x", false);
        service.initialize().join();
        sentEvents.clear();

        offerValidityRevision.set(1L);
        runScheduledPass();
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(messageB);
        sentEvents.clear();
        setVerdict(messageA, false);
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
    }

    // The author chooses the message ID, so two authors can publish messages with the same ID.
    @Test
    void messagesWithTheSameMessageIdKeepTheirOwnVerdicts() throws Exception {
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

    // Without the profile, the verdict uses a score of 0 and no item is sent, so clients keep the previous verdict.
    @Test
    void verdictIsNotRecordedWhileTheMakerProfileIsMissing() throws Exception {
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
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

    // The snapshot of the first subscriber carries the verdict which was recorded without subscribers
    @Test
    void verdictCheckedWithoutSubscribersIsNotResentToALaterSubscriber() {
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
    void failingVerdictSkipsOnlyItsOfferAndIsRetriedAtTheNextRevision() throws Exception {
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
        assertSentItem(sentEvents.get(1), "ADDED", other.getId(), false);

        offersWithFailingVerdict.clear();
        offerValidityRevision.set(2L);
        runScheduledPass();
        assertThat(sentEvents).hasSize(3);
        assertSentItem(sentEvents.get(2), "ADDED", failing.getId(), true);
    }

    // A check would put the removed offer into the reputation service's cache
    @Test
    void offerWhoseVerdictFailedWhenAddedIsRemovedWithoutACheck() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();

        offersWithFailingVerdict.clear();
        int numVerdictChecksBeforeRemoval = numVerdictChecks;
        channel.removeChatMessage(message);
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
        assertThat(numVerdictChecks).isEqualTo(numVerdictChecksBeforeRemoval);
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
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
    }

    // Only the next pass records the verdict of an offer which arrived before its maker's profile
    @Test
    void snapshotIncludesOfferWhoseProfileArrivedAfterIt() throws Exception {
        service.initialize().join();
        authorsWithoutProfile.add("maker-id");
        addSellOffer("message-a", "offer-a", false);
        authorsWithoutProfile.clear();

        String snapshot = service.getJsonPayload().orElseThrow();
        assertThat(clientVerdict(snapshot, "message-a")).isFalse();
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
    void removedOfferLeavesNoRecordedVerdict() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        assertThat(service.getNumSellerReputationVerdicts()).isEqualTo(1);

        channel.removeChatMessage(message);
        assertThat(service.getNumSellerReputationVerdicts()).isZero();
    }

    // Clients remove by offer ID and do not read the verdict of a removed item.
    @Test
    void offerIsRemovedAlthoughItsVerdictCannotBeChecked() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(message);
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
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
        assertThat(sentEvents).hasSize(1);
        assertSentType(sentEvents.get(0), "REMOVED", "message-a");
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

    // The verdict the subscriber holds after its snapshot and the events since
    private boolean clientVerdict(String snapshot, String messageId) throws Exception {
        JsonNode snapshotItem = null;
        for (JsonNode item : JsonMapperProvider.get().readTree(snapshot)) {
            if (item.get("formattedDate").asText().equals(messageId)) {
                snapshotItem = item;
            }
        }
        assertThat(snapshotItem).isNotNull();
        boolean verdict = snapshotItem.get("hasSellerSufficientReputation").asBoolean();
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
