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

package bisq.desktop.main.content.authorized_role.mediator.details;

import bisq.bisq_easy.NavigationTarget;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.mediator.MediationCaseListItem;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.MediationCase;
import bisq.support.mediation.MediationRequest;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MediationCaseDetailsController extends NavigationController implements InitWithDataController<MediationCaseDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MediationCaseListItem mediationCaseListItem;

        public InitData(MediationCaseListItem mediationCaseListItem) {
            this.mediationCaseListItem = mediationCaseListItem;
        }
    }

    @Getter
    private final MediationCaseDetailsModel model;
    @Getter
    private final MediationCaseDetailsView view;


    public MediationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MEDIATION_CASE_DETAILS);

        model = new MediationCaseDetailsModel();
        view = new MediationCaseDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMediationCaseListItem(initData.mediationCaseListItem);
    }

    @Override
    public void onActivate() {
        MediationCaseListItem mediationCaseListItem = model.getMediationCaseListItem();
        BisqEasyOpenTradeChannel channel = mediationCaseListItem.getChannel();
        MediationCase mediationCase = mediationCaseListItem.getMediationCase();
        MediationRequest mediationRequest = mediationCase.getMediationRequest();
        BisqEasyContract contract = mediationRequest.getContract();
        BisqEasyOffer offer = contract.getOffer();
        String tradeId = mediationRequest.getTradeId();

        model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));

        model.setOfferType(offer.getDirection().isBuy()
                ? Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.buyOffer")
                : Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.sellOffer"));
        model.setMarket(Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.fiatMarket",
                offer.getMarket().getQuoteCurrencyCode()));
        model.setFiatAmount(BisqEasyTradeFormatter.formatQuoteSideAmount(contract));
        model.setFiatCurrency(offer.getMarket().getQuoteCurrencyCode());
        model.setBtcAmount(BisqEasyTradeFormatter.formatBaseSideAmount(contract));
        model.setPrice(PriceFormatter.format(BisqEasyTradeUtils.getPriceQuote(contract)));
        model.setPriceCodes(offer.getMarket().getMarketCodes());
        model.setPriceSpec(offer.getPriceSpec() instanceof FixPriceSpec
                ? ""
                : String.format("(%s)", PriceSpecFormatter.getFormattedPriceSpec(offer.getPriceSpec(), true)));
        model.setPaymentMethod(contract.getQuoteSidePaymentMethodSpec().getShortDisplayString());
        model.setSettlementMethod(contract.getBaseSidePaymentMethodSpec().getShortDisplayString());
        model.setTradeId(tradeId);

        MediationCaseListItem.Trader maker = mediationCaseListItem.getMaker();
        MediationCaseListItem.Trader taker = mediationCaseListItem.getTaker();
        MediationCaseListItem.Trader buyer = offer.getDirection().isBuy() ? maker : taker;
        MediationCaseListItem.Trader seller = offer.getDirection().isSell() ? maker : taker;
        model.setBuyerUserName(buyer.getUserName());
        model.setSellerUserName(seller.getUserName());
        model.setBuyerNetworkAddress(buyer.getUserProfile().getAddressByTransportDisplayString(50));
        model.setSellerNetworkAddress(seller.getUserProfile().getAddressByTransportDisplayString(50));
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return Optional.empty();
    }

    void onClose() {
        OverlayController.hide();
    }
}
