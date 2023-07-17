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

import bisq.common.fsm.Event;
import bisq.common.monetary.Monetary;
import bisq.contract.ContractService;
import bisq.contract.ContractSignatureData;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.protocol.events.TradeMessageHandler;
import bisq.trade.protocol.events.TradeMessageSender;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.Optional;

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
        ContractSignatureData takersContractSignatureData = message.getContractSignatureData();
        ContractService contractService = serviceProvider.getContractService();
        try {
            checkArgument(contractService.verifyContractSignature(contract, takersContractSignatureData));

            ContractSignatureData makersContractSignatureData = contractService.signContract(contract, trade.getMyIdentity().getKeyPair());
            commitToModel(takersContractSignatureData, makersContractSignatureData);

            BisqEasyTakeOfferResponse response = new BisqEasyTakeOfferResponse(trade.getId(), trade.getMyself().getNetworkId(), makersContractSignatureData);
            sendMessage(response, serviceProvider, trade);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void verifyMessage(BisqEasyTakeOfferRequest message) {
        super.verifyMessage(message);

        BisqEasyContract takersContract = checkNotNull(message.getBisqEasyContract());
        BisqEasyOffer takersOffer = checkNotNull(takersContract.getOffer());
        serviceProvider.getChatService().getBisqEasyPublicChatChannelService().getChannels().stream()
                .flatMap(channel -> channel.getChatMessages().stream())
                .filter(chatMessage -> chatMessage.getBisqEasyOffer().isPresent())
                .map(chatMessage -> chatMessage.getBisqEasyOffer().get())
                .filter(offer -> offer.getMakerNetworkId().equals(trade.getMyIdentity().getNetworkId()))
                .filter(offer -> offer.equals(takersOffer))
                .findAny()
                .orElseThrow();

        checkArgument(message.getSender().equals(takersContract.getTaker().getNetworkId()));

        Monetary baseSideMinAmount = OfferAmountUtil.findBaseSideMinOrFixedAmount(serviceProvider.getBondedRolesService().getMarketPriceService(), takersOffer).orElseThrow();
        Monetary baseSideMaxAmount = OfferAmountUtil.findBaseSideMaxOrFixedAmount(serviceProvider.getBondedRolesService().getMarketPriceService(), takersOffer).orElseThrow();

        //todo add tolerance as market price might be a bit off
        // checkArgument(takersContract.getBaseSideAmount() >= baseSideMinAmount.getValue());
        //  checkArgument(takersContract.getBaseSideAmount() <= baseSideMaxAmount.getValue());

        Monetary quoteSideMinAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(serviceProvider.getBondedRolesService().getMarketPriceService(), takersOffer).orElseThrow();
        Monetary quoteSideMaxAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(serviceProvider.getBondedRolesService().getMarketPriceService(), takersOffer).orElseThrow();
        //  checkArgument(takersContract.getQuoteSideAmount() >= quoteSideMinAmount.getValue());
        //   checkArgument(takersContract.getQuoteSideAmount() <= quoteSideMaxAmount.getValue());

        checkArgument(takersOffer.getBaseSidePaymentMethodSpecs().contains(takersContract.getBaseSidePaymentMethodSpec()));
        checkArgument(takersOffer.getQuoteSidePaymentMethodSpecs().contains(takersContract.getQuoteSidePaymentMethodSpec()));

        Optional<UserProfile> mediator = serviceProvider.getSupportService().getMediationService().selectMediator(takersOffer.getMakersUserProfileId(), trade.getTaker().getNetworkId().getId());
        checkArgument(mediator.equals(takersContract.getMediator()), "Mediators do not match");
    }

    private void commitToModel(ContractSignatureData takersContractSignatureData, ContractSignatureData makersContractSignatureData) {
        trade.getTaker().getContractSignatureData().set(takersContractSignatureData);
        trade.getMaker().getContractSignatureData().set(makersContractSignatureData);
    }
}