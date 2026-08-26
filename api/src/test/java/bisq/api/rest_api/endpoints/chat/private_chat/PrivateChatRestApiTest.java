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

package bisq.api.rest_api.endpoints.chat.private_chat;

import bisq.api.rest_api.endpoints.chat.SendChatMessageReactionRequest;
import bisq.api.rest_api.endpoints.chat.SendChatMessageRequest;
import bisq.api.dto.chat.CitationDto;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.chat.priv.SendOutcome;
import bisq.chat.priv.SendRejection;
import bisq.chat.reactions.Reaction;
import bisq.chat.reactions.TwoPartyPrivateChatMessageReaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.observable.collection.ObservableSet;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the input validation on the private-chat endpoints. Client input must come back as a 4xx —
 * an unchecked index or a missing lookup surfacing as a 500 is what these exist to prevent.
 */
class PrivateChatRestApiTest {
    private static final String CHANNEL_ID = "discussion.a-b";
    private static final String MESSAGE_ID = "message-1";
    private static final String MY_PROFILE_ID = "my-profile";
    /** Citation validation requires exactly 40 characters, so this cannot be a readable name. */
    private static final String AUTHOR_PROFILE_ID = "0123456789012345678901234567890123456789";

    private ChatService chatService;
    private TwoPartyPrivateChatChannelService channelService;
    private LeavePrivateChatManager leavePrivateChatManager;
    private ChatNotificationService chatNotificationService;
    private UserProfileService userProfileService;
    private UserIdentityService userIdentityService;
    private PrivateChatRestApi restApi;
    private AsyncResponse asyncResponse;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class, RETURNS_DEEP_STUBS);
        channelService = mock(TwoPartyPrivateChatChannelService.class, RETURNS_DEEP_STUBS);
        when(chatService.getTwoPartyPrivateChatChannelService()).thenReturn(channelService);
        // Explicit rather than deep stubs: which of these two the endpoint calls is the behaviour
        // under test in leaveChannel and consumeNotifications.
        leavePrivateChatManager = mock(LeavePrivateChatManager.class);
        chatNotificationService = mock(ChatNotificationService.class);
        when(chatService.getLeavePrivateChatManager()).thenReturn(leavePrivateChatManager);
        when(chatService.getChatNotificationService()).thenReturn(chatNotificationService);

        UserService userService = mock(UserService.class, RETURNS_DEEP_STUBS);
        userProfileService = mock(UserProfileService.class, RETURNS_DEEP_STUBS);
        userIdentityService = mock(UserIdentityService.class, RETURNS_DEEP_STUBS);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);

        restApi = new PrivateChatRestApi(chatService, userService);
        asyncResponse = mock(AsyncResponse.class);
    }

    @Test
    void findOrCreateChannelReturnsNotFoundForAnUnknownPeer() {
        when(userProfileService.findUserProfile("nobody")).thenReturn(Optional.empty());

        restApi.findOrCreateChannel("nobody", asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void sendTextMessageReturnsNotFoundForAnUnknownChannel() {
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.empty());

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verifyNoMessageSent();
    }

    @Test
    void anEmptyTextIsRejectedAsBadRequest() {
        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("", null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoMessageSent();
    }

    @Test
    void anAbsentRequestBodyIsRejectedAsBadRequest() {
        restApi.sendTextMessage(CHANNEL_ID, null, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoMessageSent();
    }

    @Test
    void anAbsentReactionRequestBodyIsRejectedAsBadRequest() {
        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID, null, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoReactionSent();
    }

    /**
     * Re-reacting is a no-op, not a missing message. This used to answer 404 "No message found" about
     * a message it was holding in hand.
     */
    @Test
    void aDuplicateReactionIsAcknowledgedRatherThanReportedMissing() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        when(userIdentityService.getMyUserProfileIds()).thenReturn(Set.of(MY_PROFILE_ID));

        TwoPartyPrivateChatMessageReaction existing = mock(TwoPartyPrivateChatMessageReaction.class, RETURNS_DEEP_STUBS);
        when(existing.isRemoved()).thenReturn(false);
        when(existing.getReactionId()).thenReturn(reactionId);
        when(existing.getSenderUserProfile().getId()).thenReturn(MY_PROFILE_ID);

        channelWithMessageCarrying(Set.of(existing));

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verifyNoReactionSent();
    }

    /**
     * The send refuses a banned peer before it stores anything, and used to report that only by completing
     * the future it returns exceptionally — which this endpoint discarded, answering 204 for a message
     * that exists nowhere: not in the store, not on the WebSocket stream, not in the log. The outcome is
     * read off the send itself, so no check here can disagree with what the node did.
     */
    @Test
    void aMessageRefusedLocallyIsReportedRatherThanAcknowledged() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(channelService.trySendTextMessage(any(), any(), eq(channel)))
                .thenReturn(SendOutcome.rejected(SendRejection.PEER_BANNED));

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    /** The two reasons are phrased apart, so a client can tell "you are banned" from "they are". */
    @Test
    void aBannedOwnProfileIsReportedApartFromABannedPeer() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));
        when(channelService.trySendTextMessage(any(), any(), eq(channel)))
                .thenReturn(SendOutcome.rejected(SendRejection.MY_PROFILE_BANNED));

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        assertThat(resumedEntity()).isEqualTo("Your user profile is banned.");
    }

    @Test
    void aReactionRefusedLocallyIsReportedRatherThanAcknowledged() {
        TwoPartyPrivateChatChannel channel = channelWithMessageCarrying(Set.of());
        when(channelService.trySendTextMessageReaction(any(), eq(channel), any(), anyBoolean()))
                .thenReturn(SendOutcome.rejected(SendRejection.PEER_BANNED));

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    /**
     * Why only the branch that actually sends can answer 409: a duplicate reaction neither sends nor
     * stores anything, so its 204 stays honest even while a send would be refused. Reporting the refusal
     * before reaching that branch would turn a documented no-op into a 409.
     */
    @Test
    void aDuplicateReactionStaysAcknowledgedEvenWhenASendWouldBeRefused() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        when(userIdentityService.getMyUserProfileIds()).thenReturn(Set.of(MY_PROFILE_ID));

        TwoPartyPrivateChatMessageReaction existing = mock(TwoPartyPrivateChatMessageReaction.class, RETURNS_DEEP_STUBS);
        when(existing.isRemoved()).thenReturn(false);
        when(existing.getReactionId()).thenReturn(reactionId);
        when(existing.getSenderUserProfile().getId()).thenReturn(MY_PROFILE_ID);

        TwoPartyPrivateChatChannel channel = channelWithMessageCarrying(Set.of(existing));
        when(channelService.trySendTextMessageReaction(any(), eq(channel), any(), anyBoolean()))
                .thenReturn(SendOutcome.rejected(SendRejection.PEER_BANNED));

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verifyNoReactionSent();
    }

    /**
     * The bounds check exists so a client typo is a 400 rather than the 500 that
     * {@code Reaction.values()[reactionId]} would produce.
     */
    @Test
    void anOutOfRangeReactionIdIsRejectedAsBadRequest() {
        int outOfRange = Reaction.values().length;

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(outOfRange, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoReactionSent();
    }

    @Test
    void aNegativeReactionIdIsRejectedAsBadRequest() {
        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(-1, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoReactionSent();
    }

    @Test
    void removingAReactionWithoutASenderIsRejectedAsBadRequest() {
        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, ""), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoReactionSent();
    }

    @Test
    void removingAReactionForAnUnknownIdentityIsRejectedAsBadRequest() {
        when(userIdentityService.findUserIdentity("someone-else")).thenReturn(Optional.empty());

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, "someone-else"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoReactionSent();
    }

    @Test
    void leaveReturnsNotFoundForAnUnknownChannel() {
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.empty());

        restApi.leaveChannel(CHANNEL_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void consumeNotificationsReturnsNotFoundForAnUnknownChannel() {
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.empty());

        restApi.consumeNotifications(CHANNEL_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    /** A reaction on a channel that exists but holds no such message is a 404, not a silent success. */
    @Test
    void aReactionOnAnUnknownMessageIsNotFound() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>());
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verifyNoReactionSent();
    }


    /* --------------------------------------------------------------------- */
    // Happy paths — that a valid request actually reaches the domain service
    /* --------------------------------------------------------------------- */

    /**
     * The counterpart to {@link #aDuplicateReactionIsAcknowledgedRatherThanReportedMissing}. Without
     * it, the guard could be inverted and every other test in this class would still pass.
     * <p>
     * Uses a reaction id other than 0 so that the {@code reactionId -> Reaction.values()[reactionId]}
     * lookup is pinned too: an off-by-one is invisible at index 0.
     */
    @Test
    void aNewReactionIsSentWithTheReactionTheIdNames() {
        int reactionId = 2;
        TwoPartyPrivateChatChannel channel = channelWithMessageCarrying(Set.of());

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        ArgumentCaptor<Reaction> captor = ArgumentCaptor.forClass(Reaction.class);
        verify(channelService).trySendTextMessageReaction(any(), eq(channel), captor.capture(), eq(false));
        assertThat(captor.getValue()).isEqualTo(Reaction.values()[reactionId]);
    }

    /** A remove is forwarded even when the reaction is not there, so it is never silently dropped. */
    @Test
    void aRemoveRequestIsForwardedWithTheRemoveFlagSet() {
        UserIdentity myIdentity = mock(UserIdentity.class, RETURNS_DEEP_STUBS);
        when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(myIdentity));
        TwoPartyPrivateChatChannel channel = channelWithMessageCarrying(Set.of());

        restApi.sendChatMessageReaction(CHANNEL_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService).trySendTextMessageReaction(any(), eq(channel), eq(Reaction.values()[0]), eq(true));
    }

    @Test
    void aValidMessageIsForwardedWithItsCitation() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));
        CitationDto citation = new CitationDto(AUTHOR_PROFILE_ID, "quoted text", Optional.of("cited-message-1"));

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", citation), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        ArgumentCaptor<Optional<Citation>> captor = citationCaptor();
        verify(channelService).trySendTextMessage(eq("hi"), captor.capture(), eq(channel));
        assertThat(captor.getValue()).isPresent();
        assertThat(captor.getValue().orElseThrow().getText()).isEqualTo("quoted text");
    }

    /**
     * {@code Citation}'s constructor validates, so a malformed one throws while the endpoint is building
     * the Bisq 2 model. That has to land in the {@code IllegalArgumentException} arm and come back as a
     * 400 — the generic arm below it would report a client typo as a 500.
     */
    @Test
    void aCitationWithAMalformedAuthorIdIsRejectedAsBadRequest() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));
        CitationDto citation = new CitationDto("too-short", "quoted text", Optional.empty());

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", citation), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verifyNoMessageSent();
    }

    /** An absent citation must reach the service as an empty Optional, not as a null. */
    @Test
    void aMessageWithoutACitationIsForwardedWithAnEmptyOptional() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));

        restApi.sendTextMessage(CHANNEL_ID, new SendChatMessageRequest("hi", null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        ArgumentCaptor<Optional<Citation>> captor = citationCaptor();
        verify(channelService).trySendTextMessage(eq("hi"), captor.capture(), eq(channel));
        assertThat(captor.getValue()).isEmpty();
    }

    /**
     * Which of the two leave paths runs is a deliberate choice, not an implementation detail: the
     * manager additionally re-selects the next channel and consumes the departed channel's
     * notifications, which {@code channelService.leaveChannel} does not.
     */
    @Test
    void leavingGoesThroughTheManagerRatherThanTheChannelService() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));

        restApi.leaveChannel(CHANNEL_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(leavePrivateChatManager).leaveChannel(channel);
        verify(channelService, never()).leaveChannel(any(TwoPartyPrivateChatChannel.class));
    }

    /** The unread count the client renders is read straight out of this call's effect. */
    @Test
    void consumingNotificationsReachesTheNotificationService() {
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));

        restApi.consumeNotifications(CHANNEL_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(chatNotificationService).consume(channel);
    }

    /**
     * The channel selection is shared with the desktop's Discussion page, so the selecting wrapper
     * ({@code ChatService.createAndSelectTwoPartyPrivateChatChannel}) would swap what the desktop is
     * showing every time a client opens a DM. The lower-level call binds the channel to the selected
     * identity just the same, without touching the selection.
     */
    @Test
    void openingAChannelDoesNotSelectIt() {
        UserProfile peer = mock(UserProfile.class);
        when(userProfileService.findUserProfile("peer")).thenReturn(Optional.of(peer));
        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getId()).thenReturn(CHANNEL_ID);
        when(channelService.findOrCreateChannel(ChatChannelDomain.DISCUSSION, peer)).thenReturn(Optional.of(channel));

        restApi.findOrCreateChannel("peer", asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(resumedEntity()).isEqualTo(new FindOrCreateChannelResponse(CHANNEL_ID));
        verify(chatService, never()).createAndSelectTwoPartyPrivateChatChannel(any(), any());
    }

    /**
     * My own profile is published like any other, so the peer lookup would find it and Bisq 2 would
     * happily build a channel keyed on sorted(me, me).
     */
    @Test
    void openingAChannelWithMyOwnProfileIsRejectedAsBadRequest() {
        UserIdentity myIdentity = mock(UserIdentity.class, RETURNS_DEEP_STUBS);
        when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(myIdentity));

        restApi.findOrCreateChannel(MY_PROFILE_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(userProfileService, never()).findUserProfile(anyString());
    }

    /** A channel holding one message with the given reactions, registered under {@link #CHANNEL_ID}. */
    private TwoPartyPrivateChatChannel channelWithMessageCarrying(
            Set<TwoPartyPrivateChatMessageReaction> reactions) {
        TwoPartyPrivateChatMessage message = mock(TwoPartyPrivateChatMessage.class, RETURNS_DEEP_STUBS);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getChatMessageReactions()).thenReturn(new ObservableSet<>(reactions));

        TwoPartyPrivateChatChannel channel = mock(TwoPartyPrivateChatChannel.class, RETURNS_DEEP_STUBS);
        when(channel.getChatMessages()).thenReturn(new ObservableSet<>(Set.of(message)));
        when(channelService.findChannel(CHANNEL_ID)).thenReturn(Optional.of(channel));
        return channel;
    }

    /** Generics on a captured {@code Optional<Citation>} cannot be expressed inline. */
    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Optional<Citation>> citationCaptor() {
        return ArgumentCaptor.forClass(Optional.class);
    }

    /** The single response the endpoint resumed with, which every test asserts the status of. */
    private int status() {
        return resumedResponse().getStatus();
    }

    /** Its body, for the tests that assert on what the response says rather than only its code. */
    private Object resumedEntity() {
        return resumedResponse().getEntity();
    }

    private Response resumedResponse() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(asyncResponse).resume(captor.capture());
        Object resumed = captor.getValue();
        assertThat(resumed).isInstanceOf(Response.class);
        return (Response) resumed;
    }

    private void verifyNoMessageSent() {
        verify(channelService, never()).trySendTextMessage(any(), any(), any());
    }

    private void verifyNoReactionSent() {
        verify(channelService, never()).trySendTextMessageReaction(any(), any(), any(), anyBoolean());
    }
}
