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
import bisq.support.mediation.mu_sig.MuSigMediatorService;
import bisq.support.mediation.mu_sig.MuSigMediationRequest;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

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
    private final MuSigMediatorService muSigMediatorService;


    public MuSigMediationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_MEDIATION_CASE_DETAILS);

        model = new MuSigMediationCaseDetailsModel();
        view = new MuSigMediationCaseDetailsView(model, this);
        muSigMediatorService = serviceProvider.getSupportService().getMuSigMediatorService();
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
        model.setBuyerSecurityDeposit(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
        model.setBuyerSecurityDepositEmpty(true);
        model.setSellerSecurityDeposit(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
        model.setSellerSecurityDepositEmpty(true);
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
        model.setDepositTxId(Res.get("bisqEasy.openTrades.tradeDetails.dataNotYetProvided"));
        model.setDepositTxIdEmpty(true);
        model.setTradeId(tradeId);

        MuSigMediationCaseListItem.Trader maker = muSigMediationCaseListItem.getMaker();
        MuSigMediationCaseListItem.Trader taker = muSigMediationCaseListItem.getTaker();
        MuSigMediationCaseListItem.Trader buyer = offer.getDirection().isBuy() ? maker : taker;
        MuSigMediationCaseListItem.Trader seller = offer.getDirection().isSell() ? maker : taker;
        CaseCounts buyerCaseCounts = getCaseCounts(buyer.getUserProfile());
        CaseCounts sellerCaseCounts = getCaseCounts(seller.getUserProfile());
        model.setBuyerUserName(buyer.getUserName());
        model.setSellerUserName(seller.getUserName());
        model.setBuyerBotId(buyer.getUserProfile().getNym());
        model.setBuyerUserId(buyer.getUserProfile().getId());
        model.setBuyerCaseCountTotal(buyerCaseCounts.total());
        model.setBuyerCaseCountOpen(buyerCaseCounts.open());
        model.setBuyerCaseCountClosed(buyerCaseCounts.closed());
        model.setSellerBotId(seller.getUserProfile().getNym());
        model.setSellerUserId(seller.getUserProfile().getId());
        model.setSellerCaseCountTotal(sellerCaseCounts.total());
        model.setSellerCaseCountOpen(sellerCaseCounts.open());
        model.setSellerCaseCountClosed(sellerCaseCounts.closed());
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

    private CaseCounts getCaseCounts(UserProfile userProfile) {
        var counts = muSigMediatorService.getMediationCases().stream()
                .filter(mediationCase -> {
                    MuSigMediationRequest request = mediationCase.getMuSigMediationRequest();
                    return userProfile.equals(request.getRequester()) || userProfile.equals(request.getPeer());
                })
                .collect(Collectors.partitioningBy(mediationCase -> mediationCase.getIsClosed().get(),
                        Collectors.counting()));
        int closed = counts.getOrDefault(true, 0L).intValue();
        int open = counts.getOrDefault(false, 0L).intValue();
        return new CaseCounts(open + closed, open, closed);
    }

    private record CaseCounts(int total, int open, int closed) {
    }
}
