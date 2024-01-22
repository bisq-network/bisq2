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

import bisq.bonded_roles.explorer.ExplorerService;
import bisq.bonded_roles.explorer.dto.Output;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.monetary.Coin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class BuyerState3b extends BaseState {
    private final Controller controller;

    public BuyerState3b(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final ExplorerService explorerService;
        private UIScheduler scheduler;

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

            model.setTxId(model.getBisqEasyTrade().getTxId().get());
            model.setBtcAddress(model.getBisqEasyTrade().getBtcAddress().get());
            model.getBtcBalance().set("");
            model.getConfirmationState().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup"));
            requestTx();
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            if (scheduler != null) {
                scheduler.stop();
                scheduler = null;
            }
        }

        public void openExplorer() {
            ExplorerService.Provider provider = explorerService.getSelectedProvider().get();
            String url = provider.getBaseUrl() + provider.getTxPath() + model.getTxId();
            Browser.open(url);
        }

        private void onSkip() {
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
        }

        private void requestTx() {
            explorerService.requestTx(model.getTxId())
                    .whenComplete((tx, throwable) -> {
                        UIThread.run(() -> {
                            if (scheduler != null) {
                                scheduler.stop();
                            }
                            if (throwable == null) {
                                model.btcBalance.set(
                                        tx.getOutputs().stream()
                                                .filter(output -> model.getBtcAddress().equals(output.getAddress()))
                                                .map(Output::getValue)
                                                .map(Coin::asBtcFromValue)
                                                .map(e -> AmountFormatter.formatAmountWithCode(e, false))
                                                .findAny()
                                                .orElse(""));
                                model.getIsConfirmed().set(tx.getStatus().isConfirmed());
                                if (tx.getStatus().isConfirmed()) {
                                    onTxConfirmed();
                                } else {
                                    model.getConfirmationState().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.notConfirmed"));
                                    scheduler = UIScheduler.run(this::requestTx).after(20, TimeUnit.SECONDS);
                                }
                            } else {
                                model.getConfirmationState().set(Res.get("bisqEasy.tradeState.info.phase3b.txId.failed"));
                            }
                        });
                    });
        }

        private void onTxConfirmed() {
            model.getConfirmationState().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.confirmed"));
            sendSystemMessage(Res.get("bisqEasy.tradeState.info.phase3b.systemMessage", model.getFormattedBaseAmount(), model.btcAddress));
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected String btcAddress;
        @Setter
        protected String txId;
        private final StringProperty btcBalance = new SimpleStringProperty();
        private final StringProperty confirmationState = new SimpleStringProperty();
        private final BooleanProperty isConfirmed = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button skipButton;
        private final MaterialTextField txId, btcBalance;
        private final WaitingAnimation waitingAnimation;

        private View(Model model, Controller controller) {
            super(model, controller);

            txId = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.phase3b.txId"), "", false);
            txId.setIcon(AwesomeIcon.EXTERNAL_LINK);
            txId.setIconTooltip(Res.get("bisqEasy.tradeState.info.phase3b.txId.tooltip"));
            btcBalance = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase3b.balance"), "", false);
            btcBalance.setHelpText(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup"));
            btcBalance.setPromptText(Res.get("bisqEasy.tradeState.info.buyer.phase3b.balance.prompt"));
            btcBalance.filterMouseEventOnNonEditableText();

            skipButton = new Button(Res.get("bisqEasy.tradeState.info.phase3b.buttonText"));
            VBox.setMargin(skipButton, new Insets(5, 0, 5, 0));

            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.buyer.phase3b.headline"));
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.buyer.phase3b.info"));
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            root.getChildren().addAll(waitingInfo, txId, btcBalance, skipButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            txId.setText(model.getTxId());

            skipButton.defaultButtonProperty().bind(model.isConfirmed);
            btcBalance.textProperty().bind(model.getBtcBalance());
            btcBalance.helpHelpProperty().bind(model.getConfirmationState());

            skipButton.setOnAction(e -> controller.onSkip());
            txId.getIconButton().setOnAction(e -> controller.openExplorer());

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            skipButton.defaultButtonProperty().unbind();
            btcBalance.textProperty().unbind();
            btcBalance.helpHelpProperty().unbind();

            skipButton.setOnAction(null);
            txId.getIconButton().setOnAction(null);

            waitingAnimation.stop();
        }
    }
}
