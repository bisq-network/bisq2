package bisq.restApi.endpoints;

import bisq.application.DefaultApplicationService;
import bisq.identity.IdentityService;
import bisq.restApi.RestApiApplication;
import bisq.restApi.dto.PublicDiscussionChannelDto;
import bisq.restApi.dto.PublicTradeChannelDto;
import bisq.social.chat.ChatService;
import bisq.social.user.ChatUserService;
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
    private final ChatUserService chatUserService;
    private final IdentityService identityService;

    public ChatApi(@Context Application application) {
        DefaultApplicationService appService = ((RestApiApplication) application).getApplicationService();
        chatService = appService.getChatService();
        chatUserService = appService.getChatUserService();
        identityService = appService.getIdentityService();
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
        return chatService.getPublicDiscussionChannels().stream() //
                .map(publicDiscussionChannel -> new PublicDiscussionChannelDto().read(publicDiscussionChannel)) //
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
        return chatService.getPublicTradeChannels().stream() //
                .map(publicTradeChannel -> new PublicTradeChannelDto().read(publicTradeChannel)) //
                .collect(Collectors.toList());
    }
}
