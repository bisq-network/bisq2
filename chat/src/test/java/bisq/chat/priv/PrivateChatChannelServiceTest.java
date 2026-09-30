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

package bisq.chat.priv;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.collection.ObservableSet;
import bisq.i18n.Res;
import bisq.network.NetworkService;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.MetaData;
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
import com.google.protobuf.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the "peer is banned" system notice (#4769): who gets told, and how often. The nested class
 * covers the side effect itself, which the REST contract for a 409 has to keep matching.
 */
public class PrivateChatChannelServiceTest {
    private static final String NOTICE_BOB = Res.encode(PrivateChatChannelService.PEER_BANNED_NOTICE_KEY, "Bob");
    private static final String NOTICE_ALICE = Res.encode(PrivateChatChannelService.PEER_BANNED_NOTICE_KEY, "Alice");
    private static final String LEAVE = Res.encode("chat.privateChannel.message.leave", "Bob");

    @Test
    void emptyChannelIsNotYetNotified() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(List.of(), "Bob"));
    }

    @Test
    void noticeForThisPeerInTrailingRunMeansAlreadyNotified() {
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void aNoticeForAnotherPeerDoesNotSuppressThisPeer() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_ALICE, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void bothPeersNamedInTheRunAreEachAlreadyNotified() {
        var messages = List.of(
                message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE),
                message(2, NOTICE_ALICE, ChatMessageType.PROTOCOL_LOG_MESSAGE));
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(messages, "Bob"));
        assertTrue(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(messages, "Alice"));
    }

    @Test
    void aRealMessageAfterTheNoticeResetsTheRun() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(List.of(
                message(1, NOTICE_BOB, ChatMessageType.PROTOCOL_LOG_MESSAGE),
                message(2, "hi", ChatMessageType.TEXT)), "Bob"));
    }

    @Test
    void aDifferentSystemMessageIsNotABanNotice() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, LEAVE, ChatMessageType.PROTOCOL_LOG_MESSAGE)), "Bob"));
    }

    @Test
    void aNormalTextMessageWithTheNoticeTextIsNotABanNotice() {
        assertFalse(PrivateChatChannelService.alreadyNotifiedAboutBannedPeer(
                List.of(message(1, NOTICE_BOB, ChatMessageType.TEXT)), "Bob"));
    }

    /**
     * A rejected send is not a no-op. {@code PrivateChatRestApi.sendTextMessage} answers 409 from this
     * rejection and documents what the node did with it, so the notice being stored and reaching the
     * message stream is part of that contract rather than an implementation detail.
     */
    @Nested
    class RejectedSend {
        private static final String PEER_PROFILE_ID = "1111111111111111111111111111111111111111";
        private static final String MY_PROFILE_ID = "2222222222222222222222222222222222222222";

        private TwoPartyPrivateChatChannelService service;
        private BannedUserService bannedUserService;
        private UserProfile peer;
        private UserProfile myUserProfile;
        private TwoPartyPrivateChatChannel channel;

        @BeforeEach
        @SuppressWarnings({"unchecked", "rawtypes"})
        void setUp() {
            PersistenceService persistenceService = mock(PersistenceService.class);
            Persistence persistence = mock(Persistence.class);
            when(persistenceService.getOrCreatePersistence(any(), any(DbSubDirectory.class), anyString(), any()))
                    .thenReturn(persistence);
            when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

            bannedUserService = mock(BannedUserService.class);
            UserIdentityService userIdentityService = mock(UserIdentityService.class);
            SettingsService settingsService = mock(SettingsService.class);
            UserService userService = mock(UserService.class);
            when(userService.getUserIdentityService()).thenReturn(userIdentityService);
            when(userService.getUserProfileService()).thenReturn(mock(UserProfileService.class));
            when(userService.getBannedUserService()).thenReturn(bannedUserService);
            when(userService.getContactListService()).thenReturn(mock(ContactListService.class));

            myUserProfile = mock(UserProfile.class);
            when(myUserProfile.getId()).thenReturn(MY_PROFILE_ID);
            UserIdentity myUserIdentity = mock(UserIdentity.class);
            when(myUserIdentity.getId()).thenReturn(MY_PROFILE_ID);
            when(myUserIdentity.getUserProfile()).thenReturn(myUserProfile);
            when(userIdentityService.getSelectedUserIdentity()).thenReturn(myUserIdentity);
            when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(myUserIdentity));

            peer = mock(UserProfile.class);
            when(peer.getId()).thenReturn(PEER_PROFILE_ID);
            when(peer.getUserName()).thenReturn("Bob");
            when(peer.getNetworkId()).thenReturn(mock(NetworkId.class));

            service = new TwoPartyPrivateChatChannelService(persistenceService,
                    mock(NetworkService.class),
                    userService,
                    settingsService,
                    ChatChannelDomain.DISCUSSION);
            channel = service.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer).orElseThrow();
        }

        @Test
        void aBannedPeerRejectsTheSendAndLeavesTheNoticeBehind() {
            when(bannedUserService.isUserProfileBanned(peer)).thenReturn(true);

            SendOutcome outcome = service.trySendTextMessage("hi", Optional.empty(), channel);

            assertEquals(Optional.of(SendRejection.PEER_BANNED), outcome.getRejection());
            assertEquals(1, channel.getChatMessages().size());
            ChatMessage stored = channel.getChatMessages().iterator().next();
            assertEquals(ChatMessageType.PROTOCOL_LOG_MESSAGE, stored.getChatMessageType());
            assertEquals(Optional.of(NOTICE_BOB), stored.getText());
        }

        @Test
        void theNoticeIsNotRepeatedWhileItIsStillTheLastThingInTheChannel() {
            when(bannedUserService.isUserProfileBanned(peer)).thenReturn(true);

            service.trySendTextMessage("hi", Optional.empty(), channel);
            service.trySendTextMessage("hi again", Optional.empty(), channel);

            assertEquals(1, channel.getChatMessages().size());
        }

        @Test
        void myOwnBanRejectsTheSendWithoutStoringAnything() {
            when(bannedUserService.isUserProfileBanned(myUserProfile)).thenReturn(true);

            SendOutcome outcome = service.trySendTextMessage("hi", Optional.empty(), channel);

            assertEquals(Optional.of(SendRejection.MY_PROFILE_BANNED), outcome.getRejection());
            assertTrue(channel.getChatMessages().isEmpty());
        }

        @Test
        void aReactionToABannedPeerIsRejectedWithoutStoringAnything() {
            when(bannedUserService.isUserProfileBanned(peer)).thenReturn(true);

            SendOutcome outcome = service.trySendTextMessageReaction(messageFromPeer(),
                    channel,
                    Reaction.THUMBS_UP,
                    false);

            assertEquals(Optional.of(SendRejection.PEER_BANNED), outcome.getRejection());
            assertTrue(channel.getChatMessages().isEmpty());
        }

        private TwoPartyPrivateChatMessage messageFromPeer() {
            return new TwoPartyPrivateChatMessage("messageId",
                    ChatChannelDomain.DISCUSSION,
                    channel.getId(),
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

    private static ChatMessage message(long date, String text, ChatMessageType type) {
        return new TestChatMessage(date, text, type);
    }

    private static class TestChatMessage extends ChatMessage {
        private TestChatMessage(long date, String text, ChatMessageType type) {
            super("id", ChatChannelDomain.DISCUSSION, "channelId", "author",
                    Optional.ofNullable(text), Optional.<Citation>empty(), date, false, type);
        }

        @Override
        protected MetaData getMetaData() {
            return null;
        }

        @Override
        public <R extends ChatMessageReaction> ObservableSet<R> getChatMessageReactions() {
            return null;
        }

        @Override
        public boolean addChatMessageReaction(ChatMessageReaction reaction) {
            return false;
        }

        @Override
        public Message.Builder getBuilder(boolean serializeForHash) {
            return null;
        }
    }
}
