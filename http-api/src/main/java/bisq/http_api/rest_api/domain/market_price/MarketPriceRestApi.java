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

package bisq.http_api.rest_api.domain.market_price;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.dto.DtoMappings;
import bisq.dto.common.monetary.PriceQuoteDto;
import bisq.http_api.rest_api.domain.RestApiBase;
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
            Map<String, PriceQuoteDto> result = marketPriceService.getMarketPriceByCurrencyMap()
                    .entrySet().stream()
                    .filter(entry->entry.getKey().getBaseCurrencyCode().equals("BTC")) // We get altcoin quotes as well
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().getQuoteCurrencyCode(),
                            entry -> DtoMappings.PriceQuoteMapping.from(entry.getValue().getPriceQuote())
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
}
