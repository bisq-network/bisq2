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

package bisq.desktop.primary.main.content.trade.bisqEasy;

import bisq.application.DefaultApplicationService;
import bisq.settings.DontShowAgainService;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationController;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.BisqEasyChatController;
import bisq.desktop.primary.main.content.trade.bisqEasy.onboarding.BisqEasyOnboardingController;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.settings.DontShowAgainKey.BISQ_EASY_INTRO;

//todo can be removed if we dont have a intro screen anymore... but leave it for now...
@Slf4j
public class BisqEasyController extends NavigationController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final BisqEasyModel model;
    @Getter
    private final BisqEasyView view;
    private final SettingsService settingsService;

    public BisqEasyController(DefaultApplicationService applicationService) {
        super(NavigationTarget.BISQ_EASY);

        this.applicationService = applicationService;
        settingsService = applicationService.getSettingsService();
        model = new BisqEasyModel();
        view = new BisqEasyView(model, this);
    }

    @Override
    public void onActivate() {
        if (DontShowAgainService.showAgain(BISQ_EASY_INTRO)){
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_INTRO);
        } else {
            Navigation.navigateTo(NavigationTarget.BISQ_EASY_CHAT);
        }
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case BISQ_EASY_CHAT -> {
                return Optional.of(new BisqEasyChatController(applicationService));
            }
            case BISQ_EASY_INTRO -> {
                return Optional.of(new BisqEasyOnboardingController(applicationService));
            }

            default -> {
                return Optional.empty();
            }
        }
    }
}
