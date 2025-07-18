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

package bisq.trade.bisq_easy.protocol.messages;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.market.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.Offer;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.trade.bisq_easy.handler.BisqEasyTradeMessageHandlerAsMessageSender;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyTakeOfferRequestHandler extends BisqEasyTradeMessageHandlerAsMessageSender<BisqEasyTrade, BisqEasyTakeOfferRequest> {
    private ContractSignatureData takersContractSignatureData;
    private ContractSignatureData makersContractSignatureData;

    public BisqEasyTakeOfferRequestHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void verify(BisqEasyTakeOfferRequest message) {
        BisqEasyContract takersContract = checkNotNull(message.getBisqEasyContract(), "Takers contract must not be null");
        BisqEasyOffer takersOffer = checkNotNull(takersContract.getOffer(), "Offer from takers contract must not be null");

        List<BisqEasyOffer> myOffers = serviceProvider.getChatService().getBisqEasyOfferbookChannelService().getChannels().stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                .map(chatMessage -> chatMessage.getBisqEasyOffer().get())
                .filter(offer -> offer.getMakerNetworkId().equals(trade.getMyIdentity().getNetworkId()))
                .collect(Collectors.toList());
        Optional<BisqEasyOffer> matchingOfferInChannel = myOffers.stream()
                .filter(offer -> offer.equals(takersOffer))
                .findAny();
        if (matchingOfferInChannel.isEmpty()) {
            // If we do not find the offer it might be that another user has taken the offer recently and the offer has
            // been removed in the meantime. For this case we check if we find the offer in another open trade. If not
            // found we throw an exception.
            // We also check if there is not a trade already present with the same trade ID to avoid that we would
            // create a duplicated trade. We check for both the version 0 and version 1 trade ID to cover all edge cases.
            BisqEasyTradeService bisqEasyTradeService = serviceProvider.getBisqEasyTradeService();
            boolean hasOfferInTrades = bisqEasyTradeService.getTrades().stream().anyMatch(trade ->
                    trade.getOffer().getId().equals(takersOffer.getId()));
            boolean closeMyOfferWhenTaken = serviceProvider.getSettingsService().getCloseMyOfferWhenTaken().get();
            if (hasOfferInTrades) {
                log.info("The offer has not been found in open offers, but we found another trade with the same offer.\n" +
                                "We accept the take offer request as it might be from processing mailbox messages " +
                                "where multiple takers took the same offer.\n" +
                                "closeMyOfferWhenTaken={}; takersOffer={}",
                        closeMyOfferWhenTaken, takersOffer);
            } else {
                String errorMessage = String.format("Could not find matching offer in BisqEasyOfferbookChannel and no " +
                        "trade with that offer was found.\n" +
                        "closeMyOfferWhenTaken=%s; takersOffer=%s", closeMyOfferWhenTaken, takersOffer);
                log.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        }

        checkArgument(message.getSender().equals(takersContract.getTaker().getNetworkId()),
                "Senders networkId must be same as takers networkId from takers contract");

        validateAmount(takersOffer, takersContract);

        checkArgument(takersOffer.getBaseSidePaymentMethodSpecs().contains(takersContract.getBaseSidePaymentMethodSpec()),
                "BaseSidePaymentMethodSpec from takers contract must be present in the offers baseSidePaymentMethodSpecs");
        checkArgument(takersOffer.getQuoteSidePaymentMethodSpecs().contains(takersContract.getQuoteSidePaymentMethodSpec()),
                "QuoteSidePaymentMethodSpec from takers contract must be present in the offers quoteSidePaymentMethodSpecs");

        Optional<UserProfile> mediator = serviceProvider.getSupportService().getMediationRequestService()
                .selectMediator(takersOffer.getMakersUserProfileId(),
                        trade.getTaker().getNetworkId().getId(),
                        trade.getOffer().getId());
        checkArgument(mediator.map(UserProfile::getNetworkId).equals(takersContract.getMediator().map(UserProfile::getNetworkId)),
                "Mediators do not match. " +
                        "\nmediator=" + mediator +
                        "\ntakersContract.getMediator()=" + takersContract.getMediator());

        log.info("Selected mediator for trade {}: {}", trade.getShortId(), mediator.map(UserProfile::getUserName).orElse("N/A"));

        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        try {
            checkArgument(serviceProvider.getContractService().verifyContractSignature(takersContract, takersContractSignatureData),
                    "Verifying takers contract signature failed");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void process(BisqEasyTakeOfferRequest message) {
        BisqEasyContract contract = message.getBisqEasyContract();
//        checkArgument(trade.getOffer().getPriceSpec().equals(contract.getAgreedPriceSpec()),
//                "Price spec cannot be changed from the one set in offer since v2.0.3.");
        takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            makersContractSignatureData = contractService.signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());

            if (serviceProvider.getSettingsService().getCloseMyOfferWhenTaken().get()) {
                BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
                bisqEasyOfferbookChannelService.findMessageByOffer(trade.getOffer())
                        .ifPresent(chatMessage -> bisqEasyOfferbookChannelService.deleteChatMessage(chatMessage, trade.getMyIdentity().getNetworkIdWithKeyPair())
                                .whenComplete((deleteChatMessageResult, throwable) -> {
                                    if (throwable == null) {
                                        log.info("Offer with ID {} removed", chatMessage.getBisqEasyOffer().map(Offer::getId).orElse("N/A"));
                                    } else {
                                        log.error("We got an error at doDeleteMessage", throwable);
                                    }
                                }));
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void commit() {
        trade.getTaker().getContractSignatureData().set(takersContractSignatureData);
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }

    @Override
    protected void sendMessage() {
        send(new BisqEasyTakeOfferResponse(StringUtils.createUid(),
                trade.getId(),
                trade.getProtocolVersion(),
                trade.getMyself().getNetworkId(),
                trade.getPeer().getNetworkId(),
                makersContractSignatureData));
    }

    private void validateAmount(BisqEasyOffer takersOffer, BisqEasyContract takersContract) {
        MarketPriceService marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        Market market = takersOffer.getMarket();
        MarketPrice marketPrice = marketPriceService.getMarketPriceByCurrencyMap().get(market);
        Optional<PriceQuote> priceQuote = PriceUtil.findQuote(marketPriceService,
                takersContract.getPriceSpec(), market);
        checkArgument(priceQuote.isPresent(),
                "PriceQuote is empty. Might be that no market price is available. marketPrice=" + marketPrice);
        Optional<Monetary> amount = priceQuote.map(quote -> quote.toBaseSideMonetary(Monetary.from(takersContract.getQuoteSideAmount(),
                market.getQuoteCurrencyCode())));

        long takersAmount = takersContract.getBaseSideAmount();
        long myAmount = amount.get().getValue(); // I am maker

        double maxTradePriceDeviation = serviceProvider.getSettingsService().getMaxTradePriceDeviation().get();
        double warnDeviation = maxTradePriceDeviation / 2;
        double warnThreshold, errorThreshold;
        boolean showWaring = false;
        boolean throwException = false;
        String message = "";

        if (trade.isBuyer()) {
            // If I am buyer I accept if takers amount is larger than my expected amount (good for me as I receive more BTC).
            // If takers amount is below my maxTradePriceDeviation the trade fails.
            warnThreshold = myAmount * (1 - warnDeviation);
            errorThreshold = myAmount * (1 - maxTradePriceDeviation);
            if (takersAmount < errorThreshold) {
                throwException = true;
                message = "Takers (sellers) Bitcoin amount is too low. " +
                        "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                        "to manipulate the price.\n";
            } else if (takersAmount < warnThreshold) {
                showWaring = true;
                message = "Takers (sellers) Bitcoin amount is lower as expected. " +
                        "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                        "to manipulate the price. We still tolerate that deviation.\n";
            }
        } else {
            // If I am seller I accept if takers amount is smaller than my expected amount (good for me as I need to send less BTC).
            // If takers amount is above my maxTradePriceDeviation the trade fails.
            warnThreshold = myAmount * (1 + warnDeviation);
            errorThreshold = myAmount * (1 + maxTradePriceDeviation);
            if (takersAmount > errorThreshold) {
                throwException = true;
                message = "Takers (buyers) Bitcoin amount is too high. " +
                        "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                        "to manipulate the price.\n";
            } else if (takersAmount > warnThreshold) {
                showWaring = true;
                message = "Takers (sellers) Bitcoin amount is lower as expected. " +
                        "This can be caused by differences in the 2 traders market price or by an attempt by the taker " +
                        "to manipulate the price. We still tolerate that deviation.\n";
            }
        }

        String details = "takersAmount=" + takersAmount + "\n" +
                "myAmount=" + myAmount + "\n" +
                "errorThreshold=" + errorThreshold + "\n" +
                "marketPrice=" + marketPrice.getPriceQuote().getValue() + "\n" +
                "priceQuote=" + priceQuote.map(PriceQuote::getValue).orElse(0L) + "\n" +
                "takersContract=" + takersContract;
        if (throwException) {
            log.error("message={}, details={}", message, details);
            throw new IllegalArgumentException(message);
        } else if (showWaring) {
            log.warn("message={}, details={}", message, details);
        } else if (myAmount != takersAmount) {
            log.info("My amount and the amount set by the taker are not the same. This is expected if the offer used a " +
                    "market based price and the taker had a different market price.\n" +
                    "{}", details);
        }
    }
}
