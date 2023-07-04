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
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.oracle.service.market_price.MarketPriceService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.identity.UserIdentityService;
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
        private final MarketPriceService marketPriceService;

        protected Controller(DefaultApplicationService applicationService, BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
            chatService = applicationService.getChatService();
            bisqEasyTradeService = applicationService.getTradeService().getBisqEasyTradeService();
            accountService = applicationService.getAccountService();
            userIdentityService = applicationService.getUserService().getUserIdentityService();
            marketPriceService = applicationService.getOracleService().getMarketPriceService();

            model = createModel(bisqEasyTrade, channel);
            view = createView();
        }

        protected abstract V createView();

        protected abstract M createModel(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel);

        @Override
        public void onActivate() {
            BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
            model.setQuoteCode(bisqEasyOffer.getMarket().getQuoteCurrencyCode());

            AmountSpec amountSpec = bisqEasyOffer.getAmountSpec();
            String baseAmountString = OfferAmountFormatter.formatBaseSideMaxOrFixedAmount(marketPriceService, amountSpec,
                    bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
            model.setFormattedBaseAmount(baseAmountString);

            String quoteAmountString = OfferAmountFormatter.formatQuoteSideMaxOrFixedAmount(marketPriceService, amountSpec,
                    bisqEasyOffer.getPriceSpec(), bisqEasyOffer.getMarket(), true);
            model.setFormattedQuoteAmount(quoteAmountString);
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

        protected void sendChatBotMessage(String message) {
            chatService.getBisqEasyPrivateTradeChatChannelService().sendTextMessage(message,
                    Optional.empty(),
                    model.getChannel());
        }
    }

    @Getter
    protected static class Model implements bisq.desktop.common.view.Model {
        protected final BisqEasyTrade bisqEasyTrade;
        protected final BisqEasyPrivateTradeChatChannel channel;
        @Setter
        protected String quoteCode;
        @Setter
        protected String formattedBaseAmount;
        @Setter
        protected String formattedQuoteAmount;

        protected Model(BisqEasyTrade bisqEasyTrade, BisqEasyPrivateTradeChatChannel channel) {
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
    }
}