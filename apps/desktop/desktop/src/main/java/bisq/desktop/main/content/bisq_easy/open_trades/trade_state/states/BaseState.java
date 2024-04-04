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

import bisq.account.AccountService;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeSelectionService;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class BaseState {
    protected static abstract class Controller<M extends Model, V extends View<?, ?>> implements bisq.desktop.common.view.Controller {
        protected final M model;
        @Getter
        protected final V view;
        protected final BisqEasyTradeService bisqEasyTradeService;
        protected final ChatService chatService;
        protected final AccountService accountService;
        protected final UserIdentityService userIdentityService;
        protected final BisqEasyOpenTradeChannelService channelService;
        protected final BisqEasyOpenTradeSelectionService selectionService;

        protected Controller(ServiceProvider serviceProvider, BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            chatService = serviceProvider.getChatService();
            bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
            accountService = serviceProvider.getAccountService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            channelService = serviceProvider.getChatService().getBisqEasyOpenTradeChannelService();
            selectionService = serviceProvider.getChatService().getBisqEasyOpenTradesSelectionService();

            model = createModel(bisqEasyTrade, channel);
            view = createView();
        }

        protected abstract V createView();

        protected abstract M createModel(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel);

        @Override
        public void onActivate() {
            BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
            model.setQuoteCode(bisqEasyOffer.getMarket().getQuoteCurrencyCode());

            long baseSideAmount = model.getBisqEasyTrade().getContract().getBaseSideAmount();
            long quoteSideAmount = model.getBisqEasyTrade().getContract().getQuoteSideAmount();
            model.setBaseAmount(AmountFormatter.formatAmount(Coin.asBtcFromValue(baseSideAmount), false));
            model.setFormattedBaseAmount(AmountFormatter.formatAmountWithCode(Coin.asBtcFromValue(baseSideAmount), false));
            model.setQuoteAmount(AmountFormatter.formatAmount(Fiat.from(quoteSideAmount, bisqEasyOffer.getMarket().getQuoteCurrencyCode())));
            model.setFormattedQuoteAmount(AmountFormatter.formatAmountWithCode(Fiat.from(quoteSideAmount, bisqEasyOffer.getMarket().getQuoteCurrencyCode())));
        }

        @Override
        public void onDeactivate() {
        }

        protected Optional<String> findUsersAccountData() {
            return Optional.ofNullable(accountService.getSelectedAccount()).stream()
                    .filter(account -> account instanceof UserDefinedFiatAccount)
                    .map(account -> (UserDefinedFiatAccount) account)
                    .map(account -> account.getAccountPayload().getAccountData())
                    .findFirst();
        }

        protected void sendSystemMessage(String message) {
            chatService.getBisqEasyOpenTradeChannelService().sendSystemMessage(message, model.getChannel());
        }
    }

    @Getter
    protected static class Model implements bisq.desktop.common.view.Model {
        protected final BisqEasyTrade bisqEasyTrade;
        protected final BisqEasyOpenTradeChannel channel;
        @Setter
        protected String quoteCode;
        @Setter
        protected String baseAmount;
        @Setter
        protected String formattedBaseAmount;
        @Setter
        protected String quoteAmount;
        @Setter
        protected String formattedQuoteAmount;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyOpenTradeChannel channel) {
            this.bisqEasyTrade = bisqEasyTrade;
            this.channel = channel;
        }

        protected BisqEasyOffer getBisqEasyOffer() {
            return bisqEasyTrade.getOffer();
        }
    }

    public static class View<M extends BaseState.Model, C extends BaseState.Controller<?, ?>>
            extends bisq.desktop.common.view.View<VBox, M, C> {

        protected View(M model, C controller) {
            super(new VBox(10), model, controller);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }

        protected HBox createWaitingInfo(WaitingAnimation animation, WrappingText headline, WrappingText info) {
            animation.setAlignment(Pos.CENTER);
            VBox text = new VBox(headline, info);
            text.setAlignment(Pos.CENTER_LEFT);
            text.setSpacing(10);
            HBox waitingInfo = new HBox(animation, text);
            waitingInfo.setSpacing(20);
            return waitingInfo;
        }
    }
}
