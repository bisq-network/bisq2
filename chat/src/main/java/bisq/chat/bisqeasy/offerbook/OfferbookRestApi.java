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

package bisq.chat.bisqeasy.offerbook;

import bisq.common.currency.Market;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Path("/offerbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Offerbook API")
public class OfferbookRestApi {

    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;

    public OfferbookRestApi(BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService) {
        this.bisqEasyOfferbookChannelService = bisqEasyOfferbookChannelService;
    }

    /**
     * Retrieves a list of markets.
     *
     * @return List of {@link Market} objects.
     */
    @Operation(
            summary = "Returns a list of markets",
            description = "Fetches and returns a list of all available markets.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Markets retrieved successfully",
                            content = @Content(schema = @Schema(type = "array", implementation = Market.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GET
    @Path("markets")
    public Response getMarkets() {
        try {
            List<Market> markets = bisqEasyOfferbookChannelService.getChannels().stream()
                    .map(BisqEasyOfferbookChannel::getMarket)
                    .collect(Collectors.toList());
            return buildOkResponse(markets);
        } catch (Exception e) {
            log.error("Error retrieving markets", e);
            return buildErrorResponse("Failed to retrieve markets");
        }
    }

    /**
     * Retrieves a map of the number of offers per market code.
     *
     * @return A map where the key is the market code, and the value is the number of offers.
     */
    @Operation(
            summary = "Returns a map of the number of offers per market code",
            description = "Fetches and returns a map containing the number of offers for each market code.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Offer counts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = Map.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @GET
    @Path("markets/offers/count")
    public Response getNumOffersByMarketCode() {
        try {
            Map<String, Integer> numOffersByMarketCode = bisqEasyOfferbookChannelService.getChannels().stream()
                    .collect(Collectors.toMap(
                            channel -> channel.getMarket().getQuoteCurrencyCode(),
                            channel -> (int) channel.getChatMessages().stream()
                                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                                    .count()
                    ));
            return buildOkResponse(numOffersByMarketCode);
        } catch (Exception e) {
            log.error("Error retrieving offer counts by market code", e);
            return buildErrorResponse("Failed to retrieve offer counts");
        }
    }

    /**
     * Builds a successful 200 OK response.
     *
     * @param entity The response entity.
     * @return The HTTP response.
     */
    private Response buildOkResponse(Object entity) {
        return Response.status(Response.Status.OK)
                .entity(entity)
                .build();
    }

    /**
     * Builds an error response with a 500 status.
     *
     * @param message The error message.
     * @return The HTTP response.
     */
    private Response buildErrorResponse(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", message))
                .build();
    }
}
