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

package bisq.bonded_roles.market_price;

import bisq.common.currency.Market;
import bisq.common.rest_api.RestApiBase;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Path("/market-price")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Market Price API")
public class MarketPriceRestApi extends RestApiBase {

    private final MarketPriceService marketPriceService;

    public MarketPriceRestApi(MarketPriceService marketPriceService) {
        this.marketPriceService = marketPriceService;
    }

    @GET
    @Path("/quotes")
    @Operation(
            summary = "Get all market price quotes",
            description = "Retrieve all market price quotes",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Market price quotes retrieved successfully",
                            content = @Content(schema = @Schema(implementation = MarketPriceResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Market price quotes not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getQuotes() {
        try {
            Map<Market, MarketPrice> marketPriceByCurrencyMap = marketPriceService.getMarketPriceByCurrencyMap();
            Map<String, Long> result = marketPriceService.getMarketPriceByCurrencyMap()
                    .entrySet().stream()
                    .filter(entry->entry.getKey().getBaseCurrencyCode().equals("BTC")) // We get altcoin quotes as well
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().getQuoteCurrencyCode(),
                            entry -> entry.getValue().getPriceQuote().getValue()
                    ));

            if (result.isEmpty()) {
                return buildNotFoundResponse("No market price quotes found.");
            }

            return buildOkResponse(new MarketPriceResponse(result));
        } catch (Exception ex) {
            log.error("Failed to retrieve market price quotes", ex);
            return buildErrorResponse("An error occurred while retrieving market prices.");
        }
    }

    /**
     * Response DTO for market price quotes.
     */
    @Getter
    public static class MarketPriceResponse {
        @Schema(description = "Map of currency codes to market price quotes")
        private final Map<String, Long> quotes;

        public MarketPriceResponse(Map<String, Long> quotes) {
            this.quotes = quotes;
        }
    }
}
