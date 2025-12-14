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
import bisq.bisq_easy.BisqEasyOfferbookMessageService;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.bonded_roles.security_manager.alert.AlertNotificationsService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService;
import bisq.common.market.Market;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.main.content.dashboard.bisq_easy.BisqEasyDashboardColumns;
import bisq.desktop.main.content.dashboard.musig.MusigDashboardColumns;
import bisq.desktop.main.content.dashboard.top_panel.DashboardTopPanel;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MuSigService;
import bisq.presentation.formatters.PriceFormatter;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class DashboardController implements Controller {
    @Getter
    private final DashboardView view;
    private final MarketPriceService marketPriceService;
    private final DashboardModel model;
    private final UserProfileService userProfileService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private final BisqEasyOfferbookMessageService bisqEasyOfferbookMessageService;
    private final AlertNotificationsService alertNotificationsService;
    private final MuSigService muSigService;
    private Pin selectedMarketPin, marketPricePin, getNumUserProfilesPin, isNotificationVisiblePin, unconsumedAlertsPin;
    private final Set<Pin> channelsPins = new HashSet<>();
    private boolean allowUpdateOffersOnline;
    private Pin muSigActivatedPin;

    public DashboardController(ServiceProvider serviceProvider) {
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        bisqEasyOfferbookChannelService = serviceProvider.getChatService().getBisqEasyOfferbookChannelService();
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();
        bisqEasyOfferbookMessageService = serviceProvider.getBisqEasyService().getBisqEasyOfferbookMessageService();
        alertNotificationsService = serviceProvider.getAlertNotificationsService();
        muSigService = serviceProvider.getMuSigService();

        DashboardTopPanel dashboardTopPanel = new DashboardTopPanel(serviceProvider);
        BisqEasyDashboardColumns bisqEasyDashBoardColumns = new BisqEasyDashboardColumns(serviceProvider);
        MusigDashboardColumns musigDashboardColumns = new MusigDashboardColumns(serviceProvider);
        model = new DashboardModel();
        view = new DashboardView(model, this,
                dashboardTopPanel.getViewRoot(),
                bisqEasyDashBoardColumns.getViewRoot(),
                musigDashboardColumns.getViewRoot());
    }

    @Override
    public void onActivate() {
        muSigActivatedPin = FxBindings.bind(model.getMuSigActivated()).to(muSigService.getMuSigActivated());

        selectedMarketPin = marketPriceService.getSelectedMarket().addObserver(selectedMarket -> updateMarketPrice());
        marketPricePin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(this::updateMarketPrice);

        getNumUserProfilesPin = userProfileService.getNumUserProfiles().addObserver(numUserProfiles ->
                UIThread.run(() -> model.getActiveUsers().set(String.valueOf(numUserProfiles))));

        channelsPins.addAll(bisqEasyOfferbookChannelService.getChannels().stream()
                .map(channel -> channel.getChatMessages().addObserver(this::updateOffersOnline))
                .collect(Collectors.toSet()));

        // We trigger a call of updateOffersOnline for each channel when registering our observer. But we only want one call, 
        // so we block execution of the code inside updateOffersOnline to only call it once.
        allowUpdateOffersOnline = true;
        updateOffersOnline();

        isNotificationVisiblePin = bisqEasyNotificationsService.getIsNotificationPanelVisible().addObserver(value -> handleBannerVisibility());
        unconsumedAlertsPin = alertNotificationsService.getUnconsumedAlerts().addObserver(this::handleBannerVisibility);
    }

    @Override
    public void onDeactivate() {
        muSigActivatedPin.unbind();
        selectedMarketPin.unbind();
        marketPricePin.unbind();
        getNumUserProfilesPin.unbind();
        isNotificationVisiblePin.unbind();
        unconsumedAlertsPin.unbind();
        channelsPins.forEach(Pin::unbind);
        channelsPins.clear();
    }

    public void onBuildReputation() {
        Navigation.navigateTo(NavigationTarget.BUILD_REPUTATION);
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
                    .ifPresent(marketPrice -> UIThread.run(() -> {
                        model.getMarketPrice().set(PriceFormatter.format(marketPrice.getPriceQuote(), true));
                        model.getMarketCode().set(marketPrice.getMarket().getMarketCodes());
                    }));
        }
    }

    private void updateOffersOnline() {
        if (allowUpdateOffersOnline) {
            UIThread.run(() -> {
                long numOffers = bisqEasyOfferbookMessageService.getAllOffers().count();
                model.getOffersOnline().set(String.valueOf(numOffers));
            });
        }
    }

    private void handleBannerVisibility() {
        model.getIsBannerVisible().set(bisqEasyNotificationsService.getIsNotificationPanelVisible().get() ||
                !alertNotificationsService.getUnconsumedAlerts().isEmpty());
    }
}
