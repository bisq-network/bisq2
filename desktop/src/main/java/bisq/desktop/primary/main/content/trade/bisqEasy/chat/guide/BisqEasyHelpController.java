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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.process.BisqEasyHelpProcessController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.rules.BisqEasyHelpRulesController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.security.BisqEasyHelpSecurityController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.welcome.BisqEasyHelpWelcomeController;
import bisq.desktop.primary.overlay.OverlayController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyHelpController extends TabController<BisqEasyHelpModel> {
    @Getter
    private final BisqEasyHelpView view;
    private final DefaultApplicationService applicationService;

    public BisqEasyHelpController(DefaultApplicationService applicationService) {
        super(new BisqEasyHelpModel(), NavigationTarget.BISQ_EASY_GUIDE);

        this.applicationService = applicationService;
        view = new BisqEasyHelpView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY_GUIDE_WELCOME: {
                return Optional.of(new BisqEasyHelpWelcomeController(applicationService));
            }
            case BISQ_EASY_GUIDE_SECURITY: {
                return Optional.of(new BisqEasyHelpSecurityController(applicationService));
            }
            case BISQ_EASY_GUIDE_PROCESS: {
                return Optional.of(new BisqEasyHelpProcessController(applicationService));
            }
            case BISQ_EASY_GUIDE_RULES: {
                return Optional.of(new BisqEasyHelpRulesController(applicationService));
            }
            default: {
                return Optional.empty();
            }
        }
    }

    void onClose() {
        OverlayController.hide();
    }

    public void onQuit() {
        applicationService.shutdown().thenAccept(result -> Platform.exit());
    }
}
