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

import bisq.account.AccountService;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.chat.ChatService;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.chat.priv.LeavePrivateChatManager;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.controls.WrappingText;
import bisq.desktop.main.content.bisq_easy.components.WaitingAnimation;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.presentation.formatters.AmountFormatter;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
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
        protected final MuSigTradeService muSigTradeService;
        protected final ChatService chatService;
        protected final AccountService accountService;
        protected final UserIdentityService userIdentityService;
        protected final MuSigOpenTradeChannelService channelService;
        protected final LeavePrivateChatManager leavePrivateChatManager;

        protected Controller(ServiceProvider serviceProvider,
                             MuSigTrade trade,
                             MuSigOpenTradeChannel channel) {
            chatService = serviceProvider.getChatService();
            muSigTradeService = serviceProvider.getTradeService().getMuSigTradeService();
            accountService = serviceProvider.getAccountService();
            userIdentityService = serviceProvider.getUserService().getUserIdentityService();
            channelService = serviceProvider.getChatService().getMuSigOpenTradeChannelService();
            leavePrivateChatManager = chatService.getLeavePrivateChatManager();

            model = createModel(trade, channel);
            view = createView();
        }

        protected abstract V createView();

        protected abstract M createModel(MuSigTrade trade, MuSigOpenTradeChannel channel);

        @Override
        public void onActivate() {
            MuSigOffer muSigOffer = model.getMuSigOffer();
            long baseSideAmount = model.getTrade().getContract().getBaseSideAmount();
            long quoteSideAmount = model.getTrade().getContract().getQuoteSideAmount();
            model.setBaseAmount(AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(baseSideAmount)));
            model.setFormattedBaseAmount(AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(baseSideAmount)));
            model.setQuoteAmount(AmountFormatter.formatQuoteAmount(Fiat.from(quoteSideAmount, muSigOffer.getMarket().getQuoteCurrencyCode())));
            model.setFormattedQuoteAmount(AmountFormatter.formatQuoteAmountWithCode(Fiat.from(quoteSideAmount, muSigOffer.getMarket().getQuoteCurrencyCode())));
        }

        @Override
        public void onDeactivate() {
        }

        protected Optional<String> findUsersAccountData() {
            return accountService
                    .getSelectedAccount().stream()
                    .filter(UserDefinedFiatAccount.class::isInstance)
                    .map(UserDefinedFiatAccount.class::cast)
                    .map(account -> account.getAccountPayload().getAccountData())
                    .findFirst();
        }

        protected void sendTradeLogMessage(String encoded) {
            chatService.getMuSigOpenTradeChannelService().sendTradeLogMessage(encoded, model.getChannel());
        }
    }

    @Getter
    protected static class Model implements bisq.desktop.common.view.Model {
        protected final MuSigTrade trade;
        protected final MuSigOpenTradeChannel channel;
        protected final String quoteCode;
        @Setter
        protected String baseAmount;
        @Setter
        protected String formattedBaseAmount;
        @Setter
        protected String quoteAmount;
        @Setter
        protected String formattedQuoteAmount;

        protected Model(MuSigTrade trade, MuSigOpenTradeChannel channel) {
            this.trade = trade;
            this.channel = channel;

            quoteCode=  trade.getContract().getOffer().getMarket().getQuoteCurrencyCode();
        }

        protected MuSigOffer getMuSigOffer() {
            return trade.getOffer();
        }
    }

    public static class View<M extends Model, C extends Controller<?, ?>>
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
            return new HBox(20, animation, text);
        }
    }
}
