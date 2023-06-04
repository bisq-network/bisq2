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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade_assistant.negotiation;

import bisq.application.DefaultApplicationService;
import bisq.chat.bisqeasy.channel.priv.BisqEasyPrivateTradeChatChannel;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeAssistantNegotiationController implements Controller {
    @Getter
    private final TradeAssistantNegotiationView view;
    private final TradeAssistantNegotiationModel model;
    private final DefaultApplicationService applicationService;

    public TradeAssistantNegotiationController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new TradeAssistantNegotiationModel();
        view = new TradeAssistantNegotiationView(model, this);
    }

    public void setBisqEasyPrivateTradeChatChannel(BisqEasyPrivateTradeChatChannel privateChannel) {
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    void onNext() {
        Navigation.navigateTo(NavigationTarget.TRADE_ASSISTANT_TRADE);
    }

    void onBack() {
        Navigation.navigateTo(NavigationTarget.TRADE_ASSISTANT_OFFER);
    }

    void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }

    void onOpenTradeGuide() {
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_GUIDE);
    }
}
