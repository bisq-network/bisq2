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

package bisq.desktop.main.content.mu_sig.trade_guide;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.main.content.mu_sig.trade_guide.process.MuSigGuideProcessController;
import bisq.desktop.main.content.mu_sig.trade_guide.rules.MuSigGuideRulesController;
import bisq.desktop.main.content.mu_sig.trade_guide.security.MuSigGuideSecurityController;
import bisq.desktop.main.content.mu_sig.trade_guide.welcome.MuSigGuideWelcomeController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.desktop.overlay.OverlayController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigGuideController extends TabController<MuSigGuideModel> {
    @Getter
    private final MuSigGuideView view;
    private final ServiceProvider serviceProvider;
    private final SettingsService settingsService;

    public MuSigGuideController(ServiceProvider serviceProvider) {
        super(new MuSigGuideModel(), NavigationTarget.MU_SIG_GUIDE);

        this.serviceProvider = serviceProvider;
        settingsService = serviceProvider.getSettingsService();

        view = new MuSigGuideView(model, this);
    }

    @Override
    public void onActivate() {
        model.setTradeRulesConfirmed(settingsService.getMuSigTradeRulesConfirmed().get());
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_GUIDE_WELCOME -> Optional.of(new MuSigGuideWelcomeController(serviceProvider));
            case MU_SIG_GUIDE_SECURITY -> Optional.of(new MuSigGuideSecurityController(serviceProvider));
            case MU_SIG_GUIDE_PROCESS -> Optional.of(new MuSigGuideProcessController(serviceProvider));
            case MU_SIG_GUIDE_RULES -> Optional.of(new MuSigGuideRulesController(serviceProvider));
            default -> Optional.empty();
        };
    }

    void onClose() {
        OverlayController.hide();
    }
}
