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

package bisq.desktop.primary.main.content.social.onboarded;

import bisq.application.DefaultApplicationService;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.social.onboarded.chat.ChatController;
import bisq.desktop.primary.main.content.social.onboarded.profile.UserProfileController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class OnboardedController extends TabController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final OnboardedModel model;
    @Getter
    private final OnboardedView view;

    public OnboardedController(DefaultApplicationService applicationService) {
        super(NavigationTarget.ONBOARDED);

        this.applicationService = applicationService;

        model = new OnboardedModel();
        view = new OnboardedView(model, this);
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
            case CHAT -> {
                return Optional.of(new ChatController(applicationService));
            }
            case USER_PROFILE -> {
                return Optional.of(new UserProfileController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
