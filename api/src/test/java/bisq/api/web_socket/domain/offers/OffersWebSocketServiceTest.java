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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
    private final List<String> sentEvents = new ArrayList<>();
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
                    if (offersWithFailingVerdict.contains(offer)) {
                        throw new IllegalStateException("verdict failed");
                    }
                    return verdictByOffer.get(offer);
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
            return CompletableFuture.completedFuture(true);
        });
        SubscriberRepository subscriberRepository = mock(SubscriberRepository.class);
        when(subscriberRepository.findSubscribers(Topic.OFFERS, Optional.of("USD"))).thenReturn(Set.of());
        when(subscriberRepository.findSubscribers(Topic.OFFERS, Optional.empty())).thenReturn(Set.of(subscriber));

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
                bisqEasyService);
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void offerIsResentOnlyWhenItsReputationVerdictFlips() throws Exception {
        BisqEasyOfferbookMessage messageA = addSellOffer("message-a", "offer-a", true);
        addSellOffer("message-b", "offer-b", true);
        service.initialize().join();
        sentEvents.clear();

        offerValidityRevision.set(1L);
        assertThat(sentEvents).isEmpty();

        setVerdict(messageA, false);
        offerValidityRevision.set(2L);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);

        offerValidityRevision.set(3L);
        assertThat(sentEvents).hasSize(1);

        setVerdict(messageA, true);
        offerValidityRevision.set(4L);
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
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", "message-a", false);
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
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(messageB);
        sentEvents.clear();
        setVerdict(messageA, false);
        offerValidityRevision.set(2L);
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
        assertThat(sentEvents).isEmpty();

        channel.removeChatMessage(messageB);
        sentEvents.clear();
        offerValidityRevision.set(2L);
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
        assertThat(sentEvents).isEmpty();

        authorsWithoutProfile.clear();
        offerValidityRevision.set(2L);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", false);
    }

    @Test
    void offerAddedWhileItsMakerProfileIsMissingIsSentOnceTheProfileIsThere() throws Exception {
        service.initialize().join();
        authorsWithoutProfile.add("maker-id");
        addSellOffer("message-a", "offer-a", true);
        assertThat(sentEvents).isEmpty();

        authorsWithoutProfile.clear();
        offerValidityRevision.set(1L);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "ADDED", "message-a", true);
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
        assertThat(sentEvents).hasSize(2);
        assertSentItem(sentEvents.get(1), "ADDED", other.getId(), false);

        offersWithFailingVerdict.clear();
        offerValidityRevision.set(2L);
        assertThat(sentEvents).hasSize(3);
        assertSentItem(sentEvents.get(2), "ADDED", failing.getId(), true);
    }

    // A subscriber can have received the offer in its initial payload, which checks the verdict again.
    @Test
    void offerWhoseVerdictFailedWhenAddedIsStillRemoved() throws Exception {
        service.initialize().join();
        BisqEasyOfferbookMessage message = sellOffer("message-a", "offer-a", true);
        offersWithFailingVerdict.add(message.getBisqEasyOffer().orElseThrow());
        assertThat(channel.addChatMessage(message)).isTrue();
        assertThat(sentEvents).isEmpty();

        offersWithFailingVerdict.clear();
        channel.removeChatMessage(message);
        assertThat(sentEvents).hasSize(1);
        assertSentItem(sentEvents.get(0), "REMOVED", "message-a", true);
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

    // The set changes before its observers run, so on another thread the removal can be handled before the addition.
    // An observer which removes the message from inside onAdded creates the same order on one thread.
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

    @Test
    void nothingIsResentAfterShutdown() {
        BisqEasyOfferbookMessage message = addSellOffer("message-a", "offer-a", true);
        service.initialize().join();
        sentEvents.clear();
        service.shutdown().join();

        setVerdict(message, false);
        offerValidityRevision.set(1L);
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

    private static void assertSentItem(String event,
                                       String modificationType,
                                       String messageId,
                                       boolean hasSellerSufficientReputation) throws Exception {
        JsonNode item = assertSentType(event, modificationType, messageId);
        assertThat(item.get("hasSellerSufficientReputation").asBoolean()).isEqualTo(hasSellerSufficientReputation);
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
