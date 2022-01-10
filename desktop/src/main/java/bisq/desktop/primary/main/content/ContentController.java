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

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.primary.main.content.markets.MarketsController;
import bisq.desktop.primary.main.content.offerbook.OfferbookController;
import bisq.desktop.primary.main.content.portfolio.PortfolioController;
import bisq.desktop.primary.main.content.settings.SettingsController;
import bisq.desktop.primary.main.content.social.SocialController;
import bisq.desktop.primary.main.content.wallet.WalletController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ContentController extends NavigationController {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final ContentModel model;
    @Getter
    private final ContentView view;

    public ContentController(DefaultServiceProvider serviceProvider) {
        super(NavigationTarget.CONTENT);
        
        this.serviceProvider = serviceProvider;
        model = new ContentModel();
        view = new ContentView(model, this);
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget, Optional<Object> data) {
        switch (navigationTarget) {
            case SOCIAL -> {
                return Optional.of(new SocialController(serviceProvider));
            }
            case SETTINGS -> {
                return Optional.of(new SettingsController(serviceProvider));
            }
            case PORTFOLIO -> {
                return Optional.of(new PortfolioController(serviceProvider));
            }
            case WALLET -> {
                return Optional.of(new WalletController(serviceProvider));
            }
            case OFFERBOOK -> {
                return Optional.of(new OfferbookController(serviceProvider));
            }
            case MARKETS -> {
                return Optional.of(new MarketsController(serviceProvider));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
