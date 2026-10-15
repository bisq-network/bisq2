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

package bisq.chat;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.network.NetworkService;
import bisq.notifications.NotificationService;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * A user profile which a private chat channel still uses cannot be deleted. A channel keeps the copy of the user
 * identity with which it was created, and editing the profile replaces the user identity with one of the same key.
 */
class ChatServiceIdentityUsedTest {
    private ChatService chatService;
    private UserIdentity userIdentity;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(mock(PersistenceService.class, RETURNS_DEEP_STUBS),
                mock(NetworkService.class, RETURNS_DEEP_STUBS),
                mock(UserService.class, RETURNS_DEEP_STUBS),
                mock(SettingsService.class, RETURNS_DEEP_STUBS),
                mock(NotificationService.class, RETURNS_DEEP_STUBS));
        userIdentity = ChatTestUsers.createUserIdentity("trader");
        UserProfile peer = ChatTestUsers.createUserIdentity("peer").getUserProfile();
        chatService.getBisqEasyOpenTradeChannelService().getChannels().add(BisqEasyOpenTradeChannel.create("trade-id",
                mock(BisqEasyOffer.class),
                userIdentity,
                Set.of(peer),
                Optional.empty(),
                false));
    }

    @Test
    void identityOfAnOpenTradeChannelIsUsed() {
        assertThat(chatService.isIdentityUsed(userIdentity)).isTrue();
    }

    @Test
    void identityIsStillUsedAfterItsProfileWasEdited() {
        UserProfile editedProfile = UserProfile.forEdit(userIdentity.getUserProfile(), "new terms", "new statement");
        UserIdentity editedUserIdentity = new UserIdentity(userIdentity.getIdentity(), editedProfile);

        assertThat(chatService.isIdentityUsed(editedUserIdentity)).isTrue();
    }

    @Test
    void identityOfAnotherKeyIsNotUsed() {
        assertThat(chatService.isIdentityUsed(ChatTestUsers.createUserIdentity("other"))).isFalse();
    }
}
