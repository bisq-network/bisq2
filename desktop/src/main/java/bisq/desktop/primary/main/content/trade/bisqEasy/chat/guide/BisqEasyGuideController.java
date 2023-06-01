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
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.process.BisqEasyGuideProcessController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.rules.BisqEasyGuideRulesController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.security.BisqEasyGuideSecurityController;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.welcome.BisqEasyGuideWelcomeController;
import bisq.desktop.primary.overlay.OverlayController;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class BisqEasyGuideController extends TabController<BisqEasyGuideModel> {
    @Getter
    private final BisqEasyGuideView view;
    private final DefaultApplicationService applicationService;

    public BisqEasyGuideController(DefaultApplicationService applicationService) {
        super(new BisqEasyGuideModel(), NavigationTarget.BISQ_EASY_GUIDE);

        this.applicationService = applicationService;
        view = new BisqEasyGuideView(model, this);
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
                return Optional.of(new BisqEasyGuideWelcomeController(applicationService));
            }
            case BISQ_EASY_GUIDE_SECURITY: {
                return Optional.of(new BisqEasyGuideSecurityController(applicationService));
            }
            case BISQ_EASY_GUIDE_PROCESS: {
                return Optional.of(new BisqEasyGuideProcessController(applicationService));
            }
            case BISQ_EASY_GUIDE_RULES: {
                return Optional.of(new BisqEasyGuideRulesController(applicationService));
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
