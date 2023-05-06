package bisq.restApi.endpoints;

import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.restApi.RestApiApplication;
import bisq.restApi.dto.PublicDiscussionChannelDto;
import bisq.restApi.dto.PublicTradeChannelDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Chat API")
public class ChatApi {

    private final ChatService chatService;

    public ChatApi(@Context Application application) {
        DefaultApplicationService appService = ((RestApiApplication) application).getApplicationService();
        chatService = appService.getChatService();
    }

    @GET
    @Path("/public-discussion-channels")
    @Operation(description = "Get a list of all publicly available Discussion Channels.")
    @ApiResponse(responseCode = "200", description = "request successful.",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = PublicDiscussionChannelDto.class)
                    )}
    )
    public List<PublicDiscussionChannelDto> getPublicDiscussionChannels() {
        return chatService.getPublicDiscussionChannelService().getChannels().stream()
                .map(PublicDiscussionChannelDto::from)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/public-trade-channels")
    @Operation(description = "Get a list of all publicly available Trade Channels.")
    @ApiResponse(responseCode = "200", description = "request successful.",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = PublicTradeChannelDto.class)
                    )}
    )
    public List<PublicTradeChannelDto> getPublicTradeChannels() {
        return chatService.getPublicBisqEasyOfferChatChannelService().getChannels().stream()
                .map(PublicTradeChannelDto::from)
                .collect(Collectors.toList());
    }
}
