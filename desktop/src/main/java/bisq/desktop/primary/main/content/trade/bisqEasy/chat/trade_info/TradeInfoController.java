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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_info;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_info.offer.TradeInfoOfferController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_info.phase.TradeInfoPhaseController;
import bisq.protocol.poc.bisq_easy.BisqEasyTrade;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeInfoController extends TabController<TradeInfoModel> {
    @Getter
    private final TradeInfoView view;
    private final DefaultApplicationService applicationService;
    private final SettingsService settingsService;
    private final TradeInfoPhaseController tradeInfoPhaseController;

    public TradeInfoController(DefaultApplicationService applicationService) {
        super(new TradeInfoModel(), NavigationTarget.TRADE_INFO);

        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();
        view = new TradeInfoView(model, this);

        tradeInfoPhaseController = new TradeInfoPhaseController(applicationService);
    }

    @Override
    public void onActivate() {
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_INFO_COLLAPSED).orElse(false));
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_INFO_OFFER: {
                return Optional.of(new TradeInfoOfferController(applicationService));
            }
            case TRADE_INFO_PHASE: {
                return Optional.of(tradeInfoPhaseController);
            }
            default: {
                return Optional.empty();
            }
        }
    }

    public void setBisqEasyTrade(BisqEasyTrade bisqEasyTrade) {
        tradeInfoPhaseController.setBisqEasyTrade(bisqEasyTrade);
    }

    void onExpand() {
        setIsCollapsed(false);
    }

    void onCollapse() {
        setIsCollapsed(true);
    }

    void onHeaderClicked() {
        setIsCollapsed(!model.getIsCollapsed().get());
    }

    private void setIsCollapsed(boolean value) {
        model.getIsCollapsed().set(value);
        settingsService.setCookie(CookieKey.TRADE_INFO_COLLAPSED, value);
    }
}
