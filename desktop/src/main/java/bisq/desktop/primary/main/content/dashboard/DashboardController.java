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

package bisq.desktop.primary.main.content.dashboard;

import bisq.application.DefaultApplicationService;
import bisq.chat.trade.pub.PublicTradeChannelService;
import bisq.chat.trade.pub.PublicTradeChatMessage;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
public class DashboardController implements Controller {
    @Getter
    private final DashboardView view;
    private final MarketPriceService marketPriceService;
    private final DashboardModel model;
    private final UserProfileService userProfileService;
    private final PublicTradeChannelService publicTradeChannelService;
    private Pin selectedMarketPin, marketPriceUpdateFlagPin, userProfileUpdateFlagPin;
    private boolean allowUpdateOffersOnline;

    public DashboardController(DefaultApplicationService applicationService) {
        marketPriceService = applicationService.getOracleService().getMarketPriceService();
        userProfileService = applicationService.getUserService().getUserProfileService();
        publicTradeChannelService = applicationService.getChatService().getPublicTradeChannelService();

        model = new DashboardModel();
        view = new DashboardView(model, this);
    }

    @Override
    public void onActivate() {
        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> updateMarketPrice());
        marketPriceUpdateFlagPin = marketPriceService.getMarketPriceUpdateFlag().addObserver(__ -> updateMarketPrice());

        userProfileUpdateFlagPin = userProfileService.getUserProfilesUpdateFlag().addObserver(__ ->
                UIThread.run(() ->
                        model.getActiveUsers().set(String.valueOf(userProfileService.getUserProfiles().size()))));

        // We listen on all channels, also hidden ones and use a weak reference listener
        publicTradeChannelService.getChannels().forEach(publicTradeChannel ->
                publicTradeChannel.getChatMessages().addChangedListener(new WeakReference<Runnable>(this::updateOffersOnline).get()));

        // We trigger a call of updateOffersOnline for each channel when registering our observer. But we only want one call, 
        // so we block execution of the code inside updateOffersOnline to only call it once.
        allowUpdateOffersOnline = true;
        updateOffersOnline();
    }

    @Override
    public void onDeactivate() {
        selectedMarketPin.unbind();
        marketPriceUpdateFlagPin.unbind();
        userProfileUpdateFlagPin.unbind();
    }

    public void onLearn() {
        Navigation.navigateTo(NavigationTarget.ACADEMY_OVERVIEW);
    }

    public void onOpenTradeOverview() {
        Navigation.navigateTo(NavigationTarget.TRADE_OVERVIEW);
    }

    public void onOpenBisqEasy() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY);
    }

    private void updateMarketPrice() {
        Market selectedMarket = marketPriceService.getSelectedMarket().get();
        if (selectedMarket != null) {
            UIThread.run(() -> {
                MarketPrice marketPrice = marketPriceService.getMarketPriceByCurrencyMap().get(selectedMarket);
                model.getMarketPrice().set(QuoteFormatter.format(marketPrice.getQuote(), true));
                model.getMarketCode().set(marketPrice.getMarket().getMarketCodes());
            });
        }
    }

    private void updateOffersOnline() {
        if (allowUpdateOffersOnline) {
            UIThread.run(() ->
                    model.getOffersOnline().set(String.valueOf(publicTradeChannelService.getChannels().stream().flatMap(channel -> channel.getChatMessages().stream())
                            .filter(PublicTradeChatMessage::hasTradeChatOffer)
                            .count())));
        }
    }

}
