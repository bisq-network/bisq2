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

import bisq.api.dto.DtoMappings;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.rest_api.endpoints.chat.SendChatMessageReactionRequest;
import bisq.api.rest_api.endpoints.chat.SendChatMessageRequest;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.chat.priv.SendOutcome;
import bisq.chat.priv.SendRejection;
import bisq.chat.reactions.Reaction;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatChannelService;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.util.StringUtils;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Two-party private chat (DM) with an arbitrary peer, outside of a trade. Every operation is addressed
 * by channel id, deliberately unlike {@code TradeChatMessagesRestApi}, whose channel is implied by the
 * trade: a notification has to be able to act on a channel that is not the selected one.
 * <p>
 * <b>Known limitation — the client cannot choose its identity.</b> Channel creation goes through
 * {@code ChatService.createAndSelectTwoPartyPrivateChatChannel}, which binds the new channel to
 * whichever user identity is currently selected on the node and additionally makes the new channel
 * the selected one. A multi-identity user therefore has no way to say "open this DM as identity X",
 * and the call has a side effect on node-wide selection state. Giving the client control would mean
 * taking an optional {@code myUserProfileId} here and reaching past the wrapper — deliberately not
 * done yet, since the wrapper is also what keeps the *next* DM correctly bound (see
 * {@link #findOrCreateChannel}).
 */
@Slf4j
@Path("/private-chat-channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Private Chat API", description = "Endpoints for two-party private chat (direct messages)")
public class PrivateChatRestApi extends RestApiBase {
    private static final int TIMEOUT_SEC = 120;

    private final ChatService chatService;
    private final TwoPartyPrivateChatChannelService channelService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private final ChatNotificationService chatNotificationService;
    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;

    public PrivateChatRestApi(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        // Bisq 2 registers exactly one two-party service, for ChatChannelDomain.DISCUSSION.
        channelService = chatService.getTwoPartyPrivateChatChannelService();
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        chatNotificationService = chatService.getChatNotificationService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
    }

    @POST
    @Path("/peers/{peerProfileId}/channel")
    @Operation(
            summary = "Find or create the private chat channel with a peer",
            description = "Returns the id of the existing channel with the peer, creating it if there is none. "
                    + "Creating is purely local — nothing is sent until the first message, so the peer learns "
                    + "nothing from this call. The channel belongs to the node's currently selected user "
                    + "identity, which this call cannot choose: with several identities, an existing "
                    + "conversation held under another one is not found and a new empty channel is returned "
                    + "instead.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Channel id returned",
                            content = @Content(schema = @Schema(implementation = FindOrCreateChannelResponse.class))),
                    @ApiResponse(responseCode = "400", description = "The peer profile ID is one of my own identities"),
                    @ApiResponse(responseCode = "404", description = "No user profile found for the given peer profile ID"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void findOrCreateChannel(@PathParam("peerProfileId") String peerProfileId,
                                    @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            // My own profile is published like any other, so without this the channel would be created
            // with an id of sorted(me, me) — and selected on the node, which surfaces it on desktop.
            if (userIdentityService.findUserIdentity(peerProfileId).isPresent()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST,
                        "Cannot open a private chat with yourself."));
                return;
            }
            userProfileService.findUserProfile(peerProfileId)
                    .ifPresentOrElse(peer -> {
                        // The ChatService wrapper, not channelService.findOrCreateChannel: selecting the
                        // channel is what keeps the next DM bound correctly. See the class javadoc.
                        chatService.createAndSelectTwoPartyPrivateChatChannel(ChatChannelDomain.DISCUSSION, peer)
                                .ifPresentOrElse(channel ->
                                                asyncResponse.resume(buildOkResponse(new FindOrCreateChannelResponse(channel.getId()))),
                                        () -> asyncResponse.resume(buildErrorResponse(
                                                "Could not create a private chat channel with " + peerProfileId)));
                    }, () -> asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND,
                            "No user profile found for profile ID " + peerProfileId)));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/messages")
    @Operation(
            summary = "Send a message to a private chat channel",
            description = "Sends a text message to the peer of the given channel. Optionally includes a citation "
                    + "reference. A 204 confirms the message was accepted and stored locally, not that the peer "
                    + "received it — delivery happens asynchronously on the P2P network. A 409 means the node "
                    + "refused it outright and stored nothing, so it will not appear on the message stream either.",
            requestBody = @RequestBody(
                    description = "Message to send to the peer",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendChatMessageRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Message sent successfully"),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "400", description = "Invalid input, e.g. empty text"),
                    @ApiResponse(responseCode = "409",
                            description = "Refused locally: either my own profile or the peer is banned"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void sendTextMessage(@PathParam("channelId") String channelId,
                                SendChatMessageRequest request,
                                @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            // Jersey hands us null for an absent body, which would otherwise NPE into a 500.
            if (request == null || StringUtils.isEmpty(request.text())) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "text must not be empty."));
                return;
            }
            withChannel(channelId, asyncResponse, channel -> {
                Optional<Citation> citation = Optional.ofNullable(request.citation())
                        .map(DtoMappings.CitationMapping::toBisq2Model);
                resume(channelService.trySendTextMessage(request.text(), citation, channel), asyncResponse);
            });
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/{messageId}/reactions")
    @Operation(
            summary = "Send or remove a private chat message reaction",
            description = "Adds or removes a reaction on a message within a private chat channel. As with sending a "
                    + "message, a 204 confirms local acceptance rather than delivery to the peer, and a 409 means "
                    + "the node refused it outright and stored nothing.",
            requestBody = @RequestBody(
                    description = "Request containing the reaction data to be added or removed",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendChatMessageReactionRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204",
                            description = "Reaction processed successfully, or already present (idempotent)"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or missing required fields"),
                    @ApiResponse(responseCode = "404", description = "No channel or message found for the given IDs"),
                    @ApiResponse(responseCode = "409",
                            description = "Refused locally: either my own profile or the peer is banned"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void sendChatMessageReaction(@PathParam("channelId") String channelId,
                                        @PathParam("messageId") String messageId,
                                        SendChatMessageReactionRequest request,
                                        @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            // Jersey hands us null for an absent body, which would otherwise NPE into a 500.
            if (request == null) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "A request body is required."));
                return;
            }
            String senderUserProfileId = request.senderUserProfileId();
            boolean isRemoveRequest = request.isRemoved();
            // senderUserProfileId below is checked for sanity only; it does not pick the sender. The
            // reaction goes out as the channel's own identity (PrivateChatChannelService#sendMessageReaction),
            // so naming a different one of my identities here changes nothing. Kept because the trade
            // endpoint the mobile client already targets validates the same field the same way.
            if (isRemoveRequest) {
                if (StringUtils.isEmpty(senderUserProfileId)) {
                    asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST,
                            "For remove requests, senderUserProfileId must not be empty."));
                    return;
                }
                if (userIdentityService.findUserIdentity(senderUserProfileId).isEmpty()) {
                    asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST,
                            "No user identity found for senderUserProfileId: " + senderUserProfileId));
                    return;
                }
            }

            int reactionId = request.reactionId();
            // Bounds-checked rather than letting values()[reactionId] throw, which would surface a client
            // input error as a 500.
            Reaction[] reactions = Reaction.values();
            if (reactionId < 0 || reactionId >= reactions.length) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST,
                        "Unsupported reactionId: " + reactionId));
                return;
            }
            Reaction reaction = reactions[reactionId];

            // A missing channel is reported apart from a missing message, so the caller is pointed at
            // the half of the request that is actually wrong.
            withChannel(channelId, asyncResponse, channel -> {
                Optional<TwoPartyPrivateChatMessage> optionalMessage = channel.getChatMessages().stream()
                        .filter(chatMessage -> chatMessage.getId().equals(messageId))
                        .findFirst();
                if (optionalMessage.isEmpty()) {
                    asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND,
                            "No message found for message ID: " + messageId));
                    return;
                }
                TwoPartyPrivateChatMessage message = optionalMessage.get();
                if (isRemoveRequest || !alreadyReactedWith(message, reactionId)) {
                    // Only this branch can answer 409: the idempotent no-op below neither sends nor stores
                    // anything, so its 204 stays honest even when a send would have been refused.
                    resume(channelService.trySendTextMessageReaction(message, channel, reaction, isRemoveRequest),
                            asyncResponse);
                    return;
                }
                // Idempotent on the add side: re-sending a reaction the user already has is a no-op, and a
                // no-op is a success, so it answers 204 like any other. The remove side is not guarded,
                // so a repeated remove publishes a second removal reaction to the network. Left that way
                // deliberately: it is the shape of the trade endpoint the mobile client already targets
                // (TradeChatMessagesRestApi#sendChatMessageReaction), and diverging here would be a worse
                // trade than one redundant P2P message. Worth fixing in both at once, not in one.
                asyncResponse.resume(buildNoContentResponse());
            });
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/leave")
    @Operation(
            summary = "Leave a private chat channel",
            description = "Leaves and deletes the channel locally, notifying the peer. One-sided and irreversible: "
                    + "the peer keeps their copy of the conversation and sees a 'left' system message.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Channel left successfully"),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void leaveChannel(@PathParam("channelId") String channelId,
                             @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            withChannel(channelId, asyncResponse, channel -> {
                // Not channelService.leaveChannel: the manager additionally re-selects the next channel
                // and consumes the departed channel's notifications, as desktop does.
                leavePrivateChatManager.leaveChannel(channel);
                asyncResponse.resume(buildNoContentResponse());
            });
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/consume-notifications")
    @Operation(
            summary = "Mark every message in a private chat channel as read",
            description = "Consumes the channel's notifications in Bisq 2's persisted notification store, "
                    + "which is what the unread count on the channel is read from.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Notifications consumed successfully"),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void consumeNotifications(@PathParam("channelId") String channelId,
                                     @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            withChannel(channelId, asyncResponse, channel -> {
                chatNotificationService.consume(channel);
                asyncResponse.resume(buildNoContentResponse());
            });
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    private void applyTimeout(AsyncResponse asyncResponse) {
        // Timeout for internal processing, not for the socket
        asyncResponse.setTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response ->
                response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out")));
    }

    /** Whether any of my identities already holds this reaction on the message. */
    private boolean alreadyReactedWith(TwoPartyPrivateChatMessage message, int reactionId) {
        Set<String> myUserProfileIds = userIdentityService.getMyUserProfileIds();
        return message.getChatMessageReactions().stream()
                .anyMatch(messageReaction -> !messageReaction.isRemoved() &&
                        messageReaction.getReactionId() == reactionId &&
                        myUserProfileIds.contains(messageReaction.getSenderUserProfile().getId()));
    }

    private void withChannel(String channelId,
                             AsyncResponse asyncResponse,
                             Consumer<TwoPartyPrivateChatChannel> handler) {
        channelService.findChannel(channelId)
                .ifPresentOrElse(handler, () -> asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND,
                        "No channel found for channel ID " + channelId)));
    }

    /**
     * Answers from the decision the send itself took, never from a check made before it. The two would be
     * separate decisions, and only the send's is authoritative — a ban landing in between would leave the
     * response contradicting what the node actually did.
     * <p>
     * Only the local half is reported. Delivery resolves long after this returns, so a 204 means the node
     * accepted and stored the message, not that the peer has it; that distinction is documented on both
     * endpoints and belongs to a delivery-status topic, not to this response.
     */
    private void resume(SendOutcome outcome, AsyncResponse asyncResponse) {
        asyncResponse.resume(outcome.rejection()
                .map(reason -> buildResponse(Response.Status.CONFLICT, describe(reason)))
                .orElseGet(this::buildNoContentResponse));
    }

    /**
     * Not 403: the caller is authorised — 403 is what the permission filter answers — and what refuses
     * the send is the state of the conversation, not the client's grant.
     */
    private static String describe(SendRejection rejection) {
        return switch (rejection) {
            case MY_PROFILE_BANNED -> "Your user profile is banned.";
            case PEER_BANNED -> "The peer's user profile is banned.";
        };
    }
}
