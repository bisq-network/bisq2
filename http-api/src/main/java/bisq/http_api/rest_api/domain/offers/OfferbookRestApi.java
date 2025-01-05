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

package bisq.http_api.rest_api.domain.offers;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodUtil;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.bisq_easy.BisqEasyServiceUtil;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.dto.DtoMappings;
import bisq.dto.OfferListItemDtoFactory;
import bisq.dto.offer.bisq_easy.OfferListItemDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.UserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
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

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/offerbook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Offer API")
public class OfferbookRestApi extends RestApiBase {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final UserProfileService userProfileService;
    private final ReputationService reputationService;


    public OfferbookRestApi(ChatService chatService,
                            MarketPriceService marketPriceService,
                            UserService userService
    ) {
        this.bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        this.marketPriceService = marketPriceService;
        userIdentityService = userService.getUserIdentityService();
        userProfileService = userService.getUserProfileService();
        reputationService = userService.getReputationService();
    }

    @DELETE
    @Operation(
            summary = "Delete Bisq Easy Offer",
            description = "Delete a Bisq Easy Offer from the network.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Offer successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Offer or user identity not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    @Path("/offers/{offerId}")
    public void deleteOffer(@PathParam("offerId") String offerId, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Optional<BisqEasyOfferbookMessage> optionalOfferbookMessage = bisqEasyOfferbookChannelService.findMessageByOfferId(offerId);
            if (optionalOfferbookMessage.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "Offer not found"));
                return;
            }

            BisqEasyOfferbookMessage offerbookMessage = optionalOfferbookMessage.get();
            Optional<UserIdentity> optionalUserIdentity = userIdentityService.findUserIdentity(offerbookMessage.getAuthorUserProfileId());
            if (optionalUserIdentity.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "User identity for offer not found"));
                return;
            }

            UserIdentity userIdentity = optionalUserIdentity.get();
            bisqEasyOfferbookChannelService.deleteChatMessage(offerbookMessage, userIdentity.getNetworkIdWithKeyPair()).get();
            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (ExecutionException e) {
            asyncResponse.resume(buildErrorResponse("Failed to delete the offer: " + e.getCause().getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @POST
    @Operation(
            summary = "Create and Publish Bisq Easy Offer",
            description = "Creates a Bisq Easy Offer and publish it to the network.",
            requestBody = @RequestBody(
                    description = "",
                    content = @Content(schema = @Schema(implementation = CreateOfferRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "",
                            content = @Content(schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/offers")
    public void createOffer(CreateOfferRequest request, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
            Direction direction = DtoMappings.DirectionMapping.toBisq2Model(request.direction());
            Market market = DtoMappings.MarketMapping.toBisq2Model(request.market());
            List<BitcoinPaymentMethod> bitcoinPaymentMethods = request.bitcoinPaymentMethods().stream()
                    .map(BitcoinPaymentMethodUtil::getPaymentMethod)
                    .collect(Collectors.toList());
            List<FiatPaymentMethod> fiatPaymentMethods = request.fiatPaymentMethods().stream()
                    .map(FiatPaymentMethodUtil::getPaymentMethod)
                    .collect(Collectors.toList());
            AmountSpec amountSpec = DtoMappings.AmountSpecMapping.toBisq2Model(request.amountSpec());
            PriceSpec priceSpec = DtoMappings.PriceSpecMapping.toBisq2Model(request.priceSpec());
            List<String> supportedLanguageCodes = new ArrayList<>(request.supportedLanguageCodes());
            String chatMessageText = BisqEasyServiceUtil.createOfferBookMessageFromPeerPerspective(userIdentity.getNickName(),
                    marketPriceService,
                    direction,
                    market,
                    bitcoinPaymentMethods,
                    fiatPaymentMethods,
                    amountSpec,
                    priceSpec);
            UserProfile userProfile = userIdentity.getUserProfile();
            BisqEasyOffer bisqEasyOffer = new BisqEasyOffer(
                    userProfile.getNetworkId(),
                    direction,
                    market,
                    amountSpec,
                    priceSpec,
                    bitcoinPaymentMethods,
                    fiatPaymentMethods,
                    userProfile.getTerms(),
                    supportedLanguageCodes);
            String channelId = bisqEasyOfferbookChannelService.findChannel(market).orElseThrow().getId();
            BisqEasyOfferbookMessage myOfferMessage = new BisqEasyOfferbookMessage(channelId,
                    userProfile.getId(),
                    Optional.of(bisqEasyOffer),
                    Optional.of(chatMessageText),
                    Optional.empty(),
                    new Date().getTime(),
                    false);
            bisqEasyOfferbookChannelService.publishChatMessage(myOfferMessage, userIdentity).get();
            asyncResponse.resume(buildResponse(Response.Status.CREATED, new CreateOfferResponse(bisqEasyOffer.getId())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
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
