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

package bisq.desktop.main.content.bisq_easy.trade_wizard.direction_and_market;

import bisq.bisq_easy.BisqEasyTradeAmountLimits;
import bisq.bisq_easy.NavigationTarget;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookSelectionService;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.KeyHandlerUtil;
import bisq.desktop.common.view.Controller;
import bisq.offer.Direction;
import bisq.user.identity.UserIdentityService;
import bisq.user.reputation.ReputationService;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class TradeWizardDirectionAndMarketController implements Controller {
    private final TradeWizardDirectionAndMarketModel model;
    @Getter
    private final TradeWizardDirectionAndMarketView view;
    private final Runnable onNextHandler;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;
    private final ReputationService reputationService;
    private final Consumer<NavigationTarget> closeAndNavigateToHandler;
    private final UserIdentityService userIdentityService;
    private final MarketPriceService marketPriceService;
    private final BisqEasyOfferbookSelectionService bisqEasyOfferbookSelectionService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private Subscription searchTextPin;

    public TradeWizardDirectionAndMarketController(ServiceProvider serviceProvider,
                                                   Runnable onNextHandler,
                                                   Consumer<Boolean> navigationButtonsVisibleHandler,
                                                   Consumer<NavigationTarget> closeAndNavigateToHandler) {
        this.onNextHandler = onNextHandler;
        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        reputationService = serviceProvider.getUserService().getReputationService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyOfferbookSelectionService = serviceProvider.getChatService().getBisqEasyOfferbookChannelSelectionService();
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        this.closeAndNavigateToHandler = closeAndNavigateToHandler;

        model = new TradeWizardDirectionAndMarketModel();
        view = new TradeWizardDirectionAndMarketView(model, this);
        setDirection(Direction.BUY);
        setIsAllowedToCreateOffer();
        applyShowReputationInfo();
    }

    public ReadOnlyObjectProperty<Direction> getDirection() {
        return model.getDirection();
    }

    public ReadOnlyObjectProperty<Market> getMarket() {
        return model.getSelectedMarket();
    }

    public void reset() {
        model.reset();
    }

    @Override
    public void onActivate() {
        setDirection(Direction.BUY);
        setIsAllowedToCreateOffer();
        applyShowReputationInfo();

        model.getSearchText().set("");
        if (model.getSelectedMarket().get() == null) {
            // Use selected public channel or if private channel is selected we use any of the public channels for
            // setting the default market
            Optional.ofNullable(bisqEasyOfferbookSelectionService.getSelectedChannel().get())
                    .filter(channel -> channel instanceof BisqEasyOfferbookChannel)
                    .map(channel -> (BisqEasyOfferbookChannel) channel)
                    .map(BisqEasyOfferbookChannel::getMarket)
                    .ifPresent(market -> model.getSelectedMarket().set(market));
        }

        model.getListItems().setAll(MarketRepository.getAllFiatMarkets().stream()
                .filter(market -> marketPriceService.getMarketPriceByCurrencyMap().containsKey(market))
                .map(market -> {
                    Set<BisqEasyOfferbookMessage> allMessages = bisqEasyOfferbookChannelService.getChannels().stream()
                            .filter(channel -> channel.getMarket().equals(market))
                            .flatMap(channel -> channel.getChatMessages().stream())
                            .collect(Collectors.toSet());
                    int numOffersInChannel = (int) allMessages.stream()
                            .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                            .distinct()
                            .count();
                    int numUsersInChannel = (int) allMessages.stream()
                            .map(ChatMessage::getAuthorUserProfileId)
                            .distinct()
                            .count();
                    TradeWizardDirectionAndMarketView.ListItem item = new TradeWizardDirectionAndMarketView.ListItem(market, numOffersInChannel, numUsersInChannel);
                    if (market.equals(model.getSelectedMarket().get())) {
                        model.getSelectedMarketListItem().set(item);
                    }
                    return item;
                })
                .collect(Collectors.toList()));

        searchTextPin = EasyBind.subscribe(model.getSearchText(), searchText -> {
            if (searchText == null || searchText.isEmpty()) {
                model.getFilteredList().setPredicate(item -> true);
            } else {
                String search = searchText.toLowerCase();
                model.getFilteredList().setPredicate(item ->
                        item != null &&
                                (item.getQuoteCurrencyDisplayName().toLowerCase().contains(search) ||
                                        item.getMarket().getQuoteCurrencyDisplayName().toLowerCase().contains(search))
                );
            }
        });
    }

    @Override
    public void onDeactivate() {
        view.getRoot().setOnKeyPressed(null);
        searchTextPin.unsubscribe();
    }

    public boolean validate() {
        if (model.getDirection().get() == Direction.SELL && !model.isAllowedToCreateSellOffer()) {
            showReputationInfoOverlay();
            return false;
        }

        return true;
    }

    void onSelectDirection(Direction direction) {
        setDirection(direction);
        applyShowReputationInfo();
        if (!model.getShowReputationInfo().get()) {
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

    void onMarketListItemClicked(TradeWizardDirectionAndMarketView.ListItem item) {
        if (item == null) {
            return;
        }
        if (item.equals(model.getSelectedMarketListItem().get())) {
            onNextHandler.run();
        }
        model.getSelectedMarketListItem().set(item);
        model.getSelectedMarket().set(item.getMarket());
        bisqEasyOfferbookChannelService.findChannel(item.getMarket())
                .ifPresent(bisqEasyOfferbookSelectionService::selectChannel);
    }

    private void setDirection(Direction direction) {
        model.getDirection().set(direction);
    }

    private void setIsAllowedToCreateOffer() {
        model.setAllowedToCreateSellOffer(BisqEasyTradeAmountLimits.isAllowedToCreateSellOffer(
                reputationService, userIdentityService.getSelectedUserIdentity().getUserProfile()));
    }

    private void applyShowReputationInfo() {
        if (model.getDirection().get() == Direction.BUY) {
            model.getShowReputationInfo().set(false);
            navigationButtonsVisibleHandler.accept(true);
            return;
        }

        // Sell offer
        if (!model.isAllowedToCreateSellOffer()) {
            showReputationInfoOverlay();
        } else {
            view.getRoot().setOnKeyPressed(null);
        }
    }

    private void showReputationInfoOverlay() {
        navigationButtonsVisibleHandler.accept(false);
        model.getShowReputationInfo().set(true);
        view.getRoot().setOnKeyPressed(keyEvent -> {
            KeyHandlerUtil.handleEnterKeyEvent(keyEvent, () -> {
            });
            KeyHandlerUtil.handleEscapeKeyEvent(keyEvent, this::onCloseReputationInfo);
        });
    }
}
