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
import bisq.bonded_roles.explorer.ExplorerService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.OpenTradesUtils;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SellerState4 extends BaseState {
    private final Controller controller;

    public SellerState4(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final ExplorerService explorerService;

        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            explorerService = serviceProvider.getBondedRolesService().getExplorerService();
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();

            BitcoinPaymentRail paymentRail = model.getBisqEasyTrade().getContract().getBaseSidePaymentMethodSpec().getPaymentMethod().getPaymentRail();
            String name = paymentRail.name();
            model.setPaymentProofDescription(Res.get("bisqEasy.tradeState.paymentProof." + name));
            model.setBlockExplorerLinkVisible(paymentRail == BitcoinPaymentRail.MAIN_CHAIN);
            String paymentProof = model.getBisqEasyTrade().getPaymentProof().get();
            model.setPaymentProof(paymentProof);
            model.setPaymentProofVisible(paymentProof != null);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onLeaveChannel() {
            new Popup().feedback(Res.get("bisqEasy.openTrades.closeTrade.warning.completed"))
                    .actionButtonText(Res.get("bisqEasy.openTrades.confirmCloseTrade"))
                    .onAction(() -> {
                        bisqEasyTradeService.removeTrade(model.getBisqEasyTrade());
                        leavePrivateChatManager.leaveChatChannel(model.getChannel());
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        }

        private void onExportTrade() {
            OpenTradesUtils.exportTrade(model.getBisqEasyTrade(), getView().getRoot().getScene());
        }

        private void openExplorer() {
            ExplorerService.Provider provider = explorerService.getSelectedProvider().get();
            String url = provider.getBaseUrl() + provider.getTxPath() + model.getPaymentProof();
            Browser.open(url);
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String paymentProof;
        @Setter
        protected String paymentProofDescription;
        @Setter
        protected boolean blockExplorerLinkVisible;
        @Setter
        protected boolean paymentProofVisible;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button leaveButton, exportButton;
        private final MaterialTextField quoteAmount, baseAmount;
        private final MaterialTextField paymentProof;

        private View(Model model, Controller controller) {
            super(model, controller);

            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.seller.phase4.headline"));

            exportButton = new Button(Res.get("bisqEasy.tradeState.info.phase4.exportTrade"));
            leaveButton = new Button(Res.get("bisqEasy.tradeState.info.phase4.leaveChannel"));
            leaveButton.getStyleClass().add("outlined-button");
            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase4.quoteAmount"), "", false);
            baseAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.seller.phase4.baseAmount"), "", false);

            paymentProof = FormUtils.getTextField("", "", false);

            HBox buttons = new HBox(leaveButton, Spacer.fillHBox(), exportButton);

            VBox.setMargin(headline, new Insets(0, 0, 5, 0));
            VBox.setMargin(buttons, new Insets(5, 0, 5, 0));
            root.getChildren().addAll(
                    headline,
                    quoteAmount,
                    baseAmount,
                    paymentProof,
                    buttons);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            paymentProof.setVisible(model.isPaymentProofVisible());
            paymentProof.setManaged(model.isPaymentProofVisible());
            paymentProof.setDescription(model.getPaymentProofDescription());
            paymentProof.setText(model.getPaymentProof());
            if (model.isBlockExplorerLinkVisible()) {
                paymentProof.setIcon(AwesomeIcon.EXTERNAL_LINK);
                paymentProof.getIconButton().setOnAction(e -> controller.openExplorer());
                paymentProof.setIconTooltip(Res.get("bisqEasy.tradeState.info.phase4.txId.tooltip"));
            }

            quoteAmount.setText(model.getFormattedQuoteAmount());
            baseAmount.setText(model.getFormattedBaseAmount());

            leaveButton.setOnAction(e -> controller.onLeaveChannel());
            exportButton.setOnAction(e -> controller.onExportTrade());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            leaveButton.setOnAction(null);
            exportButton.setOnAction(null);
            paymentProof.getIconButton().setOnAction(null);
        }
    }
}
