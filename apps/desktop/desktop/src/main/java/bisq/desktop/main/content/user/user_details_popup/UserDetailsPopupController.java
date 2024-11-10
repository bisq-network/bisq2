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

package bisq.desktop.main.content.user.user_details_popup;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.common.view.TabController;
import bisq.desktop.overlay.OverlayController;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UserDetailsPopupController extends TabController<UserDetailsPopupModel>
        implements InitWithDataController<UserDetailsPopupController.InitData> {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final UserProfile userProfile;

        public InitData(UserProfile userProfile) {
            this.userProfile = userProfile;
        }
    }

    @Getter
    private final UserDetailsPopupView view;
    private final ServiceProvider serviceProvider;
    private final ReputationService reputationService;
    private UserProfile userProfile;

    public UserDetailsPopupController(ServiceProvider serviceProvider) {
        super(new UserDetailsPopupModel(), NavigationTarget.USER_DETAILS);

        this.serviceProvider = serviceProvider;
        reputationService = serviceProvider.getUserService().getReputationService();
        view = new UserDetailsPopupView(model, this);
    }

    @Override
    public void onActivate() {
        model.setUserProfile(userProfile);
        model.getReputationScore().set(reputationService.getReputationScore(userProfile));
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
//            case USER_DETAILS_OVERVIEW -> Optional.of(new (serviceProvider));
//            case USER_DETAILS_OFFERS -> Optional.of(new (serviceProvider));
//            case USER_DETAILS_REPUTATION -> Optional.of(new (serviceProvider));
            default -> Optional.empty();
        };
    }

    @Override
    public void initWithData(InitData initData) {
        userProfile = initData.userProfile;
    }

    void onClose() {
        OverlayController.hide();
    }
}
