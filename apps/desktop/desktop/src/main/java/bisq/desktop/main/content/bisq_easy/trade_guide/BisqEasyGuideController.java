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

package bisq.desktop.main.content.bisq_easy.trade_guide;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.bisq_easy.trade_guide.process.BisqEasyGuideProcessController;
import bisq.desktop.main.content.bisq_easy.trade_guide.rules.BisqEasyGuideRulesController;
import bisq.desktop.main.content.bisq_easy.trade_guide.security.BisqEasyGuideSecurityController;
import bisq.desktop.main.content.bisq_easy.trade_guide.welcome.BisqEasyGuideWelcomeController;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyGuideController extends TabController<BisqEasyGuideModel> {
    @Getter
    private final BisqEasyGuideView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;

    public BisqEasyGuideController(ServiceProvider serviceProvider) {
        super(new BisqEasyGuideModel(), NavigationTarget.BISQ_EASY_GUIDE);

        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();

        view = new BisqEasyGuideView(model, this);
    }

    @Override
    public void onActivate() {
        model.setTradeRulesConfirmed(settingsService.getTradeRulesConfirmed().get());
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY_GUIDE_WELCOME: {
                return Optional.of(new BisqEasyGuideWelcomeController(serviceProvider));
            }
            case BISQ_EASY_GUIDE_SECURITY: {
                return Optional.of(new BisqEasyGuideSecurityController(serviceProvider));
            }
            case BISQ_EASY_GUIDE_PROCESS: {
                return Optional.of(new BisqEasyGuideProcessController(serviceProvider));
            }
            case BISQ_EASY_GUIDE_RULES: {
                return Optional.of(new BisqEasyGuideRulesController(serviceProvider));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }
}
