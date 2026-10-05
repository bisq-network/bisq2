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
import bisq.common.observable.collection.ObservableArray;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataRequest;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.StorageService;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedDataStorageService;
import bisq.notifications.NotificationService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static bisq.network.p2p.services.data.storage.StoreType.AUTHENTICATED_DATA_STORE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatNotificationServicePrunedMessagesTest {
    private static final String CHANNEL_ID = "discussion.bisq";

    private final ObservableArray<DataRequest> prunedRequests = new ObservableArray<>();
    private ChatNotificationService chatNotificationService;

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

        chatNotificationService = new ChatNotificationService(persistenceService,
                networkService,
                mock(ChatService.class, RETURNS_DEEP_STUBS),
                mock(NotificationService.class),
                mock(SettingsService.class),
                mock(UserIdentityService.class),
                mock(UserProfileService.class, RETURNS_DEEP_STUBS));
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
