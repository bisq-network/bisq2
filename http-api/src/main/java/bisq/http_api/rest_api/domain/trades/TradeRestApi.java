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
import bisq.account.payment_method.BitcoinPaymentRail;
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
import bisq.common.monetary.Fiat;
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
import bisq.presentation.formatters.AmountFormatter;
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

import static com.google.common.base.Preconditions.*;

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
            Optional<UserProfile> mediator = mediationRequestService.selectMediator(bisqEasyOffer.getMakersUserProfileId(),
                    takerIdentity.getId(),
                    bisqEasyOffer.getId());
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

    @PATCH
    @Path("/{tradeId}/event")
    @Operation(
            summary = "Process a trade event for a specific trade ID",
            description = "This endpoint allows processing various trade events such as rejecting, canceling, " +
                    "closing a trade, or handling payment details from buyers and sellers. " +
                    "It interacts with the open trade channel and manages state transitions based on the event type.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Trade event processed successfully"),
                    @ApiResponse(responseCode = "404", description = "Trade or trade channel not found"),
                    @ApiResponse(responseCode = "400", description = "Invalid trade event data"),
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
            String userName = channel.getMyUserIdentity().getUserName();
            BitcoinPaymentRail paymentRail = trade.getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            switch (tradeEvent.tradeEventType()) {
                case REJECT_TRADE -> handleRejectTrade(channel, trade, userName);
                case CANCEL_TRADE -> handleCancelTrade(channel, trade, userName);
                case CLOSE_TRADE -> handleCloseTrade(channel, trade);
                case SELLER_SENDS_PAYMENT_ACCOUNT -> handleSellerSendsPaymentAccount(channel, trade, tradeEvent, userName);
                case BUYER_SEND_BITCOIN_PAYMENT_DATA -> handleBuyerSendBitcoinPaymentData(channel, trade, tradeEvent, paymentRail, userName);
                case SELLER_CONFIRM_FIAT_RECEIPT -> handleSellerConfirmFiatReceipt(channel, trade, userName);
                case BUYER_CONFIRM_FIAT_SENT -> handleBuyerConfirmFiatSent(channel, trade, userName);
                case SELLER_CONFIRM_BTC_SENT -> handleSellerConfirmBtcSent(channel, trade, tradeEvent, paymentRail, userName);
                case BTC_CONFIRMED -> handleBtcConfirmed(channel, trade, paymentRail, userName);
            }
            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    private void handleRejectTrade(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, String userName) throws Exception {
        String encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).get();
        bisqEasyTradeService.rejectTrade(trade);
    }

    private void handleCancelTrade(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, String userName) throws Exception {
        String encoded = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).get();
        bisqEasyTradeService.cancelTrade(trade);
        leavePrivateChatManager.leaveChannel(channel);
    }

    private void handleCloseTrade(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade) throws Exception {
        bisqEasyTradeService.removeTrade(trade);
        leavePrivateChatManager.leaveChannel(channel);
    }

    private void handleSellerSendsPaymentAccount(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, TradeEventDto tradeEvent, String userName) throws Exception {
        String paymentAccountData = checkNotNull(tradeEvent.data(), "paymentAccountData must not be null");
        String encoded = Res.encode("bisqEasy.tradeState.info.seller.phase1.tradeLogMessage", userName, paymentAccountData);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        bisqEasyTradeService.sellerSendsPaymentAccount(trade, paymentAccountData);
    }

    private void handleBuyerSendBitcoinPaymentData(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, TradeEventDto tradeEvent, BitcoinPaymentRail paymentRail, String userName) throws Exception {
        String btcRailName = paymentRail.name();
        String key = "bisqEasy.tradeState.info.buyer.phase1a.tradeLogMessage." + btcRailName;
        String bitcoinPaymentData = checkNotNull(tradeEvent.data(), "bitcoinPaymentData must not be null");
        String encoded = Res.encode(key, userName, bitcoinPaymentData);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        bisqEasyTradeService.buyerSendBitcoinPaymentData(trade, bitcoinPaymentData);
    }

    private void handleSellerConfirmFiatReceipt(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, String userName) throws Exception {
        long quoteSideAmount = trade.getContract().getQuoteSideAmount();
        String quoteCurrencyCode = trade.getOffer().getMarket().getQuoteCurrencyCode();
        String formattedQuoteAmount = AmountFormatter.formatQuoteAmountWithCode(Fiat.from(quoteSideAmount, quoteCurrencyCode));
        String encoded = Res.encode("bisqEasy.tradeState.info.seller.phase2b.tradeLogMessage", userName, formattedQuoteAmount);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        bisqEasyTradeService.sellerConfirmFiatReceipt(trade);
    }

    private void handleBuyerConfirmFiatSent(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, String userName) throws Exception {
        String quoteCurrencyCode = trade.getOffer().getMarket().getQuoteCurrencyCode();
        String encoded = Res.encode("bisqEasy.tradeState.info.buyer.phase2a.tradeLogMessage", userName, quoteCurrencyCode);
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        bisqEasyTradeService.buyerConfirmFiatSent(trade);
    }

    private void handleSellerConfirmBtcSent(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, TradeEventDto tradeEvent, BitcoinPaymentRail paymentRail, String userName) throws Exception {
        Optional<String> paymentProof = Optional.ofNullable(tradeEvent.data());
        boolean isMainChain = paymentRail == BitcoinPaymentRail.MAIN_CHAIN;
        if (isMainChain) {
            checkArgument(paymentProof.isPresent(), "Transaction ID is required for Bitcoin settlement");
        }
        String encoded;
        if (paymentProof.isEmpty()) {
            encoded = Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.noProofProvided", userName);
        } else {
            String btcRailName = paymentRail.name();
            String proofType = Res.get("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.paymentProof." + btcRailName);
            encoded = Res.encode("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage", userName, proofType, paymentProof.get());
        }
        bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        bisqEasyTradeService.sellerConfirmBtcSent(trade, paymentProof);
    }

    private void handleBtcConfirmed(BisqEasyOpenTradeChannel channel, BisqEasyTrade trade, BitcoinPaymentRail paymentRail, String userName) throws Exception {
        if (paymentRail == BitcoinPaymentRail.LN && trade.isBuyer()) {
            String encoded = Res.encode("bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln", userName);
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel);
        }
        bisqEasyTradeService.btcConfirmed(trade);
    }

}
