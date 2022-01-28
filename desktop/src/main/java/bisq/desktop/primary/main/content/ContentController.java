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

package bisq.desktop.primary.main.content;

import bisq.application.DefaultApplicationService;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.primary.main.content.markets.MarketsController;
import bisq.desktop.primary.main.content.portfolio.PortfolioController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.social.SocialController;
import bisq.desktop.primary.main.content.trade.TradeController;
import bisq.desktop.primary.main.content.wallet.WalletController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ContentController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final ContentModel model;
    @Getter
    private final ContentView view;

    public ContentController(DefaultApplicationService applicationService) {
        super(NavigationTarget.CONTENT);
        
        this.applicationService = applicationService;
        model = new ContentModel();
        view = new ContentView(model, this);
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SOCIAL -> {
                return Optional.of(new SocialController(applicationService));
            }
            case SETTINGS -> {
                return Optional.of(new SettingsController(applicationService));
            }
            case PORTFOLIO -> {
                return Optional.of(new PortfolioController(applicationService));
            }
            case WALLET -> {
                return Optional.of(new WalletController(applicationService));
            }
            case TRADE -> {
                return Optional.of(new TradeController(applicationService));
            }
            case MARKETS -> {
                return Optional.of(new MarketsController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
