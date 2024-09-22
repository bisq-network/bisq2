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

package bisq.desktop.main.content.bisq_easy.trade_wizard.direction;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.offer.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

import static bisq.bisq_easy.BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION;

@Slf4j
public class TradeWizardDirectionController implements Controller {
    private final TradeWizardDirectionModel model;
    @Getter
    private final TradeWizardDirectionView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final ReputationService reputationService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookSelectionService;

    public TradeWizardDirectionController(ServiceProvider serviceProvider,
                                          Runnable onNextHandler,
                                          Consumer<Boolean> navigationButtonsVisibleHandler,
                                          Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.onNextHandler = onNextHandler;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyOfferbookSelectionService = serviceProvider.getChatService().getBisqEasyOfferbookChannelSelectionService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        model = new TradeWizardDirectionModel();
        view = new TradeWizardDirectionView(model, this);
        setDirection(Direction.BUY);
        applyShowReputationInfo();
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        setDirection(Direction.BUY);
        applyShowReputationInfo();

        model.setFormattedAmountWithoutReputationNeeded(Optional.ofNullable(bisqEasyOfferbookSelectionService.getSelectedChannel().get())
                .filter(channel -> channel instanceof BisqEasyOfferbookChannel)
                .map(channel -> (BisqEasyOfferbookChannel) channel)
                .map(BisqEasyOfferbookChannel::getMarket)
                .flatMap(market -> BisqEasyTradeAmountLimits.usdToFiat(marketPriceService, market, MAX_USD_TRADE_AMOUNT_WITHOUT_REPUTATION))
                .map(amount -> amount.round(0))
                .map(AmountFormatter::formatAmountWithCode)
                .orElse("25 USD"));
    }

    @Override
    public void onDeactivate() {
        view.getRoot().setOnKeyPressed(null);
    }

    void onSelectDirection(Direction direction) {
        setDirection(direction);
        applyShowReputationInfo();
        if (direction == Direction.BUY && !model.getShowReputationInfo().get()) {
            onNextHandler.run();
        }
    }

    void onCloseReputationInfo() {
        setDirection(Direction.BUY);
        applyShowReputationInfo();
    }

    void onBuildReputation() {
        closeAndNavigateToHandler.accept(NavigationTarget.BUILD_REPUTATION);
    }

    void onTradeWithoutReputation() {
        navigationButtonsVisibleHandler.accept(true);
        onNextHandler.run();
    }

    private void setDirection(Direction direction) {
        model.getDirection().set(direction);
    }

    private void applyShowReputationInfo() {
        if (model.getDirection().get() == Direction.BUY) {
            model.getShowReputationInfo().set(false);
            navigationButtonsVisibleHandler.accept(true);
            return;
        }

        ReputationScore reputationScore = reputationService.getReputationScore(userIdentityService.getSelectedUserIdentity().getUserProfile());
        if (!reputationScore.hasReputation()) {
            navigationButtonsVisibleHandler.accept(false);
            model.getShowReputationInfo().set(true);
            view.getRoot().setOnKeyPressed(keyEvent -> {
                KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
                });
                KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseReputationInfo);
            });
        } else {
            view.getRoot().setOnKeyPressed(null);
        }
    }
}
