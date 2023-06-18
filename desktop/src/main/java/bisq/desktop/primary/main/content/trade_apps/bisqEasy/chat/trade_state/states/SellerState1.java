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

package bisq.desktop.primary.main.content.trade_apps.bisqEasy.chat.trade_state.states;

import bisq.account.AccountService;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.application.DefaultApplicationService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqText;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.trade.Trade;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SellerState1 {
    private final Controller controller;

    public SellerState1(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
        controller.setSelectedChannel(channel);
    }

    public View getView() {
        return controller.getView();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;


        @Getter
        private final View view;
        private final BisqEasyTradeService bisqEasyTradeService;
        private final ChatService chatService;
        private final AccountService accountService;

        private Controller(DefaultApplicationService applicationService) {
            chatService = applicationService.getChatService();
            bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();
            accountService = applicationService.getAccountService();
            model = new Model();
            view = new View(model, this);
        }

        public void setSelectedChannel(BisqEasyPrivateTradeChatChannel channel) {
            model.setSelectedChannel(channel);

            String tradeId = Trade.createId(channel.getBisqEasyOffer().getId(), channel.getPeer().getId());
            bisqEasyTradeService.findTrade(tradeId)
                    .ifPresent(bisqEasyTradeModel -> {
                        log.error("bisqEasyTradeModel={}", bisqEasyTradeModel);
                        model.setBisqEasyTradeModel(bisqEasyTradeModel);
                    });
        }


        @Override
        public void onActivate() {
            findUsersAccountData().ifPresent(accountData -> model.getPaymentAccountData().set(accountData));
        }

        @Override
        public void onDeactivate() {
        }

        private void onSendPaymentData() {
            String message = Res.get("bisqEasy.tradeState.info.seller.phase1.chatBotMessage", model.getPaymentAccountData().get());
            chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                    Optional.empty(),
                    model.getSelectedChannel());
            try {
                bisqEasyTradeService.sellerSendsPaymentAccount(model.getBisqEasyTradeModel(), model.getPaymentAccountData().get());
            } catch (TradeException e) {
                new Popup().error(e).show();
            }
        }

        private Optional<String> findUsersAccountData() {
            return Optional.ofNullable(accountService.getSelectedAccount()).stream()
                    .filter(account -> account instanceof UserDefinedFiatAccount)
                    .map(account -> (UserDefinedFiatAccount) account)
                    .map(account -> account.getAccountPayload().getAccountData())
                    .findFirst();
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        @Setter
        private BisqEasyPrivateTradeChatChannel selectedChannel;
        @Setter
        private BisqEasyTrade bisqEasyTradeModel;
        private final StringProperty paymentAccountData = new SimpleStringProperty();
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Button button;
        private final MaterialTextArea paymentAccountData;

        private View(Model model, Controller controller) {
            super(new VBox(10), model, controller);

            BisqText infoHeadline = new BisqText(Res.get("bisqEasy.tradeState.info.seller.phase1.headline"));
            infoHeadline.getStyleClass().add("bisq-easy-trade-state-info-headline");

            paymentAccountData = FormUtils.addTextArea(Res.get("bisqEasy.tradeState.info.seller.phase1.accountData"), model.getPaymentAccountData().get(), true);
            paymentAccountData.setPromptText(Res.get("bisqEasy.tradeState.info.seller.phase1.accountData.prompt"));

            button = new Button(Res.get("bisqEasy.tradeState.info.seller.phase1.buttonText"));
            button.setDefaultButton(true);

            Label helpLabel = FormUtils.getHelpLabel(Res.get("bisqEasy.tradeState.info.seller.phase1.note"));

            VBox.setMargin(button, new Insets(5, 0, 0, 0));
            root.getChildren().addAll(Layout.hLine(),
                    infoHeadline,
                    paymentAccountData,
                    button,
                    Spacer.fillVBox(),
                    helpLabel);
        }

        @Override
        protected void onViewAttached() {
            paymentAccountData.textProperty().bindBidirectional(model.getPaymentAccountData());
            button.disableProperty().bind(paymentAccountData.textProperty().isEmpty());
            button.setOnAction(e -> controller.onSendPaymentData());
        }

        @Override
        protected void onViewDetached() {
            button.disableProperty().unbind();
        }
    }
}