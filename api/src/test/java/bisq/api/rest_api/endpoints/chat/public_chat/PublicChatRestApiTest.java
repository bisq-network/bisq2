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


package bisq.api.rest_api.endpoints.chat.public_chat;

import bisq.api.dto.chat.CitationDto;
import bisq.api.dto.chat.SendRejectionDto;
import bisq.api.dto.chat.common.CommonPublicChatChannelDto;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.rest_api.endpoints.chat.SendChatMessageReactionRequest;
import bisq.api.rest_api.endpoints.chat.SendRefusedResponse;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.common.SubDomain;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.reactions.ChatMessageReaction;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.observable.collection.ObservableSet;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.api.chat.common.PublicChatTestMocks.DISCUSSION_ID;
import static bisq.api.chat.common.PublicChatTestMocks.SUPPORT_ID;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannel;
import static bisq.api.chat.common.PublicChatTestMocks.mockChannelService;
import static bisq.api.chat.common.PublicChatTestMocks.mockMessage;
import static bisq.api.chat.common.PublicChatTestMocks.mockReaction;
import static bisq.api.chat.common.PublicChatTestMocks.mockUserProfile;
import static bisq.api.chat.common.PublicChatTestMocks.knownProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the contract of the public chat endpoints: client input comes back as a 4xx, the pre-checks run
 * before the domain is called, and the operations that remove data answer only after the local removal.
 */
class PublicChatRestApiTest {
    private static final String MESSAGE_ID = "message-1";
    private static final String MY_PROFILE_ID = "my-profile";
    private static final String OTHER_MY_PROFILE_ID = "my-other-profile";
    private static final String PEER_PROFILE_ID = "peer-profile";
    /** A citation's author id is validated as exactly 40 characters, so it cannot be a readable name. */
    private static final String CITATION_AUTHOR_ID = "0123456789012345678901234567890123456789";

    private CommonPublicChatChannelService channelService;
    private ChatNotificationService chatNotificationService;
    private UserProfileService userProfileService;
    private UserIdentityService userIdentityService;
    private BannedUserService bannedUserService;
    private CommonPublicChatChannelService supportChannelService;
    private ObservableSet<CommonPublicChatMessage> messages;
    private CommonPublicChatChannel channel;
    private CommonPublicChatChannel supportChannel;
    private UserIdentity myIdentity;
    private PublicChatRestApi restApi;
    private AsyncResponse asyncResponse;

    @BeforeEach
    void setUp() {
        messages = new ObservableSet<>();
        channel = mockChannel(SubDomain.DISCUSSION_BISQ, messages);
        supportChannel = mockChannel(SubDomain.SUPPORT_SUPPORT, new ObservableSet<>());

        channelService = mockChannelService(channel);
        supportChannelService = mockChannelService(supportChannel);
        for (CommonPublicChatChannelService service : List.of(channelService, supportChannelService)) {
            when(service.publishChatMessage(anyString(), any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(mock(BroadcastResult.class)));
            when(service.publishChatMessageReaction(any(), any(), any()))
                    .thenReturn(CompletableFuture.completedFuture(mock(BroadcastResult.class)));
        }
        chatNotificationService = mock(ChatNotificationService.class);

        ChatService chatService = mock(ChatService.class);
        when(chatService.getCommonPublicChatChannelServices()).thenReturn(Map.of(
                ChatChannelDomain.DISCUSSION, channelService,
                ChatChannelDomain.SUPPORT, supportChannelService));
        when(chatService.getChatNotificationService()).thenReturn(chatNotificationService);

        userProfileService = mock(UserProfileService.class);
        userIdentityService = mock(UserIdentityService.class);
        bannedUserService = mock(BannedUserService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userService.getBannedUserService()).thenReturn(bannedUserService);
        for (String id : List.of(MY_PROFILE_ID, OTHER_MY_PROFILE_ID, PEER_PROFILE_ID)) {
            knownProfile(userProfileService, id);
        }
        myIdentity = mockIdentity(MY_PROFILE_ID);
        when(userIdentityService.findUserIdentity(MY_PROFILE_ID)).thenReturn(Optional.of(myIdentity));
        UserIdentity otherIdentity = mockIdentity(OTHER_MY_PROFILE_ID);
        when(userIdentityService.findUserIdentity(OTHER_MY_PROFILE_ID)).thenReturn(Optional.of(otherIdentity));
        when(userIdentityService.findUserIdentity(PEER_PROFILE_ID)).thenReturn(Optional.empty());
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(myIdentity);

        restApi = new PublicChatRestApi(chatService, userService);
        asyncResponse = mock(AsyncResponse.class);
    }


    /* --------------------------------------------------------------------- */
    // Channels and history
    /* --------------------------------------------------------------------- */

    @Test
    void bothChannelsAreListedWithTheirUnreadCounts() {
        when(chatNotificationService.getNumNotifications(channel)).thenReturn(3L);
        when(chatNotificationService.getNumNotifications(supportChannel)).thenReturn(1L);

        Response response = restApi.getChannels();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        @SuppressWarnings("unchecked")
        List<CommonPublicChatChannelDto> dtos = (List<CommonPublicChatChannelDto>) response.getEntity();
        assertThat(dtos).extracting(CommonPublicChatChannelDto::id).containsExactly(DISCUSSION_ID, SUPPORT_ID);
        assertThat(dtos).extracting(CommonPublicChatChannelDto::unreadCount).containsExactly(3L, 1L);
    }

    @Test
    void historyReturnsNotFoundForAnUnknownChannel() {
        Response response = restApi.getMessages("discussion.nope");

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void historyIsTheWholeVisibleHistoryNewestFirst() {
        messages.add(mockMessage("a", PEER_PROFILE_ID, 100));
        messages.add(mockMessage("c", PEER_PROFILE_ID, 300));
        messages.add(mockMessage("b", PEER_PROFILE_ID, 200));

        List<CommonPublicChatMessageDto> items = messageList(restApi.getMessages(DISCUSSION_ID));

        assertThat(items).extracting(CommonPublicChatMessageDto::messageId).containsExactly("c", "b", "a");
    }

    @Test
    void historyHidesBannedAuthorsButNotIgnoredOnes() {
        messages.add(mockMessage("banned", "banned-author", 100));
        messages.add(mockMessage("ignored", PEER_PROFILE_ID, 200));
        knownProfile(userProfileService, "banned-author");
        when(bannedUserService.isUserProfileBanned("banned-author")).thenReturn(true);
        when(userProfileService.isChatUserIgnored(PEER_PROFILE_ID)).thenReturn(true);

        List<CommonPublicChatMessageDto> items = messageList(restApi.getMessages(DISCUSSION_ID));

        assertThat(items).extracting(CommonPublicChatMessageDto::messageId).containsExactly("ignored");
    }

    /** The author filter and the mapping run at different instants; a profile pruned in between costs one message, not a 500. */
    @Test
    void historySkipsAMessageWhoseAuthorVanishedMidMapping() {
        UserProfile goneProfile = mockUserProfile("gone");
        when(userProfileService.findUserProfile("gone")).thenReturn(Optional.of(goneProfile), Optional.empty());
        messages.add(mockMessage("dropped", "gone", 100));
        messages.add(mockMessage("kept", PEER_PROFILE_ID, 200));

        List<CommonPublicChatMessageDto> items = messageList(restApi.getMessages(DISCUSSION_ID));

        assertThat(items).extracting(CommonPublicChatMessageDto::messageId).containsExactly("kept");
    }


    /* --------------------------------------------------------------------- */
    // Send
    /* --------------------------------------------------------------------- */

    @Test
    void anEmptyTextIsRejectedAsBadRequest() {
        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    @Test
    void anAbsentRequestBodyIsRejectedAsBadRequest() {
        restApi.sendTextMessage(DISCUSSION_ID, null, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    /**
     * The domain rejects it too, but with a message written for a log: {@code NetworkDataValidation}
     * appends the offending input, so the 400 would hand the client back the thousand characters it just
     * sent. Same reason the text length is checked in {@code ChatRequestValidation} rather than left to
     * the domain.
     */
    @Test
    void anOverLongCitationIsRejectedWithoutEchoingIt() {
        String citationText = "x".repeat(Citation.MAX_TEXT_LENGTH + 1);
        CitationDto citation = new CitationDto(CITATION_AUTHOR_ID, citationText, Optional.empty());

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", citation, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(resumedEntity().toString()).doesNotContain(citationText);
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    @Test
    void sendingReturnsNotFoundForAnUnknownChannel() {
        restApi.sendTextMessage("discussion.nope", new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void aSenderThatIsNotOneOfMyIdentitiesIsRejectedAsBadRequest() {
        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, PEER_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    /** The two channels differ only in which service publishes for them, so this is the one thing to pin. */
    @Test
    void aMessageOnTheSupportChannelIsPublishedByTheSupportService() {
        restApi.sendTextMessage(SUPPORT_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(supportChannelService).publishChatMessage(eq("hi"), eq(Optional.empty()), eq(supportChannel), eq(myIdentity));
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    @Test
    void withoutASenderTheSelectedIdentitySends() {
        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService).publishChatMessage(eq("hi"), eq(Optional.empty()), eq(channel), eq(myIdentity));
    }

    /**
     * The only test that exercises a citation that is actually sent; the others all pass none and
     * verify {@code Optional.empty()}, which the endpoint would also produce with the mapping deleted.
     * <p>
     * Asserted field by field rather than with an {@code eq}: {@code Citation} excludes
     * {@code chatMessageId} from its {@code equals} (it was added in v2.1.7 and is
     * {@code @ExcludeForHash}), so an equality check would pass on a mapping that dropped it.
     */
    @Test
    void aCitationIsMappedAndPassedToTheDomain() {
        CitationDto citation = new CitationDto(CITATION_AUTHOR_ID, "quoted", Optional.of("quoted-message"));

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", citation, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        ArgumentCaptor<Optional<Citation>> sent = ArgumentCaptor.captor();
        verify(channelService).publishChatMessage(eq("hi"), sent.capture(), eq(channel), eq(myIdentity));
        Citation forwarded = sent.getValue().orElseThrow();
        assertThat(forwarded.getAuthorUserProfileId()).isEqualTo(CITATION_AUTHOR_ID);
        assertThat(forwarded.getText()).isEqualTo("quoted");
        assertThat(forwarded.getChatMessageId()).contains("quoted-message");
    }

    @Test
    void aNamedSenderIsUsedAsGiven() {
        UserIdentity other = userIdentityService.findUserIdentity(OTHER_MY_PROFILE_ID).orElseThrow();

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, OTHER_MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService).publishChatMessage(eq("hi"), eq(Optional.empty()), eq(channel), eq(other));
    }

    @Test
    void withoutASenderAndWithoutASelectedIdentityTheSendIsABadRequestNotAnError() {
        when(userIdentityService.getSelectedUserIdentity()).thenReturn(null);

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void aBannedSenderIsRefusedBeforeTheDomainIsCalled() {
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(true);

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        assertThat(resumedEntity()).isEqualTo(
                new SendRefusedResponse(SendRejectionDto.MY_PROFILE_BANNED, "Your user profile is banned."));
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    @Test
    void aRateLimitedSenderIsRefusedBeforeTheDomainIsCalled() {
        when(bannedUserService.isRateLimitExceeding(MY_PROFILE_ID)).thenReturn(true);

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(429);
        assertThat(resumedEntity()).isEqualTo(Map.of("error", "Rate limit exceeded. Wait before sending again."));
        verify(channelService, never()).publishChatMessage(anyString(), any(), any(), any());
    }

    /** The domain repeats the check; if the limit trips between the pre-check and the call, its rejection is still a 429. */
    @Test
    void aRateLimitTrippedInsideTheDomainIsStillTooManyRequests() {
        when(bannedUserService.isRateLimitExceeding(MY_PROFILE_ID)).thenReturn(false, true);
        when(channelService.publishChatMessage(anyString(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Rate limit was exceeding"));

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(429);
    }

    /** Same window as the rate limit: the domain rejects after our pre-check, and the answer is still the refusal. */
    @Test
    void aBanLandedInsideTheDomainIsStillARefusal() {
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(false, true);
        when(channelService.publishChatMessage(anyString(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("User profile is banned"));

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
    }

    /** A domain rejection that matches neither refusal falls back to the plain 400. */
    @Test
    void aDomainRejectionWithoutARefusalReasonIsABadRequest() {
        when(channelService.publishChatMessage(anyString(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("boom"));

        restApi.sendTextMessage(DISCUSSION_ID, new SendPublicChatMessageRequest("hi", null, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }


    /* --------------------------------------------------------------------- */
    // Edit and delete
    /* --------------------------------------------------------------------- */

    @Test
    void editingSomeoneElsesMessageIsForbidden() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("edited"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        verify(channelService, never()).publishEditedChatMessage(any(), anyString(), any());
    }

    @Test
    void editingAnUnknownMessageIsNotFound() {
        restApi.editMessage(DISCUSSION_ID, "nope", new EditPublicChatMessageRequest("edited"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    /** The null-body guard is duplicated per endpoint, so each endpoint's copy is pinned on its own. */
    @Test
    void anAbsentEditRequestBodyIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, null, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void anEditWithEmptyTextIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest(""), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(channelService, never()).publishEditedChatMessage(any(), anyString(), any());
    }

    /** The domain checks after it removed the original, so a refusal there would lose the message. */
    @Test
    void editingAsABannedAuthorIsRefusedBeforeTheDomainIsCalled() {
        messages.add(mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100));
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(true);

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("edited"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        verify(channelService, never()).publishEditedChatMessage(any(), anyString(), any());
    }

    /**
     * Desktop returns early on an unchanged edit ({@code ChatMessagesListController#onSaveEditedMessage}).
     * It matters more here: {@code publishEditedChatMessage} removes the original before its own ban and
     * rate limit checks run, so an edit that would change nothing can still end with the message gone.
     */
    @Test
    void anEditThatChangesNothingIsNotRepublished() {
        // mockMessage's text is "hi".
        messages.add(mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("hi"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService, never()).publishEditedChatMessage(any(), anyString(), any());
    }

    @Test
    void anEditAnswersOnlyAfterTheLocalRemovalCompleted() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        CompletableFuture<BroadcastResult> pending = new CompletableFuture<>();
        when(channelService.publishEditedChatMessage(message, "edited", myIdentity)).thenReturn(pending);

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("edited"), asyncResponse);
        verify(asyncResponse, never()).resume(any(Response.class));

        messages.remove(message);
        pending.complete(mock(BroadcastResult.class));

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    }

    /** The store answers a rejected removal with a successful, empty result; the endpoint has to look. */
    @Test
    void anEditWhoseOriginalWasNotRemovedLocallyIsAnError() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        when(channelService.publishEditedChatMessage(message, "edited", myIdentity))
                .thenReturn(CompletableFuture.completedFuture(mock(BroadcastResult.class)));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("edited"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void aFailedEditIsAnError() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        when(channelService.publishEditedChatMessage(message, "edited", myIdentity))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        restApi.editMessage(DISCUSSION_ID, MESSAGE_ID, new EditPublicChatMessageRequest("edited"), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void deletingSomeoneElsesMessageIsForbidden() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.deleteMessage(DISCUSSION_ID, MESSAGE_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        verify(channelService, never()).deleteChatMessage(any(), any());
    }

    /**
     * The store's removal listener skips the local removal for a rate-limited author while the store still
     * marks the message removed, so a delete that went through would leave the message stuck on the node.
     */
    @Test
    void deletingAsARateLimitedAuthorIsRefusedBeforeTheDomainIsCalled() {
        messages.add(mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100));
        when(bannedUserService.isRateLimitExceeding(MY_PROFILE_ID)).thenReturn(true);

        restApi.deleteMessage(DISCUSSION_ID, MESSAGE_ID, asyncResponse);

        assertThat(status()).isEqualTo(429);
        verify(channelService, never()).deleteChatMessage(any(), any());
    }

    @Test
    void aDeleteAnswersOnlyAfterTheLocalRemovalCompleted() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        CompletableFuture<BroadcastResult> pending = new CompletableFuture<>();
        when(channelService.deleteChatMessage(message, myIdentity.getNetworkIdWithKeyPair())).thenReturn(pending);

        restApi.deleteMessage(DISCUSSION_ID, MESSAGE_ID, asyncResponse);
        verify(asyncResponse, never()).resume(any(Response.class));

        messages.remove(message);
        pending.complete(mock(BroadcastResult.class));

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void aDeleteThatLeftTheMessageInPlaceIsAnError() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        when(channelService.deleteChatMessage(message, myIdentity.getNetworkIdWithKeyPair()))
                .thenReturn(CompletableFuture.completedFuture(mock(BroadcastResult.class)));

        restApi.deleteMessage(DISCUSSION_ID, MESSAGE_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void aFailedDeleteIsAnError() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, MY_PROFILE_ID, 100);
        messages.add(message);
        when(channelService.deleteChatMessage(message, myIdentity.getNetworkIdWithKeyPair()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        restApi.deleteMessage(DISCUSSION_ID, MESSAGE_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    /* --------------------------------------------------------------------- */
    // Reactions
    /* --------------------------------------------------------------------- */

    @Test
    void anAbsentReactionRequestBodyIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID, null, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void anOutOfRangeReactionIdIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(Reaction.values().length, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void aReactionOnAnUnknownMessageIsNotFound() {
        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void aNewReactionIsPublishedAsTheNamedIdentity() {
        CommonPublicChatMessage message = mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100);
        messages.add(message);
        UserIdentity other = userIdentityService.findUserIdentity(OTHER_MY_PROFILE_ID).orElseThrow();

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(Reaction.HEART.ordinal(), false, OTHER_MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService).publishChatMessageReaction(message, Reaction.HEART, other);
    }

    /** Another of my identities holding the same reaction is a different reaction on a public message. */
    @Test
    void addingIsIdempotentPerIdentityOnly() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        reactions.add(mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId));
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, MY_PROFILE_ID), asyncResponse);
        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService, never()).publishChatMessageReaction(any(), any(), any());

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, OTHER_MY_PROFILE_ID), asyncResponse);
        UserIdentity other = userIdentityService.findUserIdentity(OTHER_MY_PROFILE_ID).orElseThrow();
        verify(channelService).publishChatMessageReaction(any(), eq(Reaction.THUMBS_UP), eq(other));
    }

    /** The domain answers a banned sender with a failed future and no exception, which would become a 204 that did nothing. */
    @Test
    void reactingAsABannedIdentityIsRefusedBeforeTheDomainIsCalled() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(true);

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, false, null), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        verify(channelService, never()).publishChatMessageReaction(any(), any(), any());
    }

    @Test
    void removingAReactionWithoutASenderIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, ""), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(channelService, never()).deleteChatMessageReaction(any(), any());
    }

    @Test
    void removingAReactionForAnUnknownIdentityIsRejectedAsBadRequest() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, PEER_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void removingAReactionTheIdentityDoesNotHoldIsANoOp() {
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(0, true, MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService, never()).deleteChatMessageReaction(any(), any());
    }

    @Test
    void aRemovalAnswersOnlyAfterTheLocalRemovalCompleted() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        CommonPublicChatMessageReaction mine = mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId);
        reactions.add(mine);
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));
        CompletableFuture<BroadcastResult> pending = new CompletableFuture<>();
        when(channelService.deleteChatMessageReaction(mine, myIdentity.getNetworkIdWithKeyPair())).thenReturn(pending);

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, true, MY_PROFILE_ID), asyncResponse);
        verify(asyncResponse, never()).resume(any(Response.class));

        reactions.remove(mine);
        pending.complete(mock(BroadcastResult.class));

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    }

    /**
     * Deliberately not pre-checked, unlike deleting a message. The domain gates the add
     * ({@code ChatChannelService#addMessageReaction} refuses a banned or rate limited sender) and gates
     * nothing on the remove — {@code #removeMessageReaction} checks nothing and
     * {@code PublicChatChannelService#processRemovedReaction} does not ask {@code isValid}, where the
     * message equivalent does. So there is no dropped local removal for a refusal to protect against
     * here, and refusing would leave a banned user's reaction on every client for good.
     * <p>
     * Pinned because the asymmetry with {@code deletingAsARateLimitedAuthorIsRefusedBeforeTheDomainIsCalled}
     * reads like an oversight and is not one.
     */
    @Test
    void removingMyReactionWhileBannedStillGoesThrough() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        CommonPublicChatMessageReaction mine = mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId);
        reactions.add(mine);
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(true);
        CompletableFuture<BroadcastResult> pending = new CompletableFuture<>();
        when(channelService.deleteChatMessageReaction(mine, myIdentity.getNetworkIdWithKeyPair())).thenReturn(pending);

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, true, MY_PROFILE_ID), asyncResponse);
        reactions.remove(mine);
        pending.complete(mock(BroadcastResult.class));

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(channelService).deleteChatMessageReaction(mine, myIdentity.getNetworkIdWithKeyPair());
    }

    /**
     * The refusal runs before the dedupe, so a repeat from a banned identity is answered with the
     * refusal rather than with the 204 the dedupe alone would give. Private chat orders the two the
     * other way round ({@code PrivateChatRestApiTest#aDuplicateReactionStaysAcknowledgedEvenWhenASendWouldBeRefused}),
     * and the difference is visible to a client, so the order is pinned here rather than left to
     * whichever check happens to come first after the next edit.
     */
    @Test
    void aDuplicateReactionFromABannedIdentityIsRefusedRatherThanAcknowledged() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        reactions.add(mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId));
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));
        when(bannedUserService.isUserProfileBanned(MY_PROFILE_ID)).thenReturn(true);

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, false, MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.CONFLICT.getStatusCode());
        verify(channelService, never()).publishChatMessageReaction(any(), any(), any());
    }

    @Test
    void aRemovalThatLeftTheReactionInPlaceIsAnError() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        CommonPublicChatMessageReaction mine = mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId);
        reactions.add(mine);
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));
        when(channelService.deleteChatMessageReaction(mine, myIdentity.getNetworkIdWithKeyPair()))
                .thenReturn(CompletableFuture.completedFuture(mock(BroadcastResult.class)));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, true, MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    void aFailedReactionRemovalIsAnError() {
        int reactionId = Reaction.THUMBS_UP.ordinal();
        ObservableSet<ChatMessageReaction> reactions = new ObservableSet<>();
        CommonPublicChatMessageReaction mine = mockReaction("r-mine", MY_PROFILE_ID, MESSAGE_ID, reactionId);
        reactions.add(mine);
        messages.add(mockMessage(MESSAGE_ID, PEER_PROFILE_ID, 100, reactions));
        when(channelService.deleteChatMessageReaction(mine, myIdentity.getNetworkIdWithKeyPair()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        restApi.sendChatMessageReaction(DISCUSSION_ID, MESSAGE_ID,
                new SendChatMessageReactionRequest(reactionId, true, MY_PROFILE_ID), asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }


    /* --------------------------------------------------------------------- */
    // Notifications
    /* --------------------------------------------------------------------- */

    @Test
    void consumeNotificationsReturnsNotFoundForAnUnknownChannel() {
        restApi.consumeNotifications("discussion.nope", asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void consumingNotificationsReachesTheNotificationService() {
        restApi.consumeNotifications(DISCUSSION_ID, asyncResponse);

        assertThat(status()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(chatNotificationService).consume(channel);
    }


    /* --------------------------------------------------------------------- */
    // Helpers
    /* --------------------------------------------------------------------- */

    private static UserIdentity mockIdentity(String id) {
        UserIdentity identity = mock(UserIdentity.class, RETURNS_DEEP_STUBS);
        when(identity.getId()).thenReturn(id);
        return identity;
    }

    @SuppressWarnings("unchecked")
    private static List<CommonPublicChatMessageDto> messageList(Response response) {
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return (List<CommonPublicChatMessageDto>) response.getEntity();
    }

    private int status() {
        return resumed().getStatus();
    }

    private Object resumedEntity() {
        return resumed().getEntity();
    }

    private Response resumed() {
        ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(asyncResponse).resume(captor.capture());
        return captor.getValue();
    }
}
