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

package bisq.desktop.main.content.authorized_role.mediator.bisq_easy.details;

import bisq.desktop.navigation.NavigationTarget;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.main.content.authorized_role.mediator.bisq_easy.BisqEasyMediationCaseListItem;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.bisq_easy.BisqEasyMediationCase;
import bisq.support.mediation.bisq_easy.BisqEasyMediatorService;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequest;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class BisqEasyMediationCaseDetailsController extends NavigationController implements InitWithDataController<BisqEasyMediationCaseDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final BisqEasyMediationCaseListItem bisqEasyMediationCaseListItem;

        public InitData(BisqEasyMediationCaseListItem bisqEasyMediationCaseListItem) {
            this.bisqEasyMediationCaseListItem = bisqEasyMediationCaseListItem;
        }
    }

    @Getter
    private final BisqEasyMediationCaseDetailsModel model;
    @Getter
    private final BisqEasyMediationCaseDetailsView view;
    private final BisqEasyMediatorService bisqEasyMediatorService;


    public BisqEasyMediationCaseDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.BISQ_EASY_MEDIATION_CASE_DETAILS);

        model = new BisqEasyMediationCaseDetailsModel();
        view = new BisqEasyMediationCaseDetailsView(model, this);
        bisqEasyMediatorService = serviceProvider.getSupportService().getBisqEasyMediatorService();
    }

    @Override
    public void initWithData(InitData initData) {
        model.setBisqEasyMediationCaseListItem(initData.bisqEasyMediationCaseListItem);
    }

    @Override
    public void onActivate() {
        BisqEasyMediationCaseListItem bisqEasyMediationCaseListItem = model.getBisqEasyMediationCaseListItem();
        BisqEasyOpenTradeChannel channel = bisqEasyMediationCaseListItem.getChannel();
        BisqEasyMediationCase bisqEasyMediationCase = bisqEasyMediationCaseListItem.getBisqEasyMediationCase();
        BisqEasyMediationRequest bisqEasyMediationRequest = bisqEasyMediationCase.getBisqEasyMediationRequest();
        BisqEasyContract contract = bisqEasyMediationRequest.getContract();
        BisqEasyOffer offer = contract.getOffer();
        String tradeId = bisqEasyMediationRequest.getTradeId();

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

        BisqEasyMediationCaseListItem.Trader maker = bisqEasyMediationCaseListItem.getMaker();
        BisqEasyMediationCaseListItem.Trader taker = bisqEasyMediationCaseListItem.getTaker();
        BisqEasyMediationCaseListItem.Trader buyer = offer.getDirection().isBuy() ? maker : taker;
        BisqEasyMediationCaseListItem.Trader seller = offer.getDirection().isSell() ? maker : taker;
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
        var counts = bisqEasyMediatorService.getMediationCases().stream()
                .filter(mediationCase -> {
                    BisqEasyMediationRequest request = mediationCase.getBisqEasyMediationRequest();
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
