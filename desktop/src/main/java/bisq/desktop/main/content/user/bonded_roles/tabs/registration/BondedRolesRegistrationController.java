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

package bisq.desktop.main.content.user.bonded_roles.tabs.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.registration.BondedRoleRegistrationService;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public abstract class BondedRolesRegistrationController implements Controller {
    @Getter
    protected final BondedRolesRegistrationView<? extends BondedRolesRegistrationModel, ? extends BondedRolesRegistrationController> view;
    protected final BondedRolesRegistrationModel model;
    protected final UserIdentityService userIdentityService;
    protected final BondedRoleRegistrationService bondedRoleRegistrationService;
    protected final ServiceProvider serviceProvider;
    protected final BondedRoleType bondedRoleType;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private final SettingsService settingsService;
    protected Pin selectedUserProfilePin, bondedRoleSetPin;

    public BondedRolesRegistrationController(ServiceProvider serviceProvider, BondedRoleType bondedRoleType) {
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        bondedRoleRegistrationService = serviceProvider.getBondedRolesService().getBondedRoleRegistrationService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        settingsService = serviceProvider.getSettingsService();
        this.serviceProvider = serviceProvider;
        this.bondedRoleType = bondedRoleType;

        model = createAndGetModel();
        view = createAndGetView();
    }

    protected abstract BondedRolesRegistrationModel createAndGetModel();

    protected abstract BondedRolesRegistrationView<? extends BondedRolesRegistrationModel, ? extends BondedRolesRegistrationController> createAndGetView();

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                userIdentity -> {
                    UIThread.run(() -> {
                        model.getSelectedChatUserIdentity().set(userIdentity);
                        if (userIdentity != null) {
                            model.getProfileId().set(userIdentity.getId());
                            model.setAuthorizedPublicKey(userIdentity.getUserProfile().getPubKeyAsHex());
                        }
                        applyRequestCancellationButtonVisible();
                    });
                }
        );
        bondedRoleSetPin = authorizedBondedRolesService.getBondedRoles().addObserver(() -> UIThread.run(this::applyRequestCancellationButtonVisible));
        applyRequestRegistrationButtonDisabledBinding();

        // Issue with height of MultiLine text so lets wait until the component is fixed
        // model.getIsCollapsed().set(settingsService.getCookie().asBoolean(CookieKey.BONDED_ROLES_COLLAPSED).orElse(false));
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
        bondedRoleSetPin.unbind();
        model.getRequestButtonDisabled().unbind();
    }

    public void onRequestAuthorization() {
        requestBondedRoleRegistration(false);
    }

    public void onRequestCancellation() {
        requestBondedRoleRegistration(true);
    }

    public void onLearnMore() {
        Browser.open("https://bisq.wiki/Bisq_2_Roles");
    }

    public void onCopyToClipboard() {
        ClipboardUtil.copyToClipboard(model.getProfileId().get());
    }

    void onExpand() {
        setIsCollapsed(false);
    }

    void onCollapse() {
        setIsCollapsed(true);
    }

    void onHeaderClicked() {
        setIsCollapsed(!model.getIsCollapsed().get());
    }

    private void setIsCollapsed(boolean value) {
        model.getIsCollapsed().set(value);
        settingsService.setCookie(CookieKey.BONDED_ROLES_COLLAPSED, value);
    }

    protected void applyRequestRegistrationButtonDisabledBinding() {
        model.getRequestButtonDisabled().bind(model.getBondUserName().isEmpty().or(model.getSignature().isEmpty()));
    }

    protected void applyRequestCancellationButtonVisible() {
        UserIdentity selectedUserIdentity = userIdentityService.getSelectedUserIdentity();
        model.getRequestCancellationButtonVisible().set(
                selectedUserIdentity != null && authorizedBondedRolesService.getAuthorizedBondedRoleStream()
                        .filter(bondedRole -> bondedRole.getBondedRoleType() == model.getBondedRoleType())
                        .anyMatch(bondedRole -> selectedUserIdentity.getUserProfile().getId().equals(bondedRole.getProfileId())));
    }

    protected void requestBondedRoleRegistration(boolean isCancellationRequest) {
        checkNotNull(userIdentityService.getSelectedUserIdentity());
        checkNotNull(model.getProfileId().get());
        checkNotNull(model.getAuthorizedPublicKey());
        boolean success = bondedRoleRegistrationService.requestBondedRoleRegistration(
                model.getProfileId().get(),
                model.getAuthorizedPublicKey(),
                model.getBondedRoleType(),
                model.getBondUserName().get(),
                model.getSignature().get(),
                model.getAddressByNetworkType(),
                checkNotNull(userIdentityService.getSelectedUserIdentity()).getNetworkIdWithKeyPair(),
                isCancellationRequest);
        if (success) {
            model.getBondUserName().set("");
            model.getSignature().set("");
            String successMessage = isCancellationRequest ?
                    Res.get("user.bondedRoles.cancellation.success") :
                    Res.get("user.bondedRoles.registration.success");
            new Popup().information(successMessage)
                    .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                    .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                    .show();
        } else {
            String warnMessage = isCancellationRequest ?
                    Res.get("user.bondedRoles.cancellation.failed", StringUtils.truncate(model.getSignature().get())) :
                    Res.get("user.bondedRoles.registration.failed", StringUtils.truncate(model.getSignature().get()));
            new Popup().warning(warnMessage)
                    .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                    .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                    .show();
        }
    }
}
