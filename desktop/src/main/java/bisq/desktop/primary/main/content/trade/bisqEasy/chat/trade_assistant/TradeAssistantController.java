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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.details.TradeDetailsController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.state.TradeStateController;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
public class TradeAssistantController extends TabController<TradeAssistantModel> {
    @Getter
    private final TradeAssistantView view;
    private final SettingsService settingsService;
    private final TradeStateController tradeStateController;
    private final TradeDetailsController tradeDetailsController;

    public TradeAssistantController(DefaultApplicationService applicationService, Consumer<UserProfile> openUserProfileSidebarHandler) {
        super(new TradeAssistantModel(), NavigationTarget.TRADE_ASSISTANT);

        settingsService = applicationService.getSettingsService();

        tradeStateController = new TradeStateController(applicationService);
        tradeDetailsController = new TradeDetailsController(applicationService, openUserProfileSidebarHandler);

        view = new TradeAssistantView(model, this);
    }

    public void selectChannel(BisqEasyPrivateTradeChatChannel channel) {
        tradeStateController.selectChannel(channel);
        tradeDetailsController.selectChannel(channel);
    }

    @Override
    public void onActivate() {
        model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.TRADE_ASSISTANT_COLLAPSED).orElse(false));
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_STATE: {
                return Optional.of(tradeStateController);
            }
            case TRADE_DETAILS: {
                return Optional.of(tradeDetailsController);
            }
            default: {
                return Optional.empty();
            }
        }
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
        settingsService.setCookie(CookieKey.TRADE_ASSISTANT_COLLAPSED, value);
    }
}
