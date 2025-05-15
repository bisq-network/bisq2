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

package bisq.desktop.main.content.mu_sig.portfolio.open_trades.trade_state.states;

import bisq.bonded_roles.explorer.ExplorerService;
import bisq.bonded_roles.explorer.dto.Output;
import bisq.bonded_roles.explorer.dto.Tx;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.common.monetary.Coin;
import bisq.common.util.ExceptionUtil;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.BitcoinTransactionValidator;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.mu_sig.MuSigTrade;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
public abstract class WaitForDepositConfirmationState<C extends WaitForDepositConfirmationState.Controller<?, ?>> extends BaseState {
    protected final C controller;

    protected WaitForDepositConfirmationState(ServiceProvider serviceProvider,
                                              MuSigTrade trade,
                                              MuSigOpenTradeChannel channel) {
        controller = getController(serviceProvider, trade, channel);
    }

    protected abstract C getController(ServiceProvider serviceProvider,
                                       MuSigTrade trade,
                                       MuSigOpenTradeChannel channel);

    protected static abstract class Controller<M extends Model, V extends View<?, ?>> extends BaseState.Controller<M, V> {
        private final static Map<String, Tx> CONFIRMED_TX_CACHE = new HashMap<>();

        private final ExplorerService explorerService;
        @Nullable
        private UIScheduler scheduler;
        @Nullable
        private CompletableFuture<Tx> requestFuture;

        protected Controller(ServiceProvider serviceProvider,
                             MuSigTrade trade,
                             MuSigOpenTradeChannel channel) {
            super(serviceProvider, trade, channel);

            explorerService = serviceProvider.getBondedRolesService().getExplorerService();
        }

        @Override
        public void onActivate() {
            super.onActivate();

            model.setTxId(model.getTrade().getPaymentProof().get());
            model.setBitcoinPaymentData(model.getTrade().getBitcoinPaymentData().get());

            if (model.getConfirmationState().get() == null) {
                model.getConfirmationState().set(Model.ConfirmationState.REQUEST_STARTED);
                requestTx();
            }

            if (model.getConfirmationState().get() == Model.ConfirmationState.CONFIRMED) {
                model.getButtonText().set(Res.get("bisqEasy.tradeState.info.phase3b.button.next"));
            } else {
                model.getButtonText().set("SKIP (just for dev)");
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
            String url = provider.getBaseUrl() + "/" + provider.getTxPath() + model.getTxId();
            Browser.open(url);
        }

        void onSkipWaitForConfirmation() {
            muSigTradeService.skipWaitForConfirmation(model.getTrade());
        }

        private void requestTx() {
            String paymentProof = model.getTxId();
            if (CONFIRMED_TX_CACHE.containsKey(paymentProof)) {
                Tx tx = CONFIRMED_TX_CACHE.get(paymentProof);
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
        protected String txId;
        @Setter
        private long txOutputValueForAddress;
        @Setter
        private String role;

        private final StringProperty confirmationInfo = new SimpleStringProperty();
        private final StringProperty buttonText = new SimpleStringProperty();
        private final BooleanProperty isConfirmed = new SimpleBooleanProperty();
        private final ObjectProperty<ConfirmationState> confirmationState = new SimpleObjectProperty<>();

        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            super(trade, channel);
            role = this instanceof BuyerState1WaitForDepositConfirmation.Model ? "buyer" : "seller";
        }
    }

    public static abstract class View<M extends Model, C extends Controller<?, ?>> extends BaseState.View<M, C> {
        private final Button button;
        private final MaterialTextField txId;
        private final WaitingAnimation waitingAnimation;
        private Subscription confirmationStatePin;

        protected View(M model, C controller) {
            super(model, controller);

            String role = model.getRole();
            WrappingText headline = FormUtils.getHeadline(Res.get("muSig.tradeState.info.phase1.headline"));
            WrappingText info = FormUtils.getInfo(Res.get("muSig.tradeState.info.phase1.info."+role, model.getQuoteCode()));
            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            txId = FormUtils.getTextField(Res.get("muSig.tradeState.info.phase1.txId"), "", false);
            txId.setHelpText(Res.get("bisqEasy.tradeState.info.phase3b.balance.help.explorerLookup"));
            txId.setPromptText(Res.get("muSig.tradeState.info.phase1.txId.prompt"));
            txId.setIcon(AwesomeIcon.EXTERNAL_LINK);
            txId.setIconTooltip(Res.get("muSig.tradeState.info.phase1.txId.tooltip"));
            txId.setValidator(new BitcoinTransactionValidator());
            txId.filterMouseEventOnNonEditableText();

            button = new Button();
            VBox.setMargin(txId, new Insets(10, 0, 5, 0));
          //  VBox.setMargin(button, new Insets(5, 0, 5, 0));

            root.getChildren().addAll(waitingInfo, txId, button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            txId.setText(model.getTxId());
            txId.validate();

            button.defaultButtonProperty().bind(model.getIsConfirmed());
            button.textProperty().bind(model.getButtonText());
            txId.helpProperty().bind(model.getConfirmationInfo());

            confirmationStatePin = EasyBind.subscribe(model.getConfirmationState(), confirmationState -> {
                if (confirmationState != null) {
                    txId.getHelpLabel().getStyleClass().remove("tx-lookup-in-mempool");
                    txId.getHelpLabel().getStyleClass().remove("tx-lookup-confirmed");
                    txId.getHelpLabel().getStyleClass().remove("tx-lookup-failed");
                    switch (confirmationState) {
                        case REQUEST_STARTED:
                            break;
                        case IN_MEMPOOL:
                            txId.getHelpLabel().getStyleClass().add("tx-lookup-in-mempool");
                            break;
                        case CONFIRMED:
                            txId.getHelpLabel().getStyleClass().add("tx-lookup-confirmed");
                            break;
                        case FAILED:
                            txId.getHelpLabel().getStyleClass().add("tx-lookup-failed");
                            break;
                    }
                }
            });

            button.setOnAction(e -> controller.onSkipWaitForConfirmation());
            txId.getIconButton().setOnAction(e -> controller.openExplorer());

            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            button.defaultButtonProperty().unbind();
            button.textProperty().unbind();
            txId.helpProperty().unbind();
            confirmationStatePin.unsubscribe();

            button.setOnAction(null);
            txId.getIconButton().setOnAction(null);

            txId.resetValidation();

            waitingAnimation.stop();
        }
    }
}
