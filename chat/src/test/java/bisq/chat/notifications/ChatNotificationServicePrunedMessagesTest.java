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

package bisq.chat.notifications;

import bisq.chat.ChatChannel;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.notifications.NotificationService;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StoreType.AUTHENTICATED_DATA_STORE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatNotificationServicePrunedMessagesTest {
    private static final String CHANNEL_ID = "discussion.bisq";

    private final ObservableArray<DataRequest> prunedRequests = new ObservableArray<>();
    private final ObservableSet<MuSigOffer> muSigOffers = new ObservableSet<>();
    private final Observable<Boolean> muSigOfferbookInitialized = new Observable<>(false);
    private ChatNotificationService chatNotificationService;
    private UserIdentityService userIdentityService;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<ChatNotificationsStore> persistence = mock(Persistence.class);
        when(persistenceService.<ChatNotificationsStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        AuthenticatedDataStorageService store = mock(AuthenticatedDataStorageService.class);
        when(store.getPrunedAndExpiredDataRequests()).thenReturn(prunedRequests);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getStoresByStoreType(AUTHENTICATED_DATA_STORE)).thenAnswer(invocation -> Stream.of(store));
        DataService dataService = mock(DataService.class);
        when(dataService.getStorageService()).thenReturn(storageService);
        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));

        ChatService chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        when(chatService.getMuSigOfferbookService().getObservableOffers()).thenReturn(muSigOffers);
        when(chatService.getMuSigOfferbookService().getInitialized()).thenReturn(muSigOfferbookInitialized);
        userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.getMyUserProfileIds()).thenReturn(Set.of("my-maker"));
        userProfileService = mock(UserProfileService.class, RETURNS_DEEP_STUBS);
        when(userProfileService.isChatUserIgnored("ignored-maker")).thenReturn(true);

        chatNotificationService = new ChatNotificationService(persistenceService,
                networkService,
                chatService,
                mock(NotificationService.class),
                mock(SettingsService.class),
                userIdentityService,
                userProfileService);
    }

    @Test
    void messagePrunedAfterInitializeIsNotKept() {
        chatNotificationService.initialize();

        for (int i = 0; i < 10; i++) {
            prunedRequests.add(prunedRequest("message-" + i));
        }

        assertTrue(chatNotificationService.notificationIdsPrunedBeforeInitialize.isEmpty());
    }

    @Test
    void prunedMusigOfferDoesNotCreateNotificationWhenOfferbookIsReplayed() {
        long offerDate = System.currentTimeMillis() - MetaData.TTL_30_DAYS - 1;
        MuSigOffer oldOffer = mock(MuSigOffer.class);
        when(oldOffer.getId()).thenReturn("old-offer");
        when(oldOffer.getDate()).thenReturn(offerDate);
        muSigOffers.add(oldOffer);

        ChatNotification persistedNotification = new ChatNotification(
                ChatNotification.createId(oldOffer),
                "MuSig Offer",
                "",
                offerDate,
                "",
                ChatChannelDomain.MU_SIG_OFFERBOOK,
                "",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                true);
        chatNotificationService.getPersistableStore().getNotifications().add(persistedNotification);

        ChatNotificationsStore prunedStore = chatNotificationService.prunePersisted(
                chatNotificationService.getPersistableStore().getClone());
        chatNotificationService.getPersistableStore().applyPersisted(prunedStore);

        chatNotificationService.initialize();

        assertTrue(chatNotificationService.getPersistableStore().getNotifications().isEmpty());
    }

    @Test
    void notificationForMusigOfferRemovedWhileOfflineIsRemovedAfterOfferbookReplay() {
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getId()).thenReturn("removed-while-offline");
        when(offer.getDate()).thenReturn(System.currentTimeMillis());
        String notificationId = ChatNotification.createId(offer);

        chatNotificationService.initialize();
        muSigOfferbookInitialized.set(true);
        muSigOffers.add(offer);
        assertTrue(chatNotificationService.getPersistableStore().findNotification(notificationId).isPresent());
        assertTrue(chatNotificationService.getPersistableStore().findNotification(notificationId).get().isNotConsumed());

        chatNotificationService.shutdown();
        muSigOfferbookInitialized.set(false);
        muSigOffers.clear();

        chatNotificationService.initialize();
        assertTrue(chatNotificationService.getPersistableStore().findNotification(notificationId).isPresent());

        muSigOfferbookInitialized.set(true);

        assertTrue(chatNotificationService.getPersistableStore().findNotification(notificationId).isEmpty());
    }

    @Test
    void offersPresentBeforeInitializeDoNotCreateNotifications() {
        muSigOffers.addAll(List.of(
                musigOffer("startup-offer-1", "maker-1"),
                musigOffer("startup-offer-2", "maker-2"),
                musigOffer("startup-offer-3", "maker-3")));

        chatNotificationService.initialize();
        muSigOfferbookInitialized.set(true);

        assertTrue(chatNotificationService.getPersistableStore().getNotifications().isEmpty());
    }

    @Test
    void offersReplayedBeforeOfferbookInitializationDoNotCreateNotifications() {
        chatNotificationService.initialize();
        muSigOffers.addAll(List.of(
                musigOffer("replayed-offer-1", "maker-1"),
                musigOffer("replayed-offer-2", "maker-2"),
                musigOffer("replayed-offer-3", "maker-3")));

        muSigOfferbookInitialized.set(true);

        assertTrue(chatNotificationService.getPersistableStore().getNotifications().isEmpty());
    }

    @Test
    void newOffersFromMyOrIgnoredMakersDoNotCreateNotifications() {
        chatNotificationService.initialize();
        muSigOfferbookInitialized.set(true);

        MuSigOffer ownOffer = musigOffer("own-offer", "my-maker");
        MuSigOffer ignoredOffer = musigOffer("ignored-offer", "ignored-maker");
        MuSigOffer otherOffer = musigOffer("other-offer", "other-maker");
        when(ownOffer.isMyOffer(anySet())).thenReturn(true);
        muSigOffers.add(ownOffer);
        muSigOffers.add(ignoredOffer);
        muSigOffers.add(otherOffer);

        assertTrue(chatNotificationService.getPersistableStore().findNotification(ChatNotification.createId(otherOffer)).isPresent());
        assertTrue(chatNotificationService.getPersistableStore().findNotification(ChatNotification.createId(ownOffer)).isEmpty());
        assertTrue(chatNotificationService.getPersistableStore().findNotification(ChatNotification.createId(ignoredOffer)).isEmpty());
    }

    private MuSigOffer musigOffer(String id, String makersUserProfileId) {
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getId()).thenReturn(id);
        when(offer.getDate()).thenReturn(System.currentTimeMillis());
        when(offer.getMakersUserProfileId()).thenReturn(makersUserProfileId);
        return offer;
    }

    @Test
    void messagePrunedAfterInitializeRemovesItsNotification() {
        chatNotificationService.initialize();
        ChatNotification notification = addNotification("message-1");

        prunedRequests.add(prunedRequest("message-1"));

        assertFalse(hasNotification(notification));
    }

    @Test
    void messagePrunedBeforeNotificationsAreReadRemovesItsNotificationAtInitialize() {
        // The stores prune their persisted data when they read it, possibly before our notifications are read
        prunedRequests.add(prunedRequest("message-1"));
        ChatNotification notification = addNotification("message-1");
        assertTrue(hasNotification(notification));

        chatNotificationService.initialize();

        assertFalse(hasNotification(notification));
        assertTrue(chatNotificationService.notificationIdsPrunedBeforeInitialize.isEmpty());
    }

    private static AddAuthenticatedDataRequest prunedRequest(String messageId) {
        CommonPublicChatMessage chatMessage = mock(CommonPublicChatMessage.class);
        when(chatMessage.hasValidChannelId()).thenReturn(true);
        when(chatMessage.getChannelId()).thenReturn(CHANNEL_ID);
        when(chatMessage.getId()).thenReturn(messageId);
        AddAuthenticatedDataRequest request = mock(AddAuthenticatedDataRequest.class);
        when(request.getDistributedData()).thenReturn(chatMessage);
        return request;
    }

    private ChatNotification addNotification(String messageId) {
        ChatChannel<?> chatChannel = mock(ChatChannel.class);
        when(chatChannel.getId()).thenReturn(CHANNEL_ID);
        when(chatChannel.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);
        CommonPublicChatMessage chatMessage = mock(CommonPublicChatMessage.class);
        when(chatMessage.getId()).thenReturn(messageId);
        when(chatMessage.getDate()).thenReturn(System.currentTimeMillis());
        ChatNotification notification = new ChatNotification(ChatNotification.createId(CHANNEL_ID, messageId),
                "title",
                "message",
                chatChannel,
                chatMessage,
                Optional.empty());
        chatNotificationService.getPersistableStore().getNotifications().add(notification);
        return notification;
    }

    private boolean hasNotification(ChatNotification notification) {
        return chatNotificationService.getPersistableStore().getNotifications().contains(notification);
    }
}
