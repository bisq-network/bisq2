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

package bisq.http_api.rest_api.domain.offer;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodUtil;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethodUtil;
import bisq.bisq_easy.BisqEasyServiceUtil;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.currency.Market;
import bisq.dto.DtoMappings;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.support.SupportService;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.UserService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/offers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Offer API")
public class OfferRestApi extends RestApiBase {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final ChatService chatService;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final MediationRequestService mediationRequestService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;

    public OfferRestApi(ChatService chatService,
                        MarketPriceService marketPriceService,
                        UserService userService,
                        SupportService supportedService,
                        TradeService tradeService) {
        this.bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        this.chatService = chatService;
        this.marketPriceService = marketPriceService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        mediationRequestService = supportedService.getMediationRequestService();
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
    }

    @POST
    @Operation(
            summary = "Create and Publish Bisq Easy Offer",
            description = "Creates a Bisq Easy Offer and publish it to the network.",
            requestBody = @RequestBody(
                    description = "",
                    content = @Content(schema = @Schema(implementation = PublishOfferRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "",
                            content = @Content(schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void createOffer(PublishOfferRequest request, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
            Direction direction = DtoMappings.DirectionMapping.toPojo(request.direction());
            Market market = DtoMappings.MarketMapping.toPojo(request.market());
            List<BitcoinPaymentMethod> bitcoinPaymentMethods = request.bitcoinPaymentMethods().stream()
                    .map(BitcoinPaymentMethodUtil::getPaymentMethod)
                    .collect(Collectors.toList());
            List<FiatPaymentMethod> fiatPaymentMethods = request.fiatPaymentMethods().stream()
                    .map(FiatPaymentMethodUtil::getPaymentMethod)
                    .collect(Collectors.toList());
            AmountSpec amountSpec = DtoMappings.AmountSpecMapping.toPojo(request.amountSpec());
            PriceSpec priceSpec = DtoMappings.PriceSpecMapping.toPojo(request.priceSpec());
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
            asyncResponse.resume(buildResponse(Response.Status.CREATED, new PublishOfferResponse(bisqEasyOffer.getId())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error publishing offer", e);
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred."));
        }
    }
}
