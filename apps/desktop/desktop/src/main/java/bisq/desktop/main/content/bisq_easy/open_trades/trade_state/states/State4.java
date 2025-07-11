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

package bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states;

import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.explorer.ExplorerService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.data.Pair;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_details.TradeDetailsController;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.OpenTradesUtils;
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class State4<C extends State4.Controller<?, ?>> extends BaseState {
    protected final C controller;

    public State4(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = getController(serviceProvider, bisqEasyTrade, channel);
    }

    protected abstract C getController(ServiceProvider serviceProvider,
                                       BisqEasyTrade bisqEasyTrade,
                                       BisqEasyOpenTradeChannel channel);

    protected static abstract class Controller<M extends State4.Model, V extends State4.View<?, ?>> extends BaseState.Controller<M, V> {
        private final ReputationService reputationService;
        protected final ExplorerService explorerService;

        protected Controller(ServiceProvider serviceProvider,
                             BisqEasyTrade bisqEasyTrade,
                             BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            explorerService = serviceProvider.getBondedRolesService().getExplorerService();
            reputationService = serviceProvider.getUserService().getReputationService();
        }

        @Override
        public void onActivate() {
            super.onActivate();

            BisqEasyTrade bisqEasyTrade = model.getBisqEasyTrade();
            BisqEasyContract contract = bisqEasyTrade.getContract();
            BitcoinPaymentRail paymentRail = contract.getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            String name = paymentRail.name();
            model.setPaymentProofDescription(Res.get("bisqEasy.tradeState.paymentProof." + name));
            model.setBlockExplorerLinkVisible(paymentRail == BitcoinPaymentRail.MAIN_CHAIN);
            String paymentProof = bisqEasyTrade.getPaymentProof().get();
            model.setPaymentProof(paymentProof);
            model.setPaymentProofVisible(paymentProof != null);
            UserProfile tradePeer = model.getChannel().getPeer();
            model.setTradePeer(tradePeer);
            model.setBuyer(bisqEasyTrade.isBuyer());
            model.setFiatCurrency(bisqEasyTrade.getOffer().getMarket().getQuoteCurrencyCode());
            model.setPaymentMethod(contract.getQuoteSidePaymentMethodSpec().getShortDisplayString());
            model.setTradeId(bisqEasyTrade.getShortId());
            long takeOfferDate = contract.getTakeOfferDate();
            model.setTradeDate(DateFormatter.formatDateTime(takeOfferDate));

            String tradeDuration = bisqEasyTrade.getTradeCompletedDate()
                    .map(tradeCompletedDate -> tradeCompletedDate - takeOfferDate)
                    .map(TimeFormatter::formatAge)
                    .orElse("");
            model.setTradeDuration(tradeDuration);

            model.setPrice(PriceFormatter.format(BisqEasyTradeUtils.getPriceQuote(bisqEasyTrade)));
            model.setPriceSymbol(model.getBisqEasyOffer().getMarket().getMarketCodes());
            model.setTradePeerReputationScore(reputationService.findReputationScore(tradePeer).orElse(ReputationScore.NONE));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        protected void onCloseCompletedTrade() {
            new Popup().feedback(Res.get("bisqEasy.openTrades.closeTrade.warning.completed"))
                    .actionButtonText(Res.get("bisqEasy.openTrades.confirmCloseTrade"))
                    .onAction(() -> {
                        bisqEasyTradeService.removeTrade(model.getBisqEasyTrade());
                        leavePrivateChatManager.leaveChannel(model.getChannel());
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        }

        protected void onShowDetails() {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_TRADE_DETAILS,
                    new TradeDetailsController.InitData(model.getBisqEasyTrade(), model.getChannel()));
        }

        protected void onExportTrade() {
            OpenTradesUtils.exportTrade(model.getBisqEasyTrade(), getView().getRoot().getScene());
        }

        protected void openExplorer() {
            Browser.open(getBlockExplorerUrl());
        }

        protected void onCopyExplorerLink() {
            ClipboardUtil.copyToClipboard(getBlockExplorerUrl());
        }

        protected String getBlockExplorerUrl() {
            ExplorerService.Provider provider = explorerService.getSelectedProvider().get();
            return provider.getBaseUrl() + "/" + provider.getTxPath() + model.getPaymentProof();
        }
    }

    @Setter
    @Getter
    protected static class Model extends BaseState.Model {
        protected String paymentProof;
        protected String paymentProofDescription;
        protected boolean blockExplorerLinkVisible;
        protected boolean paymentProofVisible;
        protected UserProfile tradePeer;
        protected boolean isBuyer;
        protected String fiatCurrency;
        protected String paymentMethod;
        protected String tradeId;
        protected String tradeDate;
        protected String tradeDuration;
        protected String price;
        protected String priceSymbol;
        protected Optional<String> txId = Optional.empty();
        protected ReputationScore tradePeerReputationScore;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static abstract class View<M extends State4.Model, C extends State4.Controller<?, ?>> extends BaseState.View<M, C> {
        private final UserProfileDisplay peerProfileDisplay;
        protected final Button closeTradeButton, exportButton, detailsButton;
        protected final TradeCompletedTable tradeCompletedTable;

        protected View(M model, C controller) {
            super(model, controller);

            tradeCompletedTable = new TradeCompletedTable();
            peerProfileDisplay = new UserProfileDisplay();

            detailsButton = new Button(Res.get("bisqEasy.tradeState.info.phase4.showDetails"));
            exportButton = new Button(Res.get("bisqEasy.tradeState.info.phase4.exportTrade"));
            closeTradeButton = new Button(Res.get("bisqEasy.tradeState.info.phase4.leaveChannel"));
            closeTradeButton.setDefaultButton(true);
            HBox buttons = new HBox(20, detailsButton, exportButton, closeTradeButton);
            buttons.setAlignment(Pos.BOTTOM_RIGHT);
            VBox.setMargin(buttons, new Insets(0, 0, 20, 0));

            VBox content = new VBox(10, tradeCompletedTable, buttons);
            content.setMaxWidth(1160);
            root.getChildren().addAll(content);
            root.setAlignment(Pos.CENTER);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            Optional<Pair<String, String>> txIdDescriptionAndValue = Optional.empty();
            if (model.isPaymentProofVisible()) {
                txIdDescriptionAndValue = Optional.of(new Pair<>(model.getPaymentProofDescription(), model.getPaymentProof()));
            }
            peerProfileDisplay.setUserProfile(model.getTradePeer());
            peerProfileDisplay.setReputationScore(model.getTradePeerReputationScore());
            tradeCompletedTable.initialize(peerProfileDisplay, model.isBuyer(), model.getBaseAmount(),
                    model.getQuoteAmount(), model.getFiatCurrency(), model.getPaymentMethod(), model.getTradeId(),
                    model.getTradeDate(), model.getTradeDuration(), model.getPrice(), model.getPriceSymbol(), txIdDescriptionAndValue);
            if (model.isBlockExplorerLinkVisible()) {
                tradeCompletedTable.showBlockExplorerLink();
                tradeCompletedTable.getOpenTxExplorerButton().setOnAction(e -> controller.openExplorer());
                tradeCompletedTable.getCopyTxExplorerLinkButton().setOnAction(e -> controller.onCopyExplorerLink());
            }
            detailsButton.setOnAction(e -> controller.onShowDetails());
            exportButton.setOnAction(e -> controller.onExportTrade());
            closeTradeButton.setOnAction(e -> controller.onCloseCompletedTrade());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            tradeCompletedTable.dispose();
            peerProfileDisplay.dispose();

            detailsButton.setOnAction(null);
            exportButton.setOnAction(null);
            closeTradeButton.setOnAction(null);
        }
    }
}
