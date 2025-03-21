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
import bisq.bonded_roles.explorer.dto.Tx;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.monetary.Coin;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.BitcoinTransactionValidator;
import bisq.desktop.components.controls.validator.ExplorerResultValidator;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class StateMainChain3b<C extends StateMainChain3b.Controller<?, ?>> extends BaseState {
    protected final C controller;

    protected StateMainChain3b(ServiceProvider serviceProvider,
                               BisqEasyTrade bisqEasyTrade,
                               BisqEasyOpenTradeChannel channel) {
        controller = getController(serviceProvider, bisqEasyTrade, channel);
    }

    protected abstract C getController(ServiceProvider serviceProvider,
                                       BisqEasyTrade bisqEasyTrade,
                                       BisqEasyOpenTradeChannel channel);

    protected static abstract class Controller<M extends StateMainChain3b.Model, V extends StateMainChain3b.View<?, ?>> extends BaseState.Controller<M, V> {
        private final static Map<String, Tx> CONFIRMED_TX_CACHE = new HashMap<>();

        private final ExplorerService explorerService;
        @Nullable
        private UIScheduler scheduler;
        @Nullable
        private CompletableFuture<Tx> requestFuture;

        protected Controller(ServiceProvider serviceProvider,
                             BisqEasyTrade bisqEasyTrade,
                             BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            explorerService = serviceProvider.getBondedRolesService().getExplorerService();
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.setPaymentProof(model.getBisqEasyTrade().getPaymentProof().get());
            model.setBitcoinPaymentData(model.getBisqEasyTrade().getBitcoinPaymentData().get());

            if (model.getConfirmationState().get() == null) {
                model.getConfirmationState().set(Model.ConfirmationState.REQUEST_STARTED);
                requestTx();
            }

            if (model.getConfirmationState().get() == Model.ConfirmationState.CONFIRMED) {
                model.getButtonText().set(Res.get("bisqEasy.tradeState.info.phase3b.button.next"));
            } else {
                model.getButtonText().set(Res.get("bisqEasy.tradeState.info.phase3b.button.skip"));
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
            if (scheduler != null) {
                scheduler.stop();
                scheduler = null;
            }
            if (requestFuture != null) {
                requestFuture.cancel(true);
                requestFuture = null;
            }
        }

        public void openExplorer() {
            ExplorerService.Provider provider = explorerService.getSelectedProvider().get();
            String url = provider.getBaseUrl() + "/" + provider.getTxPath() + model.getPaymentProof();
            Browser.open(url);
        }

        void onCompleteTrade() {
            if (model.getExplorerResultValidator().isHasErrors()) {
                String warning = createWarningMessage();
                showWarningPopup(warning);
            } else {
                doCompleteTrade();
            }
        }

        private String createWarningMessage() {
            String txValue = model.getBtcBalance().get();
            String address = model.getBitcoinPaymentData();
            String txId = model.getPaymentProof();

            if (StringUtils.isEmpty(txValue)) {
                return Res.get("bisqEasy.tradeState.info.phase3b.button.next.noOutputForAddress", address, txId);
            } else {
                String tradeAmount = getFormattedBaseAmount(model.getBisqEasyTrade().getContract().getBaseSideAmount());
                return Res.get("bisqEasy.tradeState.info.phase3b.button.next.amountNotMatching", address, txId, txValue, tradeAmount);
            }
        }

        private void showWarningPopup(String warning) {
            new Popup().warning(warning)
                    .actionButtonText(Res.get("bisqEasy.tradeState.info.phase3b.button.next.amountNotMatching.resolved"))
                    .onAction(this::doCompleteTrade)
                    .show();
        }

        private void doCompleteTrade() {
            // todo should we send a system message? if so we should change the text
            //sendTradeLogMessage(Res.get("bisqEasy.tradeState.info.phase3b.tradeLogMessage", model.getChannel().getMyUserIdentity().getUserName()));
            bisqEasyTradeService.btcConfirmed(model.getBisqEasyTrade());
        }

        private void requestTx() {
            String paymentProof = model.getPaymentProof();
            if (CONFIRMED_TX_CACHE.containsKey(paymentProof)) {
                Tx tx = CONFIRMED_TX_CACHE.get(paymentProof);
                handleTxBalance(tx);
                model.getIsConfirmed().set(tx.getStatus().isConfirmed());
                model.getConfirmationState().set(Model.ConfirmationState.CONFIRMED);
                model.getButtonText().set(Res.get("bisqEasy.tradeState.info.phase3b.button.next"));
                model.getConfirmationInfo().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.confirmed"));
                if (scheduler != null) {
                    scheduler.stop();
                }
                return;
            }

            model.getConfirmationInfo().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup",
                    explorerService.getSelectedProvider().get().getBaseUrl()));
            requestFuture = explorerService.requestTx(paymentProof)
                    .whenComplete((tx, throwable) -> UIThread.run(() -> {
                        if (scheduler != null) {
                            scheduler.stop();
                        }
                        if (throwable == null) {
                            handleTxBalance(tx);
                            model.getIsConfirmed().set(tx.getStatus().isConfirmed());
                            if (tx.getStatus().isConfirmed()) {
                                CONFIRMED_TX_CACHE.put(paymentProof, tx);
                                model.getConfirmationState().set(Model.ConfirmationState.CONFIRMED);
                                model.getButtonText().set(Res.get("bisqEasy.tradeState.info.phase3b.button.next"));
                                model.getConfirmationInfo().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.confirmed"));
                                if (scheduler != null) {
                                    scheduler.stop();
                                }
                            } else {
                                model.getConfirmationState().set(Model.ConfirmationState.IN_MEMPOOL);
                                model.getConfirmationInfo().set(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.notConfirmed"));
                                scheduler = UIScheduler.run(this::requestTx).after(20, TimeUnit.SECONDS);
                            }
                        } else {
                            model.getConfirmationState().set(Model.ConfirmationState.FAILED);
                            Throwable rootCause = ExceptionUtil.getRootCause(throwable);
                            model.getConfirmationInfo().set(Res.get("bisqEasy.tradeState.info.phase3b.txId.failed",
                                    explorerService.getSelectedProvider().get().getBaseUrl(),
                                    rootCause.getClass().getSimpleName(),
                                    ExceptionUtil.getRootCauseMessage(rootCause)));
                        }
                    }));
        }

        private void handleTxBalance(Tx tx) {
            model.getBtcBalance().set("");
            ExplorerResultValidator explorerResultValidator = model.getExplorerResultValidator();
            explorerResultValidator.setMessage(null);
            List<Long> txOutputValuesForAddress = findTxOutputValuesForAddress(tx, model.getBitcoinPaymentData());
            if (txOutputValuesForAddress.isEmpty()) {
                explorerResultValidator.setMessage(Res.get("bisqEasy.tradeState.info.phase3b.balance.invalid.noOutputsForAddress"));
            } else if (txOutputValuesForAddress.size() == 1) {
                long outputValue = txOutputValuesForAddress.get(0);
                model.getBtcBalance().set(getFormattedBaseAmount(outputValue));
                long tradeAmount = model.getBisqEasyTrade().getContract().getBaseSideAmount();
                if (outputValue != tradeAmount) {
                    explorerResultValidator.setMessage(Res.get("bisqEasy.tradeState.info.phase3b.balance.invalid.amountNotMatching"));
                }
            } else {
                // Not expected use case and not further handled. User should look up in explorer to validate tx
                explorerResultValidator.setMessage(Res.get("bisqEasy.tradeState.info.phase3b.balance.invalid.multipleOutputsForAddress"));
            }
            UIThread.runOnNextRenderFrame(explorerResultValidator::validate);
        }

        private static String getFormattedBaseAmount(long value) {
            return AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(value));
        }

        private List<Long> findTxOutputValuesForAddress(Tx tx, String address) {
            return tx.getOutputs().stream()
                    .filter(output -> address.equals(output.getAddress()))
                    .map(Output::getValue)
                    .toList();
        }
    }

    @Getter
    protected abstract static class Model extends BaseState.Model {
        enum ConfirmationState {
            REQUEST_STARTED,
            IN_MEMPOOL,
            CONFIRMED,
            FAILED,
        }

        @Setter
        protected String bitcoinPaymentData;
        @Setter
        protected String paymentProof;
        @Setter
        private long txOutputValueForAddress;
        @Setter
        private String role;

        private final StringProperty btcBalance = new SimpleStringProperty();
        private final StringProperty confirmationInfo = new SimpleStringProperty();
        private final StringProperty buttonText = new SimpleStringProperty();
        private final BooleanProperty isConfirmed = new SimpleBooleanProperty();
        private final ObjectProperty<ConfirmationState> confirmationState = new SimpleObjectProperty<>();
        private final ExplorerResultValidator explorerResultValidator = new ExplorerResultValidator();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
            role = this instanceof BuyerStateMainChain3b.Model ? "buyer" : "seller";
        }
    }

    public static abstract class View<M extends StateMainChain3b.Model, C extends StateMainChain3b.Controller<?, ?>> extends BaseState.View<M, C> {
        private final Button button;
        private final MaterialTextField paymentProof, btcBalance;
        private final WaitingAnimation waitingAnimation;
        private Subscription confirmationStatePin;

        protected View(M model, C controller) {
            super(model, controller);

            String role = model.getRole();
            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info." + role + ".phase3b.headline.MAIN_CHAIN"));
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info." + role + ".phase3b.info.MAIN_CHAIN"));
            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            paymentProof = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.phase3b.txId"), "", false);
            paymentProof.setIcon(AwesomeIcon.EXTERNAL_LINK);
            paymentProof.setIconTooltip(Res.get("bisqEasy.tradeState.info.phase3b.txId.tooltip"));
            paymentProof.setValidator(new BitcoinTransactionValidator());

            btcBalance = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info." + role + ".phase3b.balance"), "", false);
            btcBalance.setHelpText(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup"));
            btcBalance.setPromptText(Res.get("bisqEasy.tradeState.info." + role + ".phase3b.balance.prompt"));
            btcBalance.filterMouseEventOnNonEditableText();
            btcBalance.setValidator(model.getExplorerResultValidator());

            button = new Button();
            VBox.setMargin(button, new Insets(5, 0, 5, 0));

            root.getChildren().addAll(waitingInfo, paymentProof, btcBalance, button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            paymentProof.setText(model.getPaymentProof());
            paymentProof.validate();

            button.defaultButtonProperty().bind(model.getIsConfirmed());
            button.textProperty().bind(model.getButtonText());
            btcBalance.textProperty().bind(model.getBtcBalance());
            btcBalance.helpProperty().bind(model.getConfirmationInfo());
            btcBalance.validate();

            confirmationStatePin = EasyBind.subscribe(model.getConfirmationState(), confirmationState -> {
                if (confirmationState != null) {
                    btcBalance.validate();
                    btcBalance.getHelpLabel().getStyleClass().remove("tx-lookup-in-mempool");
                    btcBalance.getHelpLabel().getStyleClass().remove("tx-lookup-confirmed");
                    btcBalance.getHelpLabel().getStyleClass().remove("tx-lookup-failed");
                    switch (confirmationState) {
                        case REQUEST_STARTED:
                            break;
                        case IN_MEMPOOL:
                            btcBalance.getHelpLabel().getStyleClass().add("tx-lookup-in-mempool");
                            break;
                        case CONFIRMED:
                            btcBalance.getHelpLabel().getStyleClass().add("tx-lookup-confirmed");
                            break;
                        case FAILED:
                            btcBalance.getHelpLabel().getStyleClass().add("tx-lookup-failed");
                            break;
                    }
                }
            });

            button.setOnAction(e -> controller.onCompleteTrade());
            paymentProof.getIconButton().setOnAction(e -> controller.openExplorer());

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.defaultButtonProperty().unbind();
            button.textProperty().unbind();
            btcBalance.textProperty().unbind();
            btcBalance.helpProperty().unbind();
            confirmationStatePin.unsubscribe();

            button.setOnAction(null);
            paymentProof.getIconButton().setOnAction(null);

            paymentProof.resetValidation();
            btcBalance.resetValidation();

            waitingAnimation.stop();
        }
    }
}
