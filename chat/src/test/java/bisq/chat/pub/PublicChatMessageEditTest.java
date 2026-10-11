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

package bisq.chat.pub;

import bisq.chat.ChatChannelDomain;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.network.NetworkService;
import bisq.persistence.PersistenceService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicChatMessageEditTest {
    private static final String AUTHOR_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String OTHER_PROFILE_ID = "2222222222222222222222222222222222222222";

    // Another of our identities must not edit the message: the removal would fail, and the edited message would be
    // published under the other profile, which links the two profiles
    @Test
    void identityWhichIsNotTheAuthorCannotEdit() {
        NetworkService networkService = mock(NetworkService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(mock(UserIdentityService.class));
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        CommonPublicChatChannelService service = new CommonPublicChatChannelService(mock(PersistenceService.class),
                networkService,
                userService,
                ChatChannelDomain.DISCUSSION,
                List.of());
        CommonPublicChatMessage message = new CommonPublicChatMessage(ChatChannelDomain.DISCUSSION,
                SubDomain.DISCUSSION_BISQ.getChannelId(),
                AUTHOR_PROFILE_ID,
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false);
        UserIdentity otherIdentity = mock(UserIdentity.class);
        when(otherIdentity.getId()).thenReturn(OTHER_PROFILE_ID);

        assertThrows(CompletionException.class,
                () -> service.publishEditedChatMessage(message, "edited", otherIdentity).join());
        verify(networkService, never()).removeAuthenticatedData(any(), any());
    }
}
