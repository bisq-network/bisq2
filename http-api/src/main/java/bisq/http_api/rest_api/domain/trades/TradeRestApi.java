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

package bisq.http_api.rest_api.domain.trades;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.http_api.rest_api.domain.offers.CreateOfferRequest;
import bisq.i18n.Res;
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
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Path("/trades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Bisq Easy Trade API")
public class TradeRestApi extends RestApiBase {
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BannedUserService bannedUserService;
    private final MediationRequestService mediationRequestService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final ChatChannelSelectionService openTradesChannelSelectionService;
    private final LeavePrivateChatManager leavePrivateChatManager;
    private final ChatChannelSelectionService offerbookChannelSelectionService;

    public TradeRestApi(ChatService chatService,
                        MarketPriceService marketPriceService,
                        UserService userService,
                        SupportService supportedService,
                        TradeService tradeService) {
        this.bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        offerbookChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        openTradesChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OPEN_TRADES);
        leavePrivateChatManager = chatService.getLeavePrivateChatManager();
        this.marketPriceService = marketPriceService;
        userIdentityService = userService.getUserIdentityService();
        bannedUserService = userService.getBannedUserService();
        mediationRequestService = supportedService.getMediationRequestService();
        bisqEasyTradeService = tradeService.getBisqEasyTradeService();
    }

    @POST
    @Operation(
            summary = "Create a Trade by Taking a Bisq Easy Offer",
            description = "Create a Trade by Taking a Bisq Easy Offer.",
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
    public void takeOffer(TakeOfferRequest request, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(150, TimeUnit.SECONDS); // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });

        try {
            UserIdentity takerIdentity = userIdentityService.getSelectedUserIdentity();
            checkArgument(!bannedUserService.isUserProfileBanned(takerIdentity.getUserProfile()), "Taker profile is banned");
            //noinspection OptionalGetWithoutIsPresent
            BisqEasyOffer bisqEasyOffer = bisqEasyOfferbookChannelService.getChannels().stream().flatMap(c -> c.getChatMessages().stream())
                    .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                    .map(e -> e.getBisqEasyOffer().get())
                    .filter(e -> e.getId().equals(request.offerId()))
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
            bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.getMediator())
                    .thenAccept(result ->
                            {
                                // In case the user has switched to another market we want to select that market in the offer book
                                bisqEasyOfferbookChannelService.findChannel(contract.getOffer().getMarket())
                                        .ifPresent(offerbookChannelSelectionService::selectChannel);
                                bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                                        .ifPresent(channel -> {
                                            String taker = userIdentityService.getSelectedUserIdentity().getUserProfile().getUserName();
                                            String maker = channel.getPeer().getUserName();
                                            String encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker);
                                            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
                                        });
                            }
                    )
                    .get();

            // After the take offer is completed we check if errors happened
            String errorMessage = bisqEasyTrade.errorMessageObservable().get();
            checkArgument(errorMessage == null, "An error occurred at taking the offer: " + errorMessage +
                    ". ErrorStackTrace: " + StringUtils.truncate(bisqEasyTrade.getErrorStackTrace(), 500));

            String peersErrorMessage = bisqEasyTrade.peersErrorMessageObservable().get();
            checkArgument(peersErrorMessage == null, "An error occurred at the peers side at taking the offer: " + peersErrorMessage +
                    ". ErrorStackTrace: " + StringUtils.truncate(bisqEasyTrade.getPeersErrorStackTrace(), 500));

            asyncResponse.resume(buildResponse(Response.Status.CREATED, new TakeOfferResponse(bisqEasyTrade.getId())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            asyncResponse.resume(buildErrorResponse("Thread was interrupted."));
        } catch (IllegalArgumentException e) {
            asyncResponse.resume(buildResponse(Response.Status.BAD_REQUEST, "Invalid input: " + e.getMessage()));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PUT
    @Path("/selected/{tradeId}")
    @Operation(
            summary = "Select an open trade",
            description = "Selects the open trade channel for that trade ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trade selected successfully"),
                    @ApiResponse(responseCode = "404", description = "Trade not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void selectOpenTrade(@PathParam("tradeId") String tradeId, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Optional<BisqEasyOpenTradeChannel> optionalChannel = bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId);
            if (optionalChannel.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "Open trade channel not found for trade with ID " + tradeId));
                return;
            }

            openTradesChannelSelectionService.selectChannel(optionalChannel.get());
            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }


    @PATCH
    @Path("/{tradeId}/event")
    @Operation(
            summary = "Cancel a Trade",
            description = "Send a log message to the peer and set the state of a trade to 'canceled'.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Trade successfully canceled"),
                    @ApiResponse(responseCode = "404", description = "Trade not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void processTradeEvent(@PathParam("tradeId") String tradeId,
                         TradeEventDto tradeEvent,
                         @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });

        try {
            Optional<BisqEasyOpenTradeChannel> optionalChannel = bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId);
            if (optionalChannel.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "Open trade channel not found for trade with ID " + tradeId));
                return;
            }

            Optional<BisqEasyTrade> optionalTrade = bisqEasyTradeService.findTrade(tradeId);
            if (optionalTrade.isEmpty()) {
                asyncResponse.resume(buildResponse(Response.Status.NOT_FOUND, "Trade not found for ID " + tradeId));
                return;
            }

            BisqEasyOpenTradeChannel channel = optionalChannel.get();
            BisqEasyTrade trade = optionalTrade.get();
            switch (tradeEvent) {
                case REJECTED -> {
                    String userName = channel.getMyUserIdentity().getUserName();
                    String encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName);
                    bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).get();
                    bisqEasyTradeService.rejectTrade(trade);
                }
                case CANCELLED -> {
                    String userName = channel.getMyUserIdentity().getUserName();
                    String encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName);
                    bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).get();
                    bisqEasyTradeService.cancelTrade(trade);
                    leavePrivateChatManager.leaveChannel(channel);
                }
                case CLOSED -> {
                    bisqEasyTradeService.removeTrade(trade);
                    leavePrivateChatManager.leaveChannel(channel);
                }
            }

            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }
}
