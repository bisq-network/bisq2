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

package bisq.desktop.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.WaitingAnimation;
import bisq.i18n.Res;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BuyerState3 extends BaseState {
    private final Controller controller;

    public BuyerState3(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);
        }

        @Override
        protected Model createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            return new Model(bisqEasyTrade, channel);
        }

        @Override
        protected View createView() {
            return new View(model, this);
        }

        @Override
        public void onActivate() {
            super.onActivate();
            model.getFiatReceiptConfirmed().set(model.getBisqEasyTrade().getState() == BisqEasyTradeState.BUYER_RECEIVED_SELLERS_FIAT_RECEIPT_CONFIRMATION);
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final BooleanProperty fiatReceiptConfirmed = new SimpleBooleanProperty();

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {

        private final Label infoLabel;
        private final WaitingAnimation waitingAnimation;
        private final CheckBox fiatReceiptConfirmedCheckBox;
        private final BisqText infoHeadline;
        private Subscription fiatReceiptConfirmedPin;

        private View(Model model, Controller controller) {
            super(model, controller);

            infoHeadline = new BisqText();
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");
            infoLabel = FormUtils.getLabel("");

            fiatReceiptConfirmedCheckBox = new CheckBox();
            fiatReceiptConfirmedCheckBox.setMouseTransparent(true);
            fiatReceiptConfirmedCheckBox.setSelected(true);

            waitingAnimation = new WaitingAnimation();
            VBox.setMargin(waitingAnimation, new Insets(30, 0, 0, 20));
            root.getChildren().addAll(
                    infoHeadline,
                    infoLabel,
                    fiatReceiptConfirmedCheckBox,
                    waitingAnimation);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            waitingAnimation.play();

            fiatReceiptConfirmedPin = EasyBind.subscribe(model.getFiatReceiptConfirmed(), fiatPaymentConfirmed -> {
                fiatReceiptConfirmedCheckBox.setVisible(fiatPaymentConfirmed);
                fiatReceiptConfirmedCheckBox.setManaged(fiatPaymentConfirmed);
                if (fiatPaymentConfirmed) {
                    infoHeadline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3b.headline"));
                    infoLabel.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3b.info"));
                    fiatReceiptConfirmedCheckBox.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3.fiatReceiptConfirmedCheckBox", model.getFormattedQuoteAmount()));
                } else {
                    infoHeadline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3a.headline", model.getFormattedQuoteAmount()));
                    infoLabel.setText(Res.get("bisqEasy.tradeState.info.buyer.phase3a.info", model.getQuoteCode()));
                }
            });
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();
            waitingAnimation.stop();

            fiatReceiptConfirmedPin.unsubscribe();
        }
    }
}