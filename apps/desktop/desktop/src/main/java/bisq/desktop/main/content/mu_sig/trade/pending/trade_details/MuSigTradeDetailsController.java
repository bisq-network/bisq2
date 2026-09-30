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

package bisq.desktop.main.content.mu_sig.trade.pending.trade_details;

import bisq.account.accounts.AccountPayload;
import bisq.bonded_roles.explorer.ExplorerService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.common.monetary.Monetary;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.options.OfferOptionUtil;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeFormatter;
import bisq.trade.mu_sig.MuSigTradeUtils;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigTradeDetailsController extends NavigationController implements InitWithDataController<MuSigTradeDetailsController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final MuSigTrade trade;
        private final MuSigOpenTradeChannel channel;

        public InitData(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            this.trade = trade;
            this.channel = channel;
        }
    }

    @Getter
    private final MuSigTradeDetailsModel model;
    @Getter
    private final MuSigTradeDetailsView view;

    private final ExplorerService explorerService;


    public MuSigTradeDetailsController(ServiceProvider serviceProvider) {
        super(NavigationTarget.MU_SIG_TRADE_DETAILS);

        explorerService = serviceProvider.getBondedRolesService().getExplorerService();

        model = new MuSigTradeDetailsModel();
        view = new MuSigTradeDetailsView(model, this);
    }

    @Override
    public void initWithData(InitData initData) {
        model.setTrade(initData.trade);
        model.setChannel(initData.channel);
    }

    @Override
    public void onActivate() {
        MuSigTrade trade = model.getTrade();
        MuSigOpenTradeChannel channel = model.getChannel();
        MuSigContract contract = trade.getContract();
        model.setContract(contract);
        boolean isBaseCurrencyBitcoin = trade.getMarket().isBaseCurrencyBitcoin();

        model.setTradeDate(DateFormatter.formatDateTime(contract.getTakeOfferDate()));

        Optional<String> tradeDuration = trade.getTradeCompletedDate()
                .map(tradeCompletedDate -> tradeCompletedDate - contract.getTakeOfferDate())
                .map(TimeFormatter::formatAge);
        model.setTradeDuration(tradeDuration);

        model.setMe(String.format("%s (%s)", channel.getMyUserIdentity().getNickName(), MuSigTradeFormatter.getMakerTakerRole(trade).toLowerCase()));
        model.setPeer(channel.getPeer().getUserName());
        model.setOfferType(trade.getOffer().getDisplayDirection().isBuy()
                ? Res.get("muSig.trade.details.offerTypeAndMarket.buyOffer")
                : Res.get("muSig.trade.details.offerTypeAndMarket.sellOffer"));
        model.setMarket(Res.get("muSig.trade.details.offerTypeAndMarket.nonBtcMarket",
                trade.getOffer().getMarket().getRelevantCurrencyCode()));

        model.setPaymentMethod(contract.getNonBtcSidePaymentMethodSpec().getShortDisplayString());
        model.setPaymentMethodsBoxVisible(isBaseCurrencyBitcoin);

        model.setTradeId(trade.getId());
        model.setPeerNetworkAddress(channel.getPeer().getAddressByTransportDisplayString(50));

        Optional<AccountPayload<?>> peersAccountPayload = trade.getPeer().getAccountPayload();
        model.setPaymentAccountDataEmpty(peersAccountPayload.isEmpty());

        model.setPeersPaymentAccountDataDescription(isBaseCurrencyBitcoin
                ? Res.get("muSig.trade.details.paymentAccountData.fiat")
                : Res.get("muSig.trade.details.paymentAccountData.crypto", trade.getMarket().getRelevantCurrencyCode())
        );
        model.setPeersPaymentAccountData(peersAccountPayload.isEmpty()
                ? Res.get("muSig.trade.details.dataNotYetProvided")
                : peersAccountPayload.get().getAccountDataDisplayString());

        model.setAssignedMediator(channel.getMediator().map(UserProfile::getUserName).orElse(""));
        model.setHasMediatorBeenAssigned(channel.getMediator().isPresent());

        String depositTxId = trade.getDepositTxId();
        boolean hasDepositTxId = depositTxId != null && !depositTxId.isBlank();
        model.setDepositTxId(hasDepositTxId
                ? depositTxId
                : Res.get("muSig.trade.details.dataNotYetProvided"));
        model.setDepositTxIdEmpty(!hasDepositTxId);
        model.setDepositTxIdVisible(hasDepositTxId);

        boolean hasExplorerProvider = explorerService.getExplorerServiceProvider().isPresent();
        model.setBlockExplorerLinkVisible(hasDepositTxId && hasExplorerProvider);

        model.setSecurityDepositInfo(createSecurityDepositInfo(contract));

        // todo: set real fee amount and percent once we have that information, for now we set it to N/A
        model.setFeeAmount(Res.get("data.na"));
        model.setFeePercent(Res.get("data.na"));
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

    void openExplorer() {
        Browser.open(getBlockExplorerUrl());
    }

    void onCopyExplorerLink() {
        ClipboardUtil.copyToClipboard(getBlockExplorerUrl());
    }

    private String getBlockExplorerUrl() {
        return getBlockExplorerUrl(explorerService, model.getDepositTxId());
    }

    private static String getBlockExplorerUrl(ExplorerService explorerService, String txId) {
        return explorerService.getExplorerServiceProvider()
                .map(provider -> provider.getBaseUrl() + "/" + provider.getTxPath() + "/" + txId)
                .orElse(Res.get("data.na"));
    }

    private static MuSigTradeDetailsModel.SecurityDepositInfo createSecurityDepositInfo(MuSigContract contract) {
        double percent = OfferOptionUtil.findSymmetricSecurityDepositPercent(contract.getOffer().getOfferOptions())
                .orElseThrow(() -> new IllegalArgumentException("CollateralOption must be present"));
        Monetary btcAmount = OfferAmountUtil.calculateSecurityDepositAsBTC(MuSigTradeUtils.getBtcSideMonetary(contract), percent);
        return new MuSigTradeDetailsModel.SecurityDepositInfo(
                PercentageFormatter.formatToPercentWithSymbol(percent, 0),
                OfferAmountFormatter.formatDepositAmountAsBTC(btcAmount));
    }
}

