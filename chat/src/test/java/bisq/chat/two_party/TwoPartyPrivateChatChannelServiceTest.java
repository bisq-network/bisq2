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

package bisq.chat.two_party;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessageType;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.contact_list.ContactListService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class TwoPartyPrivateChatChannelServiceTest {
    private static final String PEER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String MY_PROFILE_ID = "2222222222222222222222222222222222222222";

    private TwoPartyPrivateChatChannelService service;
    private UserProfileService userProfileService;
    private UserIdentityService userIdentityService;
    private SettingsService settingsService;
    private ContactListService contactListService;
    private UserIdentity myUserIdentity;
    private UserProfile peer;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any()))
                .thenReturn(persistence);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        userIdentityService = mock(UserIdentityService.class);
        userProfileService = mock(UserProfileService.class);
        contactListService = mock(ContactListService.class);
        settingsService = mock(SettingsService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(contactListService);

        myUserIdentity = mock(UserIdentity.class);
        when(myUserIdentity.getId()).thenReturn(MY_PROFILE_ID);
        when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(myUserIdentity));

        peer = mock(UserProfile.class);
        when(peer.getId()).thenReturn(PEER_PROFILE_ID);

        service = new TwoPartyPrivateChatChannelService(persistenceService,
                mock(NetworkService.class),
                userService,
                settingsService,
                ChatChannelDomain.DISCUSSION);
    }

    @Test
    void receivedMessageFromIgnoredUserDoesNotCreateChannel() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(true);

        service.onMessage(textMessageFromPeer());

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void receivedMessageFromNotIgnoredUserCreatesChannelAndDeliversMessage() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        TwoPartyPrivateChatMessage message = textMessageFromPeer();

        service.onMessage(message);

        assertEquals(1, service.getChannels().size());
        TwoPartyPrivateChatChannel channel = service.getChannels().stream().findFirst().orElseThrow();
        assertTrue(channel.getChatMessages().contains(message));
    }

    @Test
    void receivedMessageFromIgnoredUserDoesNotAddThemToContactList() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(true);
        when(settingsService.getDoAutoAddToContactList()).thenReturn(true);

        service.onMessage(textMessageFromPeer());

        verifyNoInteractions(contactListService);
    }

    @Test
    void receivedMessageFromIgnoredUserIsStillAddedToExistingChannel() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(true);
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(myUserIdentity);
        TwoPartyPrivateChatChannel channel = service.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer).orElseThrow();

        service.onMessage(textMessageFromPeer());

        assertEquals(1, channel.getChatMessages().size());
    }

    private TwoPartyPrivateChatMessage textMessageFromPeer() {
        return new TwoPartyPrivateChatMessage("messageId",
                ChatChannelDomain.DISCUSSION,
                TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, PEER_PROFILE_ID, MY_PROFILE_ID),
                peer,
                MY_PROFILE_ID,
                mock(NetworkId.class),
                "hi",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                new HashSet<>());
    }
}
