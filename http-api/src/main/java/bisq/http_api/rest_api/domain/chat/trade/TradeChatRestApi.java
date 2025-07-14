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

package bisq.http_api.rest_api.domain.chat.trade;

import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.reactions.Reaction;
import bisq.common.util.StringUtils;
import bisq.dto.DtoMappings;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/trade-chat-channels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Trade Chat API", description = "Endpoints for chat communication between Bisq Easy Trade participants")
public class TradeChatRestApi extends RestApiBase {
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final UserIdentityService userIdentityService;

    public TradeChatRestApi(ChatService chatService, UserService userService) {
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        userIdentityService = userService.getUserIdentityService();
    }

    @POST
    @Path("/{channelId}/messages")
    @Operation(
            summary = "Send a chat message to a trade channel",
            description = "Sends a text message to a participant in a given trade channel. Optionally includes a citation reference.",
            requestBody = @RequestBody(
                    description = "Message to send to the channel participant",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendChatMessageRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Message sent successfully"),
                    @ApiResponse(responseCode = "404", description = "No channel found for given channel ID"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void sendTextMessage(@PathParam("channelId") String channelId,
                                SendChatMessageRequest request,
                                @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(120, TimeUnit.SECONDS); // Timeout for internal processing, not for socket
        asyncResponse.setTimeoutHandler(response ->
                response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"))
        );

        try {
            bisqEasyOpenTradeChannelService.findChannel(channelId)
                    .ifPresentOrElse(channel -> {
                        Optional<Citation> citation = Optional.ofNullable(request.citation())
                                .map(DtoMappings.CitationMapping::toBisq2Model);
                        bisqEasyOpenTradeChannelService.sendTextMessage(request.text(), citation, channel);
                        asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
                    }, () -> {
                        asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND,
                                "No channel found for channel ID " + channelId));
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
            summary = "Send or remove a chat message reaction",
            description = "Adds or removes a reaction from a chat message within a specific channel.",
            requestBody = @RequestBody(
                    description = "Request containing the reaction data to be added or removed",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SendChatMessageReactionRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Reaction processed successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input or missing required fields"),
                    @ApiResponse(responseCode = "404", description = "No channel or message found for the given IDs"),
                    @ApiResponse(responseCode = "503", description = "Request timed out"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public void sendChatMessageReaction(
            @PathParam("channelId") String channelId,
            @PathParam("messageId") String messageId,
            SendChatMessageReactionRequest request,
            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(120, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response ->
                response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"))
        );

        try {
            String senderUserProfileId = request.senderUserProfileId();
            boolean isRemoveRequest = request.isRemoved();
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
            Reaction reaction = Reaction.values()[reactionId];

            Set<String> myUserProfileIds = userIdentityService.getUserIdentities().stream()
                    .map(UserIdentity::getId)
                    .collect(Collectors.toSet());

            boolean wasSent = bisqEasyOpenTradeChannelService.findChannel(channelId)
                    .flatMap(channel -> channel.getChatMessages().stream()
                            .filter(message -> message.getId().equals(messageId))
                            .findFirst()
                            .map(message -> {
                                if (!isRemoveRequest && message.getChatMessageReactions().stream().anyMatch(
                                        messageReaction -> !messageReaction.isRemoved() &&
                                                messageReaction.getReactionId() == reactionId &&
                                                myUserProfileIds.contains(messageReaction.getSenderUserProfile().getId()))) {
                                    return false; // Reaction already exists from the same user
                                }
                                bisqEasyOpenTradeChannelService.sendTextMessageReaction(message, channel, reaction, isRemoveRequest);
                                return true;
                            }))
                    .orElse(false);

            if (wasSent) {
                asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
            } else {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND,
                        "No message found for message ID: " + messageId));
            }
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }
}
