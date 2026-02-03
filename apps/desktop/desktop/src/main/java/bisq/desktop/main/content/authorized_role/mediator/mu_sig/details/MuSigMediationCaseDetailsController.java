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

package bisq.desktop.main.content.authorized_role.mediator.mu_sig.details;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.mediator.mu_sig.MuSigMediationCaseListItem;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.mu_sig.MuSigMediationCase;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigMediationCaseDetailsController extends NavigationController implements InitWithDataController<MuSigMediationCaseDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigMediationCaseListItem muSigMediationCaseListItem;

        public InitData(MuSigMediationCaseListItem muSigMediationCaseListItem) {
            this.muSigMediationCaseListItem = muSigMediationCaseListItem;
        }
    }

    @Getter
    private final MuSigMediationCaseDetailsModel model;
    @Getter
    private final MuSigMediationCaseDetailsView view;


    public MuSigMediationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_MEDIATION_CASE_DETAILS);

        model = new MuSigMediationCaseDetailsModel();
        view = new MuSigMediationCaseDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData initData) {
        model.setMuSigMediationCaseListItem(initData.muSigMediationCaseListItem);
    }

    @Override
    public void onActivate() {
        MuSigMediationCaseListItem muSigMediationCaseListItem = model.getMuSigMediationCaseListItem();
        MuSigOpenTradeChannel channel = muSigMediationCaseListItem.getChannel();
        MuSigMediationCase muSigMediationCase = muSigMediationCaseListItem.getMuSigMediationCase();
        MuSigMediationRequest muSigMediationRequest = muSigMediationCase.getMuSigMediationRequest();
        MuSigContract contract = muSigMediationRequest.getContract();
        MuSigOffer offer = contract.getOffer();
        String tradeId = muSigMediationRequest.getTradeId();

        model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));

        model.setOfferType(offer.getDirection().isBuy()
                ? Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.buyOffer")
                : Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.sellOffer"));
        model.setMarket(Res.get("bisqEasy.openTrades.tradeDetails.offerTypeAndMarket.fiatMarket",
                offer.getMarket().getQuoteCurrencyCode()));
        model.setFiatAmount(MuSigTradeFormatter.formatQuoteSideAmount(contract));
        model.setFiatCurrency(offer.getMarket().getQuoteCurrencyCode());
        model.setBtcAmount(MuSigTradeFormatter.formatBaseSideAmount(contract));
        model.setPrice(PriceFormatter.format(MuSigTradeUtils.getPriceQuote(contract)));
        model.setPriceCodes(offer.getMarket().getMarketCodes());
        model.setPriceSpec(offer.getPriceSpec() instanceof FixPriceSpec
                ? ""
                : String.format("(%s)", PriceSpecFormatter.getFormattedPriceSpec(offer.getPriceSpec(), true)));
        model.setPaymentMethod(contract.getQuoteSidePaymentMethodSpec().getShortDisplayString());
        model.setSettlementMethod(contract.getBaseSidePaymentMethodSpec().getShortDisplayString());
        model.setTradeId(tradeId);

        MuSigMediationCaseListItem.Trader maker = muSigMediationCaseListItem.getMaker();
        MuSigMediationCaseListItem.Trader taker = muSigMediationCaseListItem.getTaker();
        MuSigMediationCaseListItem.Trader buyer = offer.getDirection().isBuy() ? maker : taker;
        MuSigMediationCaseListItem.Trader seller = offer.getDirection().isSell() ? maker : taker;
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
