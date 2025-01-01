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

package bisq.http_api.rest_api.domain.offerbook;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.dto.OfferListItemDtoFactory;
import bisq.dto.offer.bisq_easy.OfferListItemDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Path("/offerbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Offerbook API")
public class OfferbookRestApi extends RestApiBase {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MarketPriceService marketPriceService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;
    private final UserIdentityService userIdentityService;

    public OfferbookRestApi(BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService,
                            MarketPriceService marketPriceService,
                            UserService userService) {
        this.bisqEasyOfferbookChannelService = bisqEasyOfferbookChannelService;
        this.marketPriceService = marketPriceService;
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
        reputationService = userService.getReputationService();
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
                    .filter(market -> marketPriceService.getMarketPriceByCurrencyMap().isEmpty() ||
                            marketPriceService.getMarketPriceByCurrencyMap().containsKey(market))
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

    @Operation(
            summary = "Retrieve Offers for a Market",
            description = "Fetches a list of offers for the specified currency code. " +
                    "The market is determined using the 'BTC/{currencyCode}' format.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Offers retrieved successfully.",
                            content = @Content(schema = @Schema(implementation = OfferListItemDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "No offers found for the specified currency code."
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error occurred while processing the request."
                    )
            }
    )
    @GET
    @Path("markets/{currencyCode}/offers")
    public Response getOffers(@PathParam("currencyCode") String currencyCode) {
        try {
            String marketCodes = "BTC/" + currencyCode.toUpperCase();
            return findOffer(marketCodes)
                    .map(this::buildOkResponse)
                    .orElseGet(() -> {
                        log.warn("No offers found for market: {}", marketCodes);
                        return buildNotFoundResponse("No offers found for the specified market.");
                    });

        } catch (Exception e) {
            log.error("Error while fetching offers for currency code: {}", currencyCode, e);
            return buildErrorResponse("An unexpected error occurred while processing the request.");
        }
    }

    private Optional<List<OfferListItemDto>> findOffer(String marketCodes) {
        return MarketRepository.findAnyFiatMarketByMarketCodes(marketCodes)
                .flatMap(market -> bisqEasyOfferbookChannelService.findChannel(market)
                        .map(channel -> channel.getChatMessages()
                                .stream()
                                .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                                .map(this::createOfferListItemDto)
                                .collect(Collectors.toList())
                        )
                );
    }

    private OfferListItemDto createOfferListItemDto(BisqEasyOfferbookMessage bisqEasyOfferbookMessage) {
        return OfferListItemDtoFactory.createOfferListItemDto(userProfileService,
                userIdentityService,
                reputationService,
                marketPriceService,
                bisqEasyOfferbookMessage);
    }
}
