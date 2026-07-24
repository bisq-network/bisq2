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
import bisq.chat.ChatMessage;
import bisq.chat.ChatService;
import bisq.network.NetworkService;
import bisq.notifications.NotificationService;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChatNotificationServiceIgnoredUsersTest {
    private static final String IGNORED_PROFILE_ID = "ignored-user-profile-id";
    private static final String OTHER_PROFILE_ID = "other-user-profile-id";

    private ChatNotificationService chatNotificationService;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<ChatNotificationsStore> persistence = mock(Persistence.class);
        when(persistenceService.<ChatNotificationsStore>getOrCreatePersistence(any(), any(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        NetworkService networkService = mock(NetworkService.class);
        when(networkService.getDataService()).thenReturn(Optional.empty());

        userProfileService = mock(UserProfileService.class);
        when(userProfileService.isChatUserIgnored(IGNORED_PROFILE_ID)).thenReturn(true);
        when(userProfileService.isChatUserIgnored(OTHER_PROFILE_ID)).thenReturn(false);

        chatNotificationService = new ChatNotificationService(persistenceService,
                networkService,
                mock(ChatService.class),
                mock(NotificationService.class),
                mock(SettingsService.class),
                mock(UserIdentityService.class),
                userProfileService);
    }

    @Test
    void notificationFromIgnoredUserGetsConsumed() {
        ChatNotification notification = addNotification("a", IGNORED_PROFILE_ID);

        chatNotificationService.consumeNotificationsFromIgnoredUsers();

        assertTrue(notification.getIsConsumed().get());
    }

    @Test
    void notificationFromOtherUserStaysUnconsumed() {
        ChatNotification notification = addNotification("b", OTHER_PROFILE_ID);

        chatNotificationService.consumeNotificationsFromIgnoredUsers();

        assertFalse(notification.getIsConsumed().get());
    }

    @Test
    void notificationWithoutSenderStaysUnconsumed() {
        ChatNotification notification = addNotification("c", null);

        chatNotificationService.consumeNotificationsFromIgnoredUsers();

        assertFalse(notification.getIsConsumed().get());
    }

    @Test
    void onlyIgnoredSendersNotificationsAreConsumedInMixedSet() {
        ChatNotification fromIgnored = addNotification("d", IGNORED_PROFILE_ID);
        ChatNotification fromOther = addNotification("e", OTHER_PROFILE_ID);

        chatNotificationService.consumeNotificationsFromIgnoredUsers();

        assertTrue(fromIgnored.getIsConsumed().get());
        assertFalse(fromOther.getIsConsumed().get());
    }

    private ChatNotification addNotification(String id, String senderProfileId) {
        ChatChannel<?> chatChannel = mock(ChatChannel.class);
        when(chatChannel.getId()).thenReturn("channel-" + id);
        when(chatChannel.getChatChannelDomain()).thenReturn(ChatChannelDomain.DISCUSSION);

        ChatMessage chatMessage = mock(ChatMessage.class);
        when(chatMessage.getId()).thenReturn("message-" + id);
        when(chatMessage.getDate()).thenReturn(System.currentTimeMillis());

        Optional<UserProfile> sender = Optional.ofNullable(senderProfileId)
                .map(profileId -> {
                    UserProfile userProfile = mock(UserProfile.class);
                    when(userProfile.getId()).thenReturn(profileId);
                    return userProfile;
                });

        ChatNotification notification = new ChatNotification("notification-" + id,
                "title",
                "message",
                chatChannel,
                chatMessage,
                sender);
        chatNotificationService.getPersistableStore().getNotifications().add(notification);
        return notification;
    }
}
