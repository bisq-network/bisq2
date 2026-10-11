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
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.StringUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.services.data.DataService;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Isolated("ProcessedConfidentialMessages uses the static NetworkExecutors")
class ReceivedChatMessageChannelIdTest {
    private static final UserIdentity MY_USER_IDENTITY = ChatTestUsers.createUserIdentity("me");
    private static final UserIdentity PEER_IDENTITY = ChatTestUsers.createUserIdentity("peer");
    private static final UserProfile PEER = PEER_IDENTITY.getUserProfile();

    private PersistenceService persistenceService;
    private NetworkService networkService;
    private UserService userService;
    private ContactListService contactListService;

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
        contactListService = mock(ContactListService.class);
        userService = mock(UserService.class);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
        when(userService.getBannedUserService()).thenReturn(mock(BannedUserService.class));
        when(userService.getContactListService()).thenReturn(contactListService);
    }

    @Nested
    class TwoPartyChat {
        private final String channelId = TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION,
                PEER.getId(),
                MY_USER_IDENTITY.getId());
        private TwoPartyPrivateChatChannelService service;

        @BeforeEach
        void setUp() {
            SettingsService settingsService = mock(SettingsService.class);
            when(settingsService.getDoAutoAddToContactList()).thenReturn(true);
            service = new TwoPartyPrivateChatChannelService(persistenceService,
                    networkService,
                    userService,
                    settingsService,
                    ChatChannelDomain.DISCUSSION);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "...", "discussion", "discussion.a", "discussion.a-", "unknown.a-b"})
        void messageWithInvalidChannelIdOpensNoChannelAndAddsNoContact(String invalidChannelId) {
            service.onMessage(message(ChatChannelDomain.DISCUSSION, invalidChannelId));

            assertTrue(service.getChannels().isEmpty());
            verifyNoInteractions(contactListService);
        }

        @Test
        void messageWithChannelIdOfOlderVersionIsAddedToMigratedChannel() {
            TwoPartyPrivateChatMessage message = message(ChatChannelDomain.BISQ_EASY_PRIVATE_CHAT,
                    "bisq_easy_private_chat." + PEER.getId() + "-" + MY_USER_IDENTITY.getId());

            service.onMessage(message);

            TwoPartyPrivateChatChannel channel = service.findChannel(channelId).orElseThrow();
            assertEquals(Set.of(message), channel.getChatMessages());
        }

        @Test
        void initializeProcessesMessagesAfterOneWithInvalidChannelId() throws Exception {
            TwoPartyPrivateChatMessage message = message(ChatChannelDomain.DISCUSSION, channelId);
            ProcessedConfidentialMessages.addToNetworkService(networkService, MY_USER_IDENTITY, List.of(
                    new ReceivedMessage(message(ChatChannelDomain.DISCUSSION, "discussion.a"), PEER_IDENTITY),
                    new ReceivedMessage(message, PEER_IDENTITY)));

            service.initialize();

            TwoPartyPrivateChatChannel channel = service.findChannel(channelId).orElseThrow();
            assertEquals(Set.of(message), channel.getChatMessages());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "...", "discussion", "discussion.a", "discussion.a-", "unknown.a-b"})
        void messageWithInvalidChannelIdCanBeCopiedIntoAHashBasedSet(String invalidChannelId) {
            TwoPartyPrivateChatMessage message = message(ChatChannelDomain.DISCUSSION, invalidChannelId);

            Set<TwoPartyPrivateChatMessage> copy = assertDoesNotThrow(() -> Set.copyOf(List.of(message)));
            assertTrue(copy.contains(message));
            assertEquals(invalidChannelId, message.getChannelId());
            assertFalse(message.hasValidChannelId());
        }

        private TwoPartyPrivateChatMessage message(ChatChannelDomain chatChannelDomain, String channelId) {
            return new TwoPartyPrivateChatMessage(StringUtils.createUid(),
                    chatChannelDomain,
                    channelId,
                    PEER,
                    MY_USER_IDENTITY.getId(),
                    MY_USER_IDENTITY.getUserProfile().getNetworkId(),
                    "text",
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    ChatMessageType.TEXT,
                    new HashSet<>());
        }
    }

    @Nested
    class PublicChat {
        private CommonPublicChatChannel channel;
        private CommonPublicChatChannelService service;

        @BeforeEach
        void setUp() {
            channel = new CommonPublicChatChannel(ChatChannelDomain.DISCUSSION, SubDomain.DISCUSSION_BISQ);
            service = new CommonPublicChatChannelService(persistenceService,
                    networkService,
                    userService,
                    ChatChannelDomain.DISCUSSION,
                    List.of(channel));
        }

        // A node of version 2.1.13 or earlier fails at its start when it stored such a message, so a node does not
        // create, accept, store or relay it (see docs/specifications/chat/message-types.md)
        @ParameterizedTest
        @ValueSource(strings = {"", "...", "discussion", "discussion.unknown", "unknown.bisq", "discussion_off.topic",
                "bisq_easy_offerbook.BTC-USD"})
        void messageWithInvalidChannelIdCannotBeCreated(String invalidChannelId) {
            assertThrows(IllegalArgumentException.class, () -> message(invalidChannelId));
        }

        @ParameterizedTest
        @ValueSource(strings = {"discussion.unknown", "unknown.bisq", "bisq_easy_offerbook.BTC-USD"})
        void receivedMessageWithInvalidChannelIdFailsAtParsing(String invalidChannelId) {
            bisq.chat.protobuf.ChatMessage proto = message(channel.getId()).toProto(false).toBuilder()
                    .setChannelId(invalidChannelId)
                    .build();

            UnresolvableProtobufException exception = assertThrows(UnresolvableProtobufException.class,
                    () -> CommonPublicChatMessage.fromProto(proto));

            // Rule 7 of docs/specifications/network/validation-at-parsing.md: no received value in the message
            assertFalse(String.valueOf(exception.getCause().getMessage()).contains(invalidChannelId));
        }

        @ParameterizedTest
        @EnumSource(SubDomain.class)
        void messageOfEveryChannelOfThisAndEarlierVersionsCanBeCreated(SubDomain subDomain) {
            assertDoesNotThrow(() -> message(subDomain.getChannelId()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"discussion.bitcoin", "discussion.offTopic", "events.tradeEvents"})
        void messageWithChannelIdOfOlderVersionIsAddedToMigratedChannel(String channelIdOfOlderVersion) {
            service.initialize();
            CommonPublicChatMessage message = message(channelIdOfOlderVersion);

            service.onAuthenticatedDataAdded(new DefaultAuthenticatedData(message));

            assertEquals(Set.of(message), channel.getChatMessages());
        }

        private CommonPublicChatMessage message(String channelId) {
            return new CommonPublicChatMessage(StringUtils.createUid(),
                    ChatChannelDomain.DISCUSSION,
                    channelId,
                    PEER.getId(),
                    Optional.of("text"),
                    Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    ChatMessageType.TEXT);
        }
    }
}
