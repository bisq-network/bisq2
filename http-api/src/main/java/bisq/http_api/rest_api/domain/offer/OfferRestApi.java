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
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.dto.DtoMappings;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.support.SupportService;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol;
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
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

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

    // todo use AsyncResponse
    @POST
    @Operation(
            summary = "Take Bisq Easy Offer",
            description = "Takes a Bisq Easy Offer.",
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
    @Path("{offerId}/take")
    public Response takeOffer(@PathParam("offerId") String offerId, TakeOfferRequest request) {
        try {
            UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
            checkArgument(!bannedUserService.isUserProfileBanned(takerIdentity.getUserProfile()), "Taker profile is banned");
            //noinspection OptionalGetWithoutIsPresent
            BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookChannelService.getChannels().stream().flatMap(c -> c.getChatMessages().stream())
                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                    .map(e -> e.getBisqEasyOffer().get())
                    .filter(e -> e.getId().equals(offerId))
                    .findFirst()
                    .orElseThrow();
            checkArgument(!bannedUserService.isNetworkIdBanned(bisqEasyOffer.getMakerNetworkId()), "Maker profile is banned");
            Monetary baseSideAmount = Monetary.from(request.baseSideAmount(), bisqEasyOffer.getMarket().getBaseCurrencyCode());
            Monetary quoteSideAmount = Monetary.from(request.quoteSideAmount(), bisqEasyOffer.getMarket().getBaseCurrencyCode());
            BitcoinPaymentMethod bitcoinPaymentMethod = PaymentMethodSpecUtil.getBitcoinPaymentMethod(request.bitcoinPaymentMethod());
            BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec = new BitcoinPaymentMethodSpec(bitcoinPaymentMethod);
            FiatPaymentMethod fiatPaymentMethod = PaymentMethodSpecUtil.getFiatPaymentMethod(request.fiatPaymentMethod());
            FiatPaymentMethodSpec fiatPaymentMethodSpec = new FiatPaymentMethodSpec(fiatPaymentMethod);
            Optional<UserProfile> mediator = mediationRequestService.selectMediator(bisqEasyOffer.getMakersUserProfileId(), takerIdentity.getId());
            PriceSpec makersPriceSpec = bisqEasyOffer.getPriceSpec();
            long marketPrice = marketPriceService.findMarketPrice(bisqEasyOffer.getMarket())
                    .map(e -> e.getPriceQuote().getValue())
                    .orElseThrow();
            BisqEasyProtocol bisqEasyProtocol = bisqEasyTradeService.createBisqEasyProtocol(takerIdentity.getIdentity(),
                    bisqEasyOffer,
                    baseSideAmount,
                    quoteSideAmount,
                    bitcoinPaymentMethodSpec,
                    fiatPaymentMethodSpec,
                    mediator,
                    makersPriceSpec,
                    marketPrice);
            BisqEasyTrade bisqEasyTrade = bisqEasyProtocol.getModel();
            log.info("Selected mediator for trade {}: {}", bisqEasyTrade.getShortId(), mediator.map(UserProfile::getUserName).orElse("N/A"));

            bisqEasyTradeService.takeOffer(bisqEasyTrade);
            BisqEasyContract contract = bisqEasyTrade.getContract();

            String tradeId = bisqEasyTrade.getId();
            // todo set timeout
            // asyncResponse.setTimeout(150, TimeUnit.SECONDS); // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message
            bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.getMediator())
                    .thenAccept(result ->
                            {
                                // In case the user has switched to another market we want to select that market in the offer book
                                ChatChannelSelectionService chatChannelSelectionService =
                                        chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
                                bisqEasyOfferbookChannelService.findChannel(contract.getOffer().getMarket())
                                        .ifPresent(chatChannelSelectionService::selectChannel);
                                bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                                        .ifPresent(channel -> {
                                                    String taker = userIdentityService.getSelectedUserIdentity().getUserProfile().getUserName();
                                                    String maker = channel.getPeer().getUserName();
                                                    String encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker);
                                                    chatService.getBisqEasyOpenTradeChannelService().sendTradeLogMessage(encoded, channel);
                                                });
                            }
                    )
                    .get();

            String errorMessage = bisqEasyTrade.errorMessageObservable().get();
            checkArgument(errorMessage == null, "An error occurred at taking the offer: " + errorMessage +
                    ". ErrorStackTrace: " + StringUtils.truncate(bisqEasyTrade.getErrorStackTrace(), 500));

            String peersErrorMessage = bisqEasyTrade.peersErrorMessageObservable().get();
            checkArgument(peersErrorMessage == null, "An error occurred at the peers side at taking the offer: " + peersErrorMessage +
                    ". ErrorStackTrace: " + StringUtils.truncate(bisqEasyTrade.getPeersErrorStackTrace(), 500));

            return buildResponse(Response.Status.OK, new TakeOfferResponse(bisqEasyTrade.getId()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildErrorResponse("Thread was interrupted.");
        } catch (IllegalArgumentException e) {
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error publishing offer", e);
            return buildErrorResponse("An unexpected error occurred.");
        }
    }

    // todo use AsyncResponse
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
    public Response createOffer(PublishOfferRequest request) {
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
            return buildResponse(Response.Status.CREATED, new PublishOfferResponse(bisqEasyOffer.getId()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildErrorResponse("Thread was interrupted.");
        } catch (IllegalArgumentException e) {
            return buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error publishing offer", e);
            return buildErrorResponse("An unexpected error occurred.");
        }
    }
}
