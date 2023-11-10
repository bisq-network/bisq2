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

package bisq.desktop.main.content.dashboard;

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.presentation.formatters.PriceFormatter;
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
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private Pin selectedMarketPin, marketPricePin, getNumUserProfilesPin;
    private boolean allowUpdateOffersOnline;
    private Pin isNotificationVisiblePin;

    public DashboardController(ServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();

        model = new DashboardModel();
        view = new DashboardView(model, this);
    }

    @Override
    public void onActivate() {
        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> updateMarketPrice());
        marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(this::updateMarketPrice);

        getNumUserProfilesPin = userProfileService.getNumUserProfiles().addObserver(numUserProfiles ->
                UIThread.run(() -> model.getActiveUsers().set(String.valueOf(userProfileService.getUserProfiles().size()))));

        // We listen on all channels, also hidden ones and use a weak reference listener
        bisqEasyOfferbookChannelService.getChannels().forEach(publicTradeChannel ->
                publicTradeChannel.getChatMessages().addObserver(new WeakReference<Runnable>(this::updateOffersOnline).get()));

        // We trigger a call of updateOffersOnline for each channel when registering our observer. But we only want one call, 
        // so we block execution of the code inside updateOffersOnline to only call it once.
        allowUpdateOffersOnline = true;
        updateOffersOnline();

        isNotificationVisiblePin = FxBindings.bind(model.getIsNotificationVisible())
                .to(bisqEasyNotificationsService.getIsNotificationPanelVisible());
    }

    @Override
    public void onDeactivate() {
        selectedMarketPin.unbind();
        marketPricePin.unbind();
        getNumUserProfilesPin.unbind();
        isNotificationVisiblePin.unbind();
    }

    public void onLearn() {
        Navigation.navigateTo(NavigationTarget.ACADEMY);
    }

    public void onOpenTradeOverview() {
        Navigation.navigateTo(NavigationTarget.TRADE_PROTOCOLS);
    }

    public void onOpenBisqEasy() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY);
    }

    private void updateMarketPrice() {
        Market selectedMarket = marketPriceService.getSelectedMarket().get();
        if (selectedMarket != null) {
            marketPriceService.findMarketPrice(selectedMarket)
                    .ifPresent(marketPrice -> {
                        UIThread.run(() -> {
                            model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true));
                            model.getMarketCode().set(marketPrice.getMarket().getMarketCodes());
                        });
                    });
        }
    }

    private void updateOffersOnline() {
        if (allowUpdateOffersOnline) {
            UIThread.run(() ->
                    model.getOffersOnline().set(String.valueOf(bisqEasyOfferbookChannelService.getChannels().stream().flatMap(channel -> channel.getChatMessages().stream())
                            .filter(BisqEasyOfferbookMessage::hasBisqEasyOffer)
                            .count())));
        }
    }

}
