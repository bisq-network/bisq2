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

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.NavigationTarget;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.social.chat.ChatController;
import bisq.desktop.primary.main.content.social.createoffer.CreateSimpleOfferController;
import bisq.desktop.primary.main.content.social.init.InitialUserNameController;
import bisq.desktop.primary.main.content.social.profile.UserProfileController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SocialController extends TabController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SocialModel model;
    @Getter
    private final SocialView view;
    private Pin selectedUserProfilePin;

    public SocialController(DefaultApplicationService applicationService) {
        super(NavigationTarget.SOCIAL);

        this.applicationService = applicationService;

        model = new SocialModel(applicationService.getUserProfileService());
        view = new SocialView(model, this);
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.bind(model.getSelectedUserProfile())
                .to(applicationService.getUserProfileService().getPersistableStore().getSelectedUserProfile());
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case SETUP_INITIAL_USER_PROFILE -> {
                return Optional.of(new InitialUserNameController(applicationService));
            }
            case CREATE_SIMPLE_OFFER -> {
                return Optional.of(new CreateSimpleOfferController(applicationService));
            }
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
