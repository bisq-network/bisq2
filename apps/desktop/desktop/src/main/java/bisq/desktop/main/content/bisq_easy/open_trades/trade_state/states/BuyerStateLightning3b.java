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

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.LightningPreImageValidator;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.desktop.main.content.bisq_easy.components.WaitingState;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BuyerStateLightning3b extends BaseState {
    private final Controller controller;

    public BuyerStateLightning3b(ServiceProvider serviceProvider,
                                 BisqEasyTrade bisqEasyTrade,
                                 BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider,
                           BisqEasyTrade bisqEasyTrade,
                           BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
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

            model.setPaymentProof(Optional.ofNullable(model.getTrade().getPaymentProof().get()));
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onButtonClicked() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln", model.getChannel().getMyUserIdentity().getUserName()));
            bisqEasyTradeService.btcConfirmed(model.getTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        @Setter
        protected Optional<String> paymentProof = Optional.empty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button button;
        private final MaterialTextField paymentProof;
        private final WaitingAnimation waitingAnimation;

        private View(Model model, Controller controller) {
            super(model, controller);

            waitingAnimation = new WaitingAnimation(WaitingState.BITCOIN_CONFIRMATION);
            WrappingText headline = FormUtils.getHeadline(Res.get("bisqEasy.tradeState.info.buyer.phase3b.headline.ln"));
            WrappingText info = FormUtils.getInfo(Res.get("bisqEasy.tradeState.info.buyer.phase3b.info.ln"));
            HBox waitingInfo = createWaitingInfo(waitingAnimation, headline, info);

            paymentProof = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.phase3b.lightning.preimage"), "", false);
            paymentProof.setValidator(new LightningPreImageValidator());

            button = new Button(Res.get("bisqEasy.tradeState.info.buyer.phase3b.confirmButton.ln"));
            button.setDefaultButton(true);

            VBox.setMargin(paymentProof, new Insets(15, 0, 5, 0));
            root.getChildren().addAll(waitingInfo, paymentProof, button);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();
            boolean isPaymentProofPresent = model.getPaymentProof().isPresent();
            paymentProof.setVisible(isPaymentProofPresent);
            paymentProof.setManaged(isPaymentProofPresent);
            if (isPaymentProofPresent) {
                paymentProof.setText(model.getPaymentProof().get());
            } else {
                VBox.setMargin(button, new Insets(25, 0, 5, 0));
            }
            paymentProof.validate();
            button.setOnAction(e -> controller.onButtonClicked());
            waitingAnimation.play();
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            paymentProof.resetValidation();
            button.setOnAction(null);
            waitingAnimation.stop();
        }
    }
}
