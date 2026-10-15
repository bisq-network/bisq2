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
import bisq.chat.priv.PrivateChatChannelService;
import bisq.chat.reactions.Reaction;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
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
import java.util.concurrent.TimeUnit;

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

public class TwoPartyPrivateChatChannelServiceTest {
    private static final String PEER_PROFILE_ID = "1111111111111111111111111111111111111111";
    private static final String MY_PROFILE_ID = "2222222222222222222222222222222222222222";
    private static final String THIRD_PROFILE_ID = "3333333333333333333333333333333333333333";
    private static final String OTHER_OWN_PROFILE_ID = "4444444444444444444444444444444444444444";

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

    // The sender chooses the channel ID. A third user who learned the ID of our channel with the peer must not add
    // messages to it, and its message must not create another channel either.
    @Test
    void messageOfThirdUserWithChannelIdOfOtherConversationIsIgnored() {
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(myUserIdentity);
        TwoPartyPrivateChatChannel channel = service.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer).orElseThrow();
        UserProfile thirdUser = mock(UserProfile.class);
        when(thirdUser.getId()).thenReturn(THIRD_PROFILE_ID);

        service.onMessage(textMessageFrom(thirdUser));

        assertTrue(channel.getChatMessages().isEmpty());
        assertEquals(1, service.getChannels().size());
    }

    @Test
    void channelPeerIsOnlyTheOtherUser() {
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(myUserIdentity);
        TwoPartyPrivateChatChannel channel = service.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer).orElseThrow();

        assertTrue(channel.isPeer(PEER_PROFILE_ID));
        assertFalse(channel.isPeer(THIRD_PROFILE_ID));
        assertFalse(channel.isPeer(MY_PROFILE_ID));
    }

    @Test
    void pendingReactionIsAppliedWhenItsMessageArrives() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        service.onMessage(reactionFromPeer("reaction", "messageId", peerChannelId()));

        service.onMessage(textMessageFromPeer());

        assertEquals(1, receivedMessage().getChatMessageReactions().size());
    }

    @Test
    void messageIsProcessedWhileSeveralReactionsArePending() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        service.onMessage(reactionFromPeer("reaction1", "unknownMessage1", "unknownChannel1"));
        service.onMessage(reactionFromPeer("reaction2", "unknownMessage2", "unknownChannel2"));
        service.onMessage(reactionFromPeer("reaction3", "messageId", peerChannelId()));

        assertDoesNotThrow(() -> service.onMessage(textMessageFromPeer()));

        assertEquals(1, receivedMessage().getChatMessageReactions().size());
    }

    @Test
    void pendingReactionOfThirdUserIsNotApplied() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        UserProfile thirdUser = mock(UserProfile.class);
        when(thirdUser.getId()).thenReturn(THIRD_PROFILE_ID);
        service.onMessage(reactionFrom(thirdUser, "reaction", "messageId", peerChannelId()));

        service.onMessage(textMessageFromPeer());

        assertTrue(receivedMessage().getChatMessageReactions().isEmpty());
    }

    @Test
    void oldestPendingReactionIsDroppedWhenTheBoundIsReached() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        service.onMessage(reactionFromPeer("oldest", "messageId", peerChannelId()));
        for (int i = 0; i < PrivateChatChannelService.MAX_PENDING_REACTIONS; i++) {
            service.onMessage(reactionFromPeer("reaction" + i, "unknownMessage" + i, "unknownChannel" + i));
        }

        service.onMessage(textMessageFromPeer());

        assertTrue(receivedMessage().getChatMessageReactions().isEmpty());
    }

    @Test
    void pendingReactionIsKeptBelowTheBound() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        service.onMessage(reactionFromPeer("oldest", "messageId", peerChannelId()));
        for (int i = 0; i < PrivateChatChannelService.MAX_PENDING_REACTIONS - 1; i++) {
            service.onMessage(reactionFromPeer("reaction" + i, "unknownMessage" + i, "unknownChannel" + i));
        }

        service.onMessage(textMessageFromPeer());

        assertEquals(1, receivedMessage().getChatMessageReactions().size());
    }

    @Test
    void pendingReactionReceivedAgainDoesNotDropTheOldest() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        service.onMessage(reactionFromPeer("oldest", "messageId", peerChannelId()));
        TwoPartyPrivateChatMessageReaction receivedTwice = reactionFromPeer("receivedTwice", "unknownMessage", "unknownChannel");
        service.onMessage(receivedTwice);
        for (int i = 0; i < PrivateChatChannelService.MAX_PENDING_REACTIONS - 2; i++) {
            service.onMessage(reactionFromPeer("reaction" + i, "unknownMessage" + i, "unknownChannel" + i));
        }
        service.onMessage(receivedTwice);

        service.onMessage(textMessageFromPeer());

        assertEquals(1, receivedMessage().getChatMessageReactions().size());
    }

    @Test
    void rejectedPendingReactionFreesItsPlace() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        UserProfile thirdUser = mock(UserProfile.class);
        when(thirdUser.getId()).thenReturn(THIRD_PROFILE_ID);
        service.onMessage(reactionFromPeer("oldest", "secondMessageId", peerChannelId()));
        service.onMessage(reactionFrom(thirdUser, "rejected", "messageId", peerChannelId()));
        service.onMessage(textMessageFromPeer());
        for (int i = 0; i < PrivateChatChannelService.MAX_PENDING_REACTIONS - 1; i++) {
            service.onMessage(reactionFromPeer("reaction" + i, "unknownMessage" + i, "unknownChannel" + i));
        }

        service.onMessage(textMessageFrom(peer, "secondMessageId"));

        TwoPartyPrivateChatMessage secondMessage = service.getChannels().stream().findFirst().orElseThrow()
                .getChatMessages().stream()
                .filter(message -> message.getId().equals("secondMessageId"))
                .findFirst().orElseThrow();
        assertEquals(1, secondMessage.getChatMessageReactions().size());
    }

    @Test
    void expiredPendingReactionIsNotApplied() {
        when(userProfileService.isChatUserIgnored(peer)).thenReturn(false);
        TwoPartyPrivateChatMessageReaction reaction = reactionFromPeer("reaction", "messageId", peerChannelId());
        long expiredDate = System.currentTimeMillis() - reaction.getMetaData().getTtl() - TimeUnit.DAYS.toMillis(1);
        service.onMessage(reactionFrom(peer, "reaction", "messageId", peerChannelId(), expiredDate));

        service.onMessage(textMessageFromPeer());

        assertTrue(receivedMessage().getChatMessageReactions().isEmpty());
    }

    // The network layer decrypted the message with the key of its receiver network ID. A message which names another
    // of our identities as receiver must not open a conversation of that identity. Such a message or reaction fails at
    // parsing (PrivateChatMessage.verify, PrivateChatMessageReaction.verify), so it never reaches the service.
    @Test
    void messageWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdOpensNoChannel() {
        when(userIdentityService.findUserIdentity(OTHER_OWN_PROFILE_ID)).thenReturn(Optional.of(mock(UserIdentity.class)));

        assertThrows(IllegalArgumentException.class,
                () -> textMessageFrom(peer, "messageId", networkIdOf(OTHER_OWN_PROFILE_ID)));

        assertTrue(service.getChannels().isEmpty());
    }

    @Test
    void messageWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdIsNotAddedToExistingChannel() {
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(myUserIdentity);
        TwoPartyPrivateChatChannel channel = service.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer).orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> textMessageFrom(peer, "messageId", networkIdOf(OTHER_OWN_PROFILE_ID)));

        assertTrue(channel.getChatMessages().isEmpty());
    }

    @Test
    void reactionWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdIsNotApplied() {
        TwoPartyPrivateChatMessage message = textMessageFromPeer();
        service.onMessage(message);

        assertThrows(IllegalArgumentException.class,
                () -> reactionFromPeer(message, networkIdOf(OTHER_OWN_PROFILE_ID)));

        assertTrue(message.getChatMessageReactions().isEmpty());
    }

    // Such a reaction is ignored when it arrives, so it is not applied when its message arrives later
    @Test
    void reactionWhoseReceiverProfileIdIsNotTheIdOfItsReceiverNetworkIdIsNotKeptAsPending() {
        assertThrows(IllegalArgumentException.class, () -> reactionFrom(peer, "reaction", "messageId", peerChannelId(),
                System.currentTimeMillis(), networkIdOf(OTHER_OWN_PROFILE_ID)));

        service.onMessage(textMessageFromPeer());

        assertTrue(receivedMessage().getChatMessageReactions().isEmpty());
    }

    @Test
    void reactionOfPeerIsApplied() {
        TwoPartyPrivateChatMessage message = textMessageFromPeer();
        service.onMessage(message);

        service.onMessage(reactionFromPeer(message, networkIdOf(MY_PROFILE_ID)));

        assertEquals(1, message.getChatMessageReactions().size());
    }

    private TwoPartyPrivateChatMessage receivedMessage() {
        return service.getChannels().stream().findFirst().orElseThrow()
                .getChatMessages().stream().findFirst().orElseThrow();
    }

    private static String peerChannelId() {
        return TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, PEER_PROFILE_ID, MY_PROFILE_ID);
    }

    private TwoPartyPrivateChatMessageReaction reactionFromPeer(String id, String chatMessageId, String chatChannelId) {
        return reactionFrom(peer, id, chatMessageId, chatChannelId);
    }

    private TwoPartyPrivateChatMessageReaction reactionFromPeer(TwoPartyPrivateChatMessage message,
                                                                NetworkId receiverNetworkId) {
        return reactionFrom(peer, "reactionId", message.getId(), message.getChannelId(), System.currentTimeMillis(),
                receiverNetworkId);
    }

    private TwoPartyPrivateChatMessageReaction reactionFrom(UserProfile sender,
                                                            String id,
                                                            String chatMessageId,
                                                            String chatChannelId) {
        return reactionFrom(sender, id, chatMessageId, chatChannelId, System.currentTimeMillis());
    }

    private TwoPartyPrivateChatMessageReaction reactionFrom(UserProfile sender,
                                                            String id,
                                                            String chatMessageId,
                                                            String chatChannelId,
                                                            long date) {
        return reactionFrom(sender, id, chatMessageId, chatChannelId, date, networkIdOf(MY_PROFILE_ID));
    }

    private TwoPartyPrivateChatMessageReaction reactionFrom(UserProfile sender,
                                                            String id,
                                                            String chatMessageId,
                                                            String chatChannelId,
                                                            long date,
                                                            NetworkId receiverNetworkId) {
        return new TwoPartyPrivateChatMessageReaction(id,
                sender,
                MY_PROFILE_ID,
                receiverNetworkId,
                chatChannelId,
                ChatChannelDomain.DISCUSSION,
                chatMessageId,
                Reaction.THUMBS_UP.ordinal(),
                date,
                false);
    }

    private TwoPartyPrivateChatMessage textMessageFromPeer() {
        return textMessageFrom(peer);
    }

    private TwoPartyPrivateChatMessage textMessageFrom(UserProfile sender) {
        return textMessageFrom(sender, "messageId");
    }

    private TwoPartyPrivateChatMessage textMessageFrom(UserProfile sender, String messageId) {
        return textMessageFrom(sender, messageId, networkIdOf(MY_PROFILE_ID));
    }

    private TwoPartyPrivateChatMessage textMessageFrom(UserProfile sender,
                                                       String messageId,
                                                       NetworkId receiverNetworkId) {
        return new TwoPartyPrivateChatMessage(messageId,
                ChatChannelDomain.DISCUSSION,
                TwoPartyPrivateChatChannel.createId(ChatChannelDomain.DISCUSSION, PEER_PROFILE_ID, MY_PROFILE_ID),
                sender,
                MY_PROFILE_ID,
                receiverNetworkId,
                "hi",
                Optional.empty(),
                System.currentTimeMillis(),
                false,
                ChatMessageType.TEXT,
                new HashSet<>());
    }

    // The ID of a network ID is the hash of its public key, which is also the profile ID of that key
    private static NetworkId networkIdOf(String userProfileId) {
        NetworkId networkId = mock(NetworkId.class);
        when(networkId.getId()).thenReturn(userProfileId);
        return networkId;
    }
}
