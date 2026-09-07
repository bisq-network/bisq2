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

import bisq.api.chat.common.PublicChatChannels;
import bisq.api.chat.common.PublicChatDtoFactory;
import bisq.api.dto.DtoMappings;
import bisq.api.dto.chat.SendRejectionDto;
import bisq.api.dto.chat.common.CommonPublicChatChannelDto;
import bisq.api.dto.chat.common.CommonPublicChatMessageDto;
import bisq.api.dto.mappings.chat.common.CommonPublicChatChannelDtoMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.api.rest_api.endpoints.chat.SendChatMessageReactionRequest;
import bisq.api.rest_api.endpoints.chat.SendRefusedResponse;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.notifications.ChatNotificationService;
import bisq.chat.reactions.CommonPublicChatMessageReaction;
import bisq.chat.reactions.Reaction;
import bisq.common.util.StringUtils;
import bisq.network.p2p.services.data.BroadcastResult;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static bisq.api.rest_api.endpoints.chat.ChatRequestValidation.citationError;
import static bisq.api.rest_api.endpoints.chat.ChatRequestValidation.parseReaction;
import static bisq.api.rest_api.endpoints.chat.ChatRequestValidation.textError;

/**
 * The public chat channels, Discussions and Support. Structurally {@code PrivateChatRestApi}, with the
 * differences a public channel brings: the channels are fixed, so nothing is created or left; a channel
 * has no identity of its own, so sending and reacting take the sender from the request and editing,
 * deleting and un-reacting use the author's identity; and messages are removed for real, which the
 * messages topic reports as REMOVED events. The history is delivered whole, see
 * {@link PublicChatDtoFactory#visibleMessagesNewestFirst}.
 * <p>
 * Every path addresses a channel by id, so the two channels differ only in which
 * {@code CommonPublicChatChannelService} publishes for them; {@link PublicChatChannels} resolves that.
 * <p>
 * A 204 means the node applied the change locally and started broadcasting it, never that peers have
 * it. Where the local change happens asynchronously (edit, delete, un-react all go through the P2P
 * store's remove), the response waits for it, so the matching websocket event has been emitted before
 * the 204 goes out. Emitted, not delivered: the frame is written by a per-subscriber executor the
 * response does not wait for, so a client must tolerate the 204 and the event in either order.
 */
@Slf4j
@Path("/public-chat-channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Public Chat API", description = "Endpoints for the public chat channels, Discussions and Support")
public class PublicChatRestApi extends RestApiBase {
    private final PublicChatChannels channels;
    private final ChatNotificationService chatNotificationService;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final PublicChatDtoFactory dtoFactory;

    public PublicChatRestApi(ChatService chatService, UserService userService) {
        channels = new PublicChatChannels(chatService);
        chatNotificationService = chatService.getChatNotificationService();
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        dtoFactory = new PublicChatDtoFactory(userService.getUserProfileService(), userService.getBannedUserService());
    }

    @GET
    @Operation(
            summary = "List the public chat channels",
            description = "Returns the Discussions and Support channels with their unread counts. A count "
                    + "follows the channel's notification setting on the node: with the global default set "
                    + "to mentions only, a channel with new messages reports 0.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Channels returned",
                            content = @Content(schema = @Schema(implementation = CommonPublicChatChannelDto[].class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public Response getChannels() {
        try {
            List<CommonPublicChatChannelDto> dtos = channels.getChannels()
                    .map(channel -> CommonPublicChatChannelDtoMapping.fromBisq2Model(channel,
                            chatNotificationService.getNumNotifications(channel)))
                    .toList();
            return buildOkResponse(dtos);
        } catch (Exception e) {
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Path("/{channelId}/messages")
    @Operation(
            summary = "Read the channel's whole history, newest first",
            description = "Returns every visible message; the history is bounded by the P2P store's 10-day "
                    + "TTL, and the mobile client needs all of it for its local message search. Messages "
                    + "from banned or unresolved authors and expired messages are left out; messages from "
                    + "ignored users are not — the client filters those against GET /user-profiles/ignored.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Messages returned",
                            content = @Content(schema = @Schema(implementation = CommonPublicChatMessageDto[].class))),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public Response getMessages(@PathParam("channelId") String channelId) {
        try {
            Optional<CommonPublicChatChannel> channel = channels.findChannel(channelId);
            if (channel.isEmpty()) {
                return buildNotFoundResponse("No channel found for channel ID " + channelId);
            }
            return buildOkResponse(dtoFactory.visibleMessageDtosNewestFirst(channel.get().getChatMessages()));
        } catch (Exception e) {
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Path("/{channelId}/messages")
    @Operation(
            summary = "Send a message to the channel",
            description = "Publishes a text message, optionally citing another one, as the given identity or "
                    + "as the node's selected identity when none is given. A 204 confirms the node accepted "
                    + "the message and started its broadcast, not that any peer has it. (In a rare race with "
                    + "the rate limiter the local add can be dropped while the broadcast still goes out; "
                    + "pre-existing domain behaviour, shared with desktop.) The message "
                    + "arrives on the PUBLIC_CHAT_MESSAGES topic like any other; its id is not returned.",
            requestBody = @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = SendPublicChatMessageRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Message accepted"),
                    @ApiResponse(responseCode = "400", description = "Invalid input, or no identity found for senderUserProfileId"),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "409", description = "Refused locally: the sending profile is banned",
                            content = @Content(schema = @Schema(implementation = SendRefusedResponse.class))),
                    @ApiResponse(responseCode = "429", description = "The sending profile exceeded the rate limit"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void sendTextMessage(@PathParam("channelId") String channelId,
                                SendPublicChatMessageRequest request,
                                @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            // Jersey hands us null for an absent body, which would otherwise NPE into a 500.
            if (request == null) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "A request body is required."));
                return;
            }
            Optional<String> inputError = textError(request.text()).or(() -> citationError(request.citation()));
            if (inputError.isPresent()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, inputError.get()));
                return;
            }
            Optional<UserIdentity> sender = findSender(request.senderUserProfileId());
            if (sender.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, noSender(request.senderUserProfileId())));
                return;
            }
            withChannel(channelId, asyncResponse, channel -> {
                Optional<Response> refusal = refusal(sender.get());
                if (refusal.isPresent()) {
                    asyncResponse.resume(refusal.get());
                    return;
                }
                Optional<Citation> citation = Optional.ofNullable(request.citation())
                        .map(DtoMappings.CitationMapping::toBisq2Model);
                try {
                    // The local add happens inside the call, before the publish; only the broadcast is async.
                    channels.serviceOf(channel).publishChatMessage(request.text(), citation, channel, sender.get())
                            .whenComplete(logIfFailed("Publishing a public chat message"));
                } catch (IllegalArgumentException e) {
                    // The domain repeats the ban and rate limit checks with checkArgument. If the state moved
                    // between our pre-check and the call, answer as the pre-check would have; anything else
                    // the domain rejects is a 400 like any other invalid input.
                    asyncResponse.resume(refusal(sender.get())
                            .orElseGet(() -> buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage())));
                    return;
                }
                asyncResponse.resume(buildNoContentResponse());
            });
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PUT
    @Path("/{channelId}/messages/{messageId}")
    @Operation(
            summary = "Edit one of my messages",
            description = "Replaces the text of a message written by one of my identities. On the network an edit "
                    + "is a removal plus a new message: the topic reports REMOVED for the old id and ADDED for a "
                    + "new one with wasEdited=true and the original date. The 204 is sent after the new "
                    + "message is stored locally, so it never precedes those events.",
            requestBody = @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = EditPublicChatMessageRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Message edited"),
                    @ApiResponse(responseCode = "400", description = "Invalid input, e.g. empty text"),
                    @ApiResponse(responseCode = "403", description = "The message was not written by one of my identities"),
                    @ApiResponse(responseCode = "404", description = "No channel or message found for the given IDs"),
                    @ApiResponse(responseCode = "409", description = "Refused locally: the author's profile is banned",
                            content = @Content(schema = @Schema(implementation = SendRefusedResponse.class))),
                    @ApiResponse(responseCode = "429", description = "The author's profile exceeded the rate limit"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500",
                            description = "The edit did not complete, in one of two ways: the original was "
                                    + "removed and the replacement never published, leaving nothing in its "
                                    + "place, or the replacement was published and the original could not be "
                                    + "removed locally, leaving both. Re-read the channel to tell which")
            }
    )
    public void editMessage(@PathParam("channelId") String channelId,
                            @PathParam("messageId") String messageId,
                            EditPublicChatMessageRequest request,
                            @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            if (request == null) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "A request body is required."));
                return;
            }
            Optional<String> inputError = textError(request.text());
            if (inputError.isPresent()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, inputError.get()));
                return;
            }
            withMessage(channelId, messageId, asyncResponse, (channel, message) -> {
                Optional<UserIdentity> author = userIdentityService.findUserIdentity(message.getAuthorUserProfileId());
                if (author.isEmpty()) {
                    asyncResponse.resume(buildResponse(Response.Status.FORBIDDEN, "Only the author can edit a message."));
                    return;
                }
                // As desktop does (ChatMessagesListController#onSaveEditedMessage), and for a stronger
                // reason: publishEditedChatMessage removes the original before its own checks run, so
                // republishing identical text is a way to lose the message for nothing in return. Answered
                // before the refusal below because nothing is published either way, so there is nothing to
                // refuse.
                if (message.getText().map(request.text()::equals).orElse(false)) {
                    asyncResponse.resume(buildNoContentResponse());
                    return;
                }
                // Not cosmetic here: the domain runs its ban and rate limit checks after it has removed the
                // original, so a failure inside the domain would lose the message.
                Optional<Response> refusal = refusal(author.get());
                if (refusal.isPresent()) {
                    asyncResponse.resume(refusal.get());
                    return;
                }
                channels.serviceOf(channel).publishEditedChatMessage(message, request.text(), author.get())
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                asyncResponse.resume(buildErrorResponse("Editing the message failed: " + throwable.getMessage()));
                            } else if (channel.getChatMessages().contains(message)) {
                                // The store answers a rejected removal with a successful, empty result and no
                                // event, and the domain then publishes the edited copy next to the original.
                                asyncResponse.resume(buildErrorResponse(
                                        "The edited message was published but the original could not be removed locally."));
                            } else {
                                asyncResponse.resume(buildNoContentResponse());
                            }
                        });
            });
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DELETE
    @Path("/{channelId}/messages/{messageId}")
    @Operation(
            summary = "Delete one of my messages",
            description = "Removes a message written by one of my identities from the network. The 204 is sent "
                    + "after the message is removed locally, so it never precedes the REMOVED event on the topic. "
                    + "A 409 or 429 means the node would have dropped the local removal, leaving the message "
                    + "on this node while the network forgets it, so nothing was done.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Message deleted"),
                    @ApiResponse(responseCode = "403", description = "The message was not written by one of my identities"),
                    @ApiResponse(responseCode = "404", description = "No channel or message found for the given IDs"),
                    @ApiResponse(responseCode = "409", description = "Refused locally: the author's profile is banned",
                            content = @Content(schema = @Schema(implementation = SendRefusedResponse.class))),
                    @ApiResponse(responseCode = "429", description = "The author's profile exceeded the rate limit"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "The message could not be removed locally, or an unexpected error")
            }
    )
    public void deleteMessage(@PathParam("channelId") String channelId,
                              @PathParam("messageId") String messageId,
                              @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            withMessage(channelId, messageId, asyncResponse, (channel, message) -> {
                Optional<UserIdentity> author = userIdentityService.findUserIdentity(message.getAuthorUserProfileId());
                if (author.isEmpty()) {
                    asyncResponse.resume(buildResponse(Response.Status.FORBIDDEN, "Only the author can delete a message."));
                    return;
                }
                // The domain has no check on the delete itself, but the store's removal listener
                // (PublicChatChannelService#processRemovedMessage) drops the local removal for a banned or
                // rate-limited author. The store would still mark the message removed and answer success,
                // so a retry finds nothing left to remove and the message is stuck on this node for good.
                // Seen live: three sends in a second followed by a delete.
                Optional<Response> refusal = refusal(author.get());
                if (refusal.isPresent()) {
                    asyncResponse.resume(refusal.get());
                    return;
                }
                channels.serviceOf(channel).deleteChatMessage(message, author.get().getNetworkIdWithKeyPair())
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                asyncResponse.resume(buildErrorResponse("Deleting the message failed: " + throwable.getMessage()));
                            } else if (channel.getChatMessages().contains(message)) {
                                asyncResponse.resume(buildErrorResponse("The message could not be removed locally."));
                            } else {
                                asyncResponse.resume(buildNoContentResponse());
                            }
                        });
            });
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/messages/{messageId}/reactions")
    @Operation(
            summary = "Add or remove a reaction on a message",
            description = "Adds the reaction as the given identity (or the node's selected one), or removes it "
                    + "when isRemoved is true, in which case senderUserProfileId is required to say whose "
                    + "reaction to remove. Adding is idempotent per identity for sequential requests. "
                    + "Removing waits for the local removal before answering, and is a no-op 204 when "
                    + "that identity holds no such reaction.",
            requestBody = @RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = SendChatMessageReactionRequest.class))),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Reaction applied, or nothing to do"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or missing required fields"),
                    @ApiResponse(responseCode = "404", description = "No channel or message found for the given IDs"),
                    @ApiResponse(responseCode = "409", description = "Refused locally: the sending profile is banned",
                            content = @Content(schema = @Schema(implementation = SendRefusedResponse.class))),
                    @ApiResponse(responseCode = "429", description = "The sending profile exceeded the rate limit"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "The reaction could not be removed locally, or an unexpected error")
            }
    )
    public void sendChatMessageReaction(@PathParam("channelId") String channelId,
                                        @PathParam("messageId") String messageId,
                                        SendChatMessageReactionRequest request,
                                        @Suspended AsyncResponse asyncResponse) {
        applyTimeout(asyncResponse);
        try {
            if (request == null) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "A request body is required."));
                return;
            }
            int reactionId = request.reactionId();
            Optional<Reaction> reaction = parseReaction(reactionId);
            if (reaction.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Unsupported reactionId: " + reactionId));
                return;
            }
            String senderUserProfileId = request.senderUserProfileId();
            boolean isRemoveRequest = request.isRemoved();
            // A public message carries reactions from many people and I may have several identities, so a
            // removal has to name whose reaction goes; falling back to the selected identity would guess.
            if (isRemoveRequest && StringUtils.isEmpty(senderUserProfileId)) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST,
                        "For remove requests, senderUserProfileId must not be empty."));
                return;
            }
            Optional<UserIdentity> sender = findSender(senderUserProfileId);
            if (sender.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, noSender(senderUserProfileId)));
                return;
            }
            withMessage(channelId, messageId, asyncResponse, (channel, message) -> {
                if (isRemoveRequest) {
                    removeReaction(channels.serviceOf(channel), message, sender.get(), reactionId, asyncResponse);
                    return;
                }
                // Mandatory: the domain answers a banned sender with a future failed on a bare
                // RuntimeException carrying no message, and the publish below is fire-and-forget, so
                // without this the client would get a 204 for a reaction that was never published.
                Optional<Response> refusal = refusal(sender.get());
                if (refusal.isPresent()) {
                    asyncResponse.resume(refusal.get());
                    return;
                }
                // Idempotent per identity, for sequential requests: the local add inside
                // publishChatMessageReaction is synchronous, so a retry finds it here and no-ops. Two
                // overlapping requests can both pass this check, and the domain does not dedupe — a
                // ChatMessageReaction carries its own uid and date, so the two are unequal and both land.
                // Another of my identities holding the same reaction is a different reaction anyway.
                if (findReaction(message, sender.get(), reactionId).isEmpty()) {
                    channels.serviceOf(channel).publishChatMessageReaction(message, reaction.get(), sender.get())
                            .whenComplete(logIfFailed("Publishing a public chat reaction"));
                }
                asyncResponse.resume(buildNoContentResponse());
            });
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Path("/{channelId}/consume-notifications")
    @Operation(
            summary = "Mark every message in the channel as read",
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

    private void removeReaction(CommonPublicChatChannelService channelService,
                                CommonPublicChatMessage message,
                                UserIdentity sender,
                                int reactionId,
                                AsyncResponse asyncResponse) {
        Optional<CommonPublicChatMessageReaction> reaction = findReaction(message, sender, reactionId);
        if (reaction.isEmpty()) {
            asyncResponse.resume(buildNoContentResponse());
            return;
        }
        channelService.deleteChatMessageReaction(reaction.get(), sender.getNetworkIdWithKeyPair())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        asyncResponse.resume(buildErrorResponse("Removing the reaction failed: " + throwable.getMessage()));
                    } else if (message.getChatMessageReactions().contains(reaction.get())) {
                        asyncResponse.resume(buildErrorResponse("The reaction could not be removed locally."));
                    } else {
                        asyncResponse.resume(buildNoContentResponse());
                    }
                });
    }

    private static Optional<CommonPublicChatMessageReaction> findReaction(CommonPublicChatMessage message,
                                                                         UserIdentity identity,
                                                                         int reactionId) {
        return PublicChatDtoFactory.reactionsOf(message)
                .filter(reaction -> reaction.getReactionId() == reactionId
                        && reaction.getUserProfileId().equals(identity.getId()))
                .findFirst();
    }

    /**
     * The identity a send goes out as: the named one, or the node's selected one when none is named.
     * Empty when the named one is not mine, or nothing is selected.
     */
    private Optional<UserIdentity> findSender(@Nullable String senderUserProfileId) {
        if (StringUtils.isEmpty(senderUserProfileId)) {
            return Optional.ofNullable(userIdentityService.getSelectedUserIdentity());
        }
        return userIdentityService.findUserIdentity(senderUserProfileId);
    }

    private static String noSender(@Nullable String senderUserProfileId) {
        return StringUtils.isEmpty(senderUserProfileId)
                ? "No user identity is selected on this node."
                : "No user identity found for senderUserProfileId: " + senderUserProfileId;
    }

    /**
     * The checks the domain makes with checkArgument, or not at all, made before it is called.
     * <p>
     * Not 403, because what refuses the send is the state of the profile rather than anything about the
     * request: a banned or rate-limited sender is answered the same way whatever it asked for. Editing
     * or deleting someone else's message does answer 403 — there the refusal is about this message and
     * this caller — and a client tells that apart from the permission filter's 403 by the body, which is
     * {@code {"error": "permission_not_granted", "required": …}} only when the filter is the one
     * refusing.
     */
    private Optional<Response> refusal(UserIdentity identity) {
        if (bannedUserService.isUserProfileBanned(identity.getId())) {
            return Optional.of(buildResponse(Response.Status.CONFLICT,
                    new SendRefusedResponse(SendRejectionDto.MY_PROFILE_BANNED, "Your user profile is banned.")));
        }
        if (bannedUserService.isRateLimitExceeding(identity.getId())) {
            return Optional.of(buildErrorResponse(Response.Status.TOO_MANY_REQUESTS,
                    "Rate limit exceeded. Wait before sending again."));
        }
        return Optional.empty();
    }

    /**
     * The broadcast is not awaited, so this is the only place its failure is seen. Banned is the known
     * case: the domain answers a banned reactor with a failed future rather than an exception, and the
     * ban can land between the pre-check and the call.
     */
    private static BiConsumer<BroadcastResult, Throwable> logIfFailed(String action) {
        return (result, throwable) -> {
            if (throwable != null) {
                log.warn("{} failed after the request was answered", action, throwable);
            }
        };
    }

    private void withChannel(String channelId,
                             AsyncResponse asyncResponse,
                             Consumer<CommonPublicChatChannel> handler) {
        channels.findChannel(channelId)
                .ifPresentOrElse(handler, () -> asyncResponse.resume(
                        buildNotFoundResponse("No channel found for channel ID " + channelId)));
    }

    /**
     * A missing channel is reported apart from a missing message, so the caller is pointed at the half
     * of the request that is actually wrong.
     */
    private void withMessage(String channelId,
                             String messageId,
                             AsyncResponse asyncResponse,
                             BiConsumer<CommonPublicChatChannel, CommonPublicChatMessage> handler) {
        withChannel(channelId, asyncResponse, channel -> {
            // Ids are not unique within a channel, so a peer can park a decoy under the id of the
            // caller's own message; resolved by iteration order, the decoy would shadow it and turn
            // its edit or delete into a spurious 403. Among colliders the caller's own message wins;
            // with none of them the caller's — the whole reacting case — this is findFirst as before.
            List<CommonPublicChatMessage> matches = channel.getChatMessages().stream()
                    .filter(message -> message.getId().equals(messageId))
                    .toList();
            matches.stream()
                    .filter(message -> userIdentityService.findUserIdentity(message.getAuthorUserProfileId()).isPresent())
                    .findFirst()
                    .or(() -> matches.stream().findFirst())
                    .ifPresentOrElse(message -> handler.accept(channel, message), () -> asyncResponse.resume(
                            buildNotFoundResponse("No message found for message ID: " + messageId)));
        });
    }
}
