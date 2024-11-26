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

import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.common.rest_api.RestApiBase;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.user.UserService;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.util.Date;
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
                                .map(message -> {
                                    BisqEasyOffer bisqEasyOffer = message.getBisqEasyOffer().orElseThrow();
                                    long date = message.getDate();
                                    String formattedDate = DateFormatter.formatDateTime(new Date(date), DateFormat.MEDIUM, DateFormat.SHORT,
                                            true, " " + Res.get("temporal.at") + " ");
                                    String authorUserProfileId = message.getAuthorUserProfileId();
                                    Optional<UserProfile> senderUserProfile = userProfileService.findUserProfile(authorUserProfileId);
                                    String nym = senderUserProfile.map(UserProfile::getNym).orElse("");
                                    String userName = senderUserProfile.map(UserProfile::getUserName).orElse("");

                                    ReputationScoreDto reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore)
                                            .map(score -> new ReputationScoreDto(
                                                    score.getTotalScore(),
                                                    score.getFiveSystemScore(),
                                                    score.getRanking()
                                            ))
                                            .orElse(new ReputationScoreDto(0, 0, 0));
                                    AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
                                    PriceSpec priceSpec = bisqEasyOffer.getPriceSpec();
                                    boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
                                    // Market   market= bisqEasyOffer.getMarket();
                                    String formattedQuoteAmount = OfferAmountFormatter.formatQuoteAmount(
                                            marketPriceService,
                                            amountSpec,
                                            priceSpec,
                                            market,
                                            hasAmountRange,
                                            true
                                    );
                                    String formattedPrice = PriceSpecFormatter.getFormattedPriceSpec(priceSpec, true);

                                    List<String> quoteSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getQuoteSidePaymentMethodSpecs())
                                            .stream()
                                            .map(PaymentMethod::getName)
                                            .collect(Collectors.toList());

                                    List<String> baseSidePaymentMethods = PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getBaseSidePaymentMethodSpecs())
                                            .stream()
                                            .map(PaymentMethod::getName)
                                            .collect(Collectors.toList());

                                    String supportedLanguageCodes = Joiner.on(",").join(bisqEasyOffer.getSupportedLanguageCodes());
                                    boolean isMyMessage = message.isMyMessage(userIdentityService);
                                    Direction direction = bisqEasyOffer.getDirection();
                                    String offerTitle = getOfferTitle(message, direction, isMyMessage);
                                    String messageId = message.getId();
                                    String offerId = bisqEasyOffer.getId();
                                    return new OfferListItemDto(messageId,
                                            offerId,
                                            isMyMessage,
                                            direction,
                                            offerTitle,
                                            date,
                                            formattedDate,
                                            nym,
                                            userName,
                                            reputationScore,
                                            formattedQuoteAmount,
                                            formattedPrice,
                                            quoteSidePaymentMethods,
                                            baseSidePaymentMethods,
                                            supportedLanguageCodes);
                                })
                                .collect(Collectors.toList())
                        )
                );
    }

    private String getOfferTitle(BisqEasyOfferbookMessage message, Direction direction, boolean isMyMessage) {
        if (isMyMessage) {
            String directionString = StringUtils.capitalize(Res.get("offer." + direction.name().toLowerCase()));
            return Res.get("bisqEasy.tradeWizard.review.chatMessage.myMessageTitle", directionString);
        } else {
            return message.getText();
        }
    }

    @Getter
    @ToString
    @Schema(name = "OfferListItem", description = "Detailed information about an offer in the offerbook.")
    public static class OfferListItemDto {
        @Schema(description = "Unique identifier for the message.", example = "msg-123456")
        private final String messageId;
        @Schema(description = "Unique identifier for the offer.", example = "offer-987654")
        private final String offerId;
        @JsonProperty("isMyMessage")
        @Schema(description = "Indicates whether this message belongs to the current user.", example = "true")
        private final boolean isMyMessage;
        @Schema(description = "Direction of the offer (buy or sell).", implementation = Direction.class)
        private final Direction direction;
        @Schema(description = "Title of the offer.", example = "Buy 1 BTC at $30,000")
        private final String offerTitle;
        @Schema(description = "Timestamp of the offer in milliseconds since epoch.", example = "1672531200000")
        private final long date;
        @Schema(description = "Formatted date string for the offer.", example = "2023-01-01 12:00:00")
        private final String formattedDate;
        @Schema(description = "Anonymous pseudonym of the user.", example = "Nym123")
        private final String nym;
        @Schema(description = "Username of the offer's creator.", example = "Alice")
        private final String userName;
        @Schema(description = "Reputation score of the user who created the offer.", implementation = ReputationScoreDto.class)
        private final ReputationScoreDto reputationScore;
        @Schema(description = "Formatted amount for the quoted currency.", example = "30,000 USD")
        private final String formattedQuoteAmount;
        @Schema(description = "Formatted price of the offer.", example = "$30,000 per BTC")
        private final String formattedPrice;
        @Schema(description = "List of payment methods supported by the quote side.", example = "[\"Bank Transfer\", \"PayPal\"]")
        private final List<String> quoteSidePaymentMethods;
        @Schema(description = "List of payment methods supported by the base side.", example = "[\"Cash Deposit\"]")
        private final List<String> baseSidePaymentMethods;
        @Schema(description = "Supported language codes for the offer.", example = "en,es,fr")
        private final String supportedLanguageCodes;

        public OfferListItemDto(String messageId,
                                String offerId,
                                boolean isMyMessage,
                                Direction direction,
                                String offerTitle,
                                long date,
                                String formattedDate,
                                String nym,
                                String userName,
                                ReputationScoreDto reputationScore,
                                String formattedQuoteAmount,
                                String formattedPrice,
                                List<String> quoteSidePaymentMethods,
                                List<String> baseSidePaymentMethods,
                                String supportedLanguageCodes) {
            this.messageId = messageId;
            this.offerId = offerId;
            this.isMyMessage = isMyMessage;
            this.direction = direction;
            this.offerTitle = offerTitle;
            this.date = date;
            this.formattedDate = formattedDate;
            this.nym = nym;
            this.userName = userName;
            this.reputationScore = reputationScore;
            this.formattedQuoteAmount = formattedQuoteAmount;
            this.formattedPrice = formattedPrice;
            this.quoteSidePaymentMethods = quoteSidePaymentMethods;
            this.baseSidePaymentMethods = baseSidePaymentMethods;
            this.supportedLanguageCodes = supportedLanguageCodes;
        }
    }

    @Getter
    @Schema(name = "ReputationScoreDto", description = "User reputation details including total score, 5-star rating, and ranking.")
    public static class ReputationScoreDto {
        @Schema(description = "Total reputation score of the user.", example = "1500")
        private final long totalScore;
        @Schema(description = "5-star system equivalent score (out of 5).", example = "4.8")
        private final double fiveSystemScore;
        @Schema(description = "User's ranking among peers.", example = "12")
        private final int ranking;
        public ReputationScoreDto(long totalScore, double fiveSystemScore, int ranking) {
            this.totalScore = totalScore;
            this.fiveSystemScore = fiveSystemScore;
            this.ranking = ranking;
        }
    }

}
