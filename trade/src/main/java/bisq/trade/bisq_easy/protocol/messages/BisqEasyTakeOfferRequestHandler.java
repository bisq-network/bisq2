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

import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.fsm.Event;
import bisq.common.monetary.Monetary;
import bisq.common.util.StringUtils;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.Offer;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.PriceUtil;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class BisqEasyTakeOfferRequestHandler extends TradeMessageHandler<BisqEasyTrade, BisqEasyTakeOfferRequest> implements TradeMessageSender<BisqEasyTrade> {

    public BisqEasyTakeOfferRequestHandler(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        BisqEasyTakeOfferRequest message = (BisqEasyTakeOfferRequest) event;
        verifyMessage(message);

        BisqEasyContract contract = message.getBisqEasyContract();
        checkArgument(trade.getOffer().getPriceSpec().equals(contract.getAgreedPriceSpec()),
                "Price spec cannot be changed from the one set in offer since v2.0.3. Update software to take this offer.");
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            ContractSignatureData makersContractSignatureData = contractService.signContract(contract,
                    trade.getMyIdentity().getKeyBundle().getKeyPair());
            commitToModel(takersContractSignatureData, makersContractSignatureData);

            BisqEasyTakeOfferResponse response = new BisqEasyTakeOfferResponse(StringUtils.createUid(),
                    trade.getId(),
                    trade.getProtocolVersion(),
                    trade.getMyself().getNetworkId(),
                    trade.getPeer().getNetworkId(),
                    makersContractSignatureData);
            sendMessage(response, serviceProvider, trade);

            if (serviceProvider.getSettingsService().getCloseMyOfferWhenTaken().get()) {
                BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
                bisqEasyOfferbookChannelService.findMessageByOffer(trade.getOffer())
                        .ifPresent(chatMessage -> bisqEasyOfferbookChannelService.deleteChatMessage(chatMessage, trade.getMyIdentity().getNetworkIdWithKeyPair())
                                .whenComplete((deleteChatMessageResult, throwable) -> {
                                    if (throwable == null) {
                                        log.info("Offer with ID {} removed", chatMessage.getBisqEasyOffer().map(Offer::getId).orElse("N/A"));
                                    } else {
                                        log.error("We got an error at doDeleteMessage: " + throwable);
                                    }
                                }));
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferRequest message) {
        super.verifyMessage(message);

        BisqEasyContract takersContract = checkNotNull(message.getBisqEasyContract());
        BisqEasyOffer takersOffer = checkNotNull(takersContract.getOffer());

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
            log.error("Could not find matching offer in BisqEasyOfferbookChannel.\n" +
                            "takersOffer={}\n" +
                            "myOffers={}",
                    takersOffer, myOffers);
            throw new RuntimeException("Could not find matching offer in BisqEasyOfferbookChannel");
        }

        checkArgument(message.getSender().equals(takersContract.getTaker().getNetworkId()));

        validateAmount(takersOffer, takersContract);

        checkArgument(takersOffer.getBaseSidePaymentMethodSpecs().contains(takersContract.getBaseSidePaymentMethodSpec()));
        checkArgument(takersOffer.getQuoteSidePaymentMethodSpecs().contains(takersContract.getQuoteSidePaymentMethodSpec()));

        Optional<UserProfile> mediator = serviceProvider.getSupportService().getMediationRequestService()
                .selectMediator(takersOffer.getMakersUserProfileId(), trade.getTaker().getNetworkId().getId());
        checkArgument(mediator.equals(takersContract.getMediator()), "Mediators do not match. " +
                        "mediator={}, takersContract.getMediator()={}",
                mediator, takersContract.getMediator());

        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        try {
            checkArgument(serviceProvider.getContractService().verifyContractSignature(takersContract, takersContractSignatureData));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData, ContractSignatureData makersContractSignatureData) {
        trade.getTaker().getContractSignatureData().set(takersContractSignatureData);
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }

    private void validateAmount(BisqEasyOffer takersOffer, BisqEasyContract takersContract) {
        Optional<Monetary> amount = getAmount(takersOffer, takersContract);
        checkArgument(amount.isPresent(), "No market price available for validation.");

        double tolerancePercentage = 0.03;
        long tolerance = (long) (amount.get().getValue() * tolerancePercentage);
        long minAmountWithTolerance = amount.get().getValue() - tolerance;
        long maxAmountWithTolerance = amount.get().getValue() + tolerance;

        long takersAmount = takersContract.getBaseSideAmount();
        String errorMsg = "Market price deviation is too big.";
        checkArgument(takersAmount >= minAmountWithTolerance, errorMsg);
        checkArgument(takersAmount <= maxAmountWithTolerance, errorMsg);
    }

    private Optional<Monetary> getAmount(BisqEasyOffer takersOffer, BisqEasyContract takersContract) {
        return PriceUtil.findQuote(serviceProvider.getBondedRolesService().getMarketPriceService(),
                        takersContract.getAgreedPriceSpec(), takersOffer.getMarket())
                .map(quote -> quote.toBaseSideMonetary(Monetary.from(takersContract.getQuoteSideAmount(),
                        takersOffer.getMarket().getQuoteCurrencyCode())));
    }
}
