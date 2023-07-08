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

package bisq.desktop.main.content.user.roles.tabs.registration;

import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import bisq.user.identity.UserIdentityService;
import bisq.user.role.RoleRegistrationService;
import bisq.user.role.RoleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoleRegistrationController implements Controller {
    @Getter
    private final RoleRegistrationView view;
    private final RoleRegistrationModel model;
    private final UserIdentityService userIdentityService;
    private final RoleRegistrationService roleRegistrationService;
    private Pin selectedUserProfilePin;

    public RoleRegistrationController(ServiceProvider serviceProvider, RoleType roleType) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        roleRegistrationService = serviceProvider.getUserService().getRoleRegistrationService();

        UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);
        model = new RoleRegistrationModel(roleType);
        view = new RoleRegistrationView(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                chatUserIdentity -> {
                    model.getSelectedChatUserIdentity().set(chatUserIdentity);
                    model.getProfileId().set(chatUserIdentity.getId());
                }
        );

        model.getRequestRegistrationButtonDisabled().bind(model.getBondUserName().isEmpty());
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
        model.getRequestRegistrationButtonDisabled().unbind();
    }

    void onRequestAuthorization() {
        ClipboardUtil.getClipboardString().ifPresent(signature -> {
            boolean success = roleRegistrationService.requestAuthorization(model.getProfileId().get(),
                    model.getRoleType(),
                    model.getBondUserName().get(),
                    signature);
            if (success) {
                new Popup().information(Res.get("user.reputation.request.success"))
                        .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                        .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                        .show();
            } else {
                new Popup().warning(Res.get("user.reputation.request.error", StringUtils.truncate(signature)))
                        .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                        .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                        .show();
            }
        });
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisq2/roles/" + model.getRoleType().name().toLowerCase());
    }

    void onCopyToClipboard() {
        ClipboardUtil.copyToClipboard(model.getProfileId().get());
    }
}
