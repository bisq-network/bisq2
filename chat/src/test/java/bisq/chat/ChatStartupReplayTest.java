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

import bisq.chat.ProcessedConfidentialMessages.ReceivedMessage;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.network.p2p.services.data.storage.auth.DefaultAuthenticatedData;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.PersistableStore;
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
import org.junit.jupiter.api.parallel.Isolated;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * At startup the chat services replay data which they received before. An item whose processing fails does not stop
 * the start of the service nor the processing of the other items (docs/specifications/network/startup-replay.md).
 */
@Isolated("ProcessedConfidentialMessages uses the static NetworkExecutors")
class ChatStartupReplayTest {
    private static final UserIdentity MY_USER_IDENTITY = ChatTestUsers.createUserIdentity("me");
    private static final UserIdentity PEER_IDENTITY = ChatTestUsers.createUserIdentity("peer");
    private static final UserProfile PEER = PEER_IDENTITY.getUserProfile();
    private static final UserIdentity FAILING_PEER = ChatTestUsers.createUserIdentity("failingPeer");

    private PersistenceService persistenceService;
    private NetworkService networkService;
    private UserService userService;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Read by the shutdown hook of RateLimitedPersistenceClient
        when(persistence.getStorePath()).thenReturn(Path.of("test"));
        persistenceService = mock(PersistenceService.class);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), any(PersistableStore.class)))
                .thenReturn(persistence);
        when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any(PersistableStore.class)))
                .thenReturn(persistence);

        networkService = mock(NetworkService.class);

        UserIdentityService userIdentityService = mock(UserIdentityService.class);
        when(userIdentityService.findUserIdentity(MY_USER_IDENTITY.getId())).thenReturn(Optional.of(MY_USER_IDENTITY));
        userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(mock(ContactListService.class));
    }

    @Test
    void publicChatStartsAndAddsStoredMessagesAfterOneWhoseProcessingFails() {
        CommonPublicChatChannel channel = new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, SubDomain.DISCUSSION_BISQ);
        CommonPublicChatChannelService service = new CommonPublicChatChannelService(persistenceService,
                networkService,
                userService,
                ChatChannelDomain.DISCUSSION,
                List.of(channel));
        CommonPublicChatMessage message = new CommonPublicChatMessage(StringUtils.createUid(),
                ChatChannelDomain.DISCUSSION,
                channel.getId(),
                PEER.getId(),
                Optional.of("text"),
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT);
        DataService dataService = mock(DataService.class);
        when(dataService.getAuthenticatedData()).thenReturn(Stream.of(
                failingItem(AuthenticatedData.class),
                new DefaultAuthenticatedData(message)));
        when(networkService.getDataService()).thenReturn(Optional.of(dataService));

        service.initialize();

        assertEquals(Set.of(message), channel.getChatMessages());
    }

    @Test
    void privateChatStartsAndProcessesMessagesAfterOneWhoseProcessingFails() throws Exception {
        // Stands for any exception in the processing of a message from another user
        BannedUserService bannedUserService = mock(BannedUserService.class);
        when(bannedUserService.isUserProfileBanned(argThat((UserProfile profile) ->
                profile != null && profile.getId().equals(FAILING_PEER.getId()))))
                .thenThrow(new IllegalStateException("Processing a received message failed"));
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.getDoAutoAddToContactList()).thenReturn(true);
        TwoPartyPrivateChatChannelService service = new TwoPartyPrivateChatChannelService(persistenceService,
                networkService,
                userService,
                settingsService,
                ChatChannelDomain.DISCUSSION);
        String channelId = privateChannelIdWith(PEER_IDENTITY);
        TwoPartyPrivateChatMessage message = privateMessage(PEER_IDENTITY);
        // The services receive the processed messages through the network layer, so a real ConfidentialMessageService
        // decrypts them. It processes the failing message first.
        ProcessedConfidentialMessages.addToNetworkService(networkService, MY_USER_IDENTITY, List.of(
                new ReceivedMessage(privateMessage(FAILING_PEER), FAILING_PEER),
                new ReceivedMessage(message, PEER_IDENTITY)));

        service.initialize();

        TwoPartyPrivateChatChannel channel = service.findChannel(channelId).orElseThrow();
        assertEquals(Set.of(message), channel.getChatMessages());
        // The processing of the failing message throws before it creates a channel
        assertTrue(service.findChannel(privateChannelIdWith(FAILING_PEER)).isEmpty());
    }

    private static String privateChannelIdWith(UserIdentity peer) {
        return TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, peer.getId(), MY_USER_IDENTITY.getId());
    }

    private static TwoPartyPrivateChatMessage privateMessage(UserIdentity sender) {
        return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                ChatChannelDomain.DISCUSSION,
                privateChannelIdWith(sender),
                sender.getUserProfile(),
                MY_USER_IDENTITY.getId(),
                MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                "text",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                new HashSet<>());
    }

    // An item of which every method throws, so that its processing fails whichever method the service calls first
    private static <T> T failingItem(Class<T> type) {
        return mock(type, invocation -> {
            throw new IllegalStateException("Processing a stored item failed");
        });
    }
}
