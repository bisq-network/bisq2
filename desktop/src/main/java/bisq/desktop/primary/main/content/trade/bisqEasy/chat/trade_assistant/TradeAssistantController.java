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
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.negotiation.TradeAssistantNegotiationController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.offer.TradeAssistantOfferController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.trade.TradeAssistantTradeController;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class TradeAssistantController extends TabController<TradeAssistantModel> {
    @Getter
    private final TradeAssistantView view;
    private final DefaultApplicationService applicationService;
    private final SettingsService settingsService;
    private final TradeAssistantOfferController tradeAssistantOfferController;
    private final TradeAssistantNegotiationController tradeAssistantNegotiationController;
    private final TradeAssistantTradeController tradeAssistantTradeController;

    public TradeAssistantController(DefaultApplicationService applicationService) {
        super(new TradeAssistantModel(), NavigationTarget.TRADE_ASSISTANT);

        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();

        tradeAssistantOfferController = new TradeAssistantOfferController(applicationService);
        tradeAssistantNegotiationController = new TradeAssistantNegotiationController(applicationService);
        tradeAssistantTradeController = new TradeAssistantTradeController(applicationService);

        view = new TradeAssistantView(model, this);
    }

    public void setBisqEasyOffer(BisqEasyOffer bisqEasyOffer) {
        tradeAssistantOfferController.setBisqEasyOffer(bisqEasyOffer);
        tradeAssistantNegotiationController.setBisqEasyOffer(bisqEasyOffer);
        tradeAssistantTradeController.setBisqEasyOffer(bisqEasyOffer);
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
            case TRADE_ASSISTANT_OFFER: {
                return Optional.of(tradeAssistantOfferController);
            }
            case TRADE_ASSISTANT_NEGOTIATION: {
                return Optional.of(tradeAssistantNegotiationController);
            }
            case TRADE_ASSISTANT_TRADE: {
                return Optional.of(tradeAssistantTradeController);
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
