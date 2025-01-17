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

import bisq.bisq_easy.BisqEasyService;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.components.controls.validator.SettableErrorValidator;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.support.mediation.MediationRequestService;
import bisq.support.moderator.ModeratorService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuyerState2a extends BaseState {
    private final Controller controller;

    public BuyerState2a(ServiceProvider serviceProvider,
                        BisqEasyTrade bisqEasyTrade,
                        BisqEasyOpenTradeChannel channel) {
        controller = new Controller(serviceProvider, bisqEasyTrade, channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller extends BaseState.Controller<Model, View> {
        private final BisqEasyService bisqEasyService;
        private final MediationRequestService mediationRequestService;
        private final ModeratorService moderatorService;

        private Controller(ServiceProvider serviceProvider,
                           BisqEasyTrade bisqEasyTrade,
                           BisqEasyOpenTradeChannel channel) {
            super(serviceProvider, bisqEasyTrade, channel);

            bisqEasyService = serviceProvider.getBisqEasyService();
            mediationRequestService = serviceProvider.getSupportService().getMediationRequestService();
            moderatorService = serviceProvider.getSupportService().getModeratorService();
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
            BisqEasyTrade bisqEasyTrade = model.getBisqEasyTrade();
            String sellersAccountData = bisqEasyTrade.getPaymentAccountData().get();
            if (bisqEasyService.isAccountDataBanned(sellersAccountData)) {
                model.getConfirmFiatSentButtonDisabled().set(true);
                model.getAccountDataBannedValidator().setIsInvalid(true);

                UserProfile peer = model.getChannel().getPeer();
                String peerUserName = peer.getUserName();

                // Report to moderator
                String message = "Account data of " + peerUserName + " is banned: " + sellersAccountData;
                moderatorService.reportUserProfile(peer, message);

                // We reject the trade to avoid the banned user can continue
                bisqEasyTradeService.cancelTrade(bisqEasyTrade);

                new Popup().warning(Res.get("bisqEasy.tradeState.info.buyer.phase2a.accountDataBanned.popup.warning")).show();
            } else {
                model.getConfirmFiatSentButtonDisabled().set(false);
                model.getAccountDataBannedValidator().setIsInvalid(false);
            }
        }

        @Override
        public void onDeactivate() {
            super.onDeactivate();
        }

        private void onConfirmFiatSent() {
            sendTradeLogMessage(Res.encode("bisqEasy.tradeState.info.buyer.phase2a.tradeLogMessage",
                    model.getChannel().getMyUserIdentity().getUserName(), model.getQuoteCode()));
            bisqEasyTradeService.buyerConfirmFiatSent(model.getBisqEasyTrade());
        }
    }

    @Getter
    private static class Model extends BaseState.Model {
        private final BooleanProperty confirmFiatSentButtonDisabled = new SimpleBooleanProperty();
        private final SettableErrorValidator accountDataBannedValidator = new SettableErrorValidator(Res.get("bisqEasy.tradeState.info.buyer.phase2a.accountDataBannedError"));

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            super(bisqEasyTrade, channel);
        }
    }

    public static class View extends BaseState.View<Model, Controller> {
        private final Button confirmFiatSentButton;
        private final MaterialTextArea account;
        private final MaterialTextField quoteAmount;
        private final WrappingText headline;

        private View(Model model, Controller controller) {
            super(model, controller);

            headline = FormUtils.getHeadline();

            quoteAmount = FormUtils.getTextField(Res.get("bisqEasy.tradeState.info.buyer.phase2a.quoteAmount"), "", false);
            account = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.buyer.phase2a.sellersAccount"), "", false);
            account.setHelpText(Res.get("bisqEasy.tradeState.info.buyer.phase2a.reasonForPaymentInfo"));
            account.setValidator(model.getAccountDataBannedValidator());

            confirmFiatSentButton = new Button();
            confirmFiatSentButton.setDefaultButton(true);
            VBox.setMargin(confirmFiatSentButton, new Insets(0, 0, 5, 0));

            root.getChildren().addAll(
                    headline,
                    quoteAmount,
                    account,
                    confirmFiatSentButton);
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            headline.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2a.headline", model.getFormattedQuoteAmount()));
            quoteAmount.setText(model.getFormattedQuoteAmount());
            quoteAmount.getIconButton().setOnAction(e -> ClipboardUtil.copyToClipboard(model.getQuoteAmount()));
            account.setText(model.getBisqEasyTrade().getPaymentAccountData().get());
            account.validate();
            confirmFiatSentButton.setText(Res.get("bisqEasy.tradeState.info.buyer.phase2a.confirmFiatSent", model.getFormattedQuoteAmount()));
            confirmFiatSentButton.setOnAction(e -> controller.onConfirmFiatSent());
            confirmFiatSentButton.disableProperty().bind(model.getConfirmFiatSentButtonDisabled());
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            confirmFiatSentButton.disableProperty().unbind();
            confirmFiatSentButton.setOnAction(null);
            quoteAmount.getIconButton().setOnAction(null);
        }
    }
}
