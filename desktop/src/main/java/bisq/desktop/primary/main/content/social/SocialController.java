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

package bisq.desktop.primary.main.content.social;

import bisq.application.DefaultServiceProvider;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.social.hangout.HangoutController;
import bisq.desktop.primary.main.content.social.tradeintent.TradeIntentController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SocialController extends TabController {
    private final DefaultServiceProvider serviceProvider;
    @Getter
    private final SocialModel model;
    @Getter
    private final SocialView view;

    public SocialController(DefaultServiceProvider serviceProvider) {
        super(NavigationTarget.SOCIAL);

        this.serviceProvider = serviceProvider;
        model = new SocialModel(serviceProvider);
        view = new SocialView(model, this);
    }

    @Override
    protected Optional<Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case TRADE_INTENT -> {
                return Optional.of(new TradeIntentController(serviceProvider));
            }
            case HANGOUT -> {
                return Optional.of(new HangoutController(serviceProvider));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
