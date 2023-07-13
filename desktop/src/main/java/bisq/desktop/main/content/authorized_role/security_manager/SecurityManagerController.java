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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.alert.AlertService;
import bisq.bonded_roles.alert.AlertType;
import bisq.bonded_roles.alert.AuthorizedAlertData;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SecurityManagerController implements Controller {
    @Getter
    private final SecurityManagerView view;
    private final SecurityManagerModel model;
    private final UserIdentityService userIdentityService;
    private final SecurityManagerService securityManagerService;
    private final AlertService alertService;
    private final UserProfileService userProfileService;
    private final AuthorizedBondedRolesService authorizedBondedRolesService;
    private Pin userIdentityPin, alertsPin, bondedRoleSetPin;
    private Subscription messagePin, requireVersionForTradingPin, minVersionPin, selectedBondedRolePin;

    public SecurityManagerController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        model = new SecurityManagerModel();
        view = new SecurityManagerView(model, this);
    }

    @Override
    public void onActivate() {
        applySelectAlertType(AlertType.INFO);
        model.getAlertTypes().setAll(AlertType.values());

        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> UIThread.run(this::updateSendButtonDisabled));
        messagePin = EasyBind.subscribe(model.getMessage(), e -> updateSendButtonDisabled());
        requireVersionForTradingPin = EasyBind.subscribe(model.getRequireVersionForTrading(), e -> updateSendButtonDisabled());
        minVersionPin = EasyBind.subscribe(model.getMinVersion(), e -> updateSendButtonDisabled());
        selectedBondedRolePin = EasyBind.subscribe(model.getSelectedBondedRoleListItem(), e -> updateSendButtonDisabled());

        alertsPin = FxBindings.<AuthorizedAlertData, SecurityManagerView.AlertListItem>bind(model.getAlertListItems())
                .map(authorizedBondedRole -> new SecurityManagerView.AlertListItem(authorizedBondedRole, this))
                .to(alertService.getAuthorizedAlertDataSet());

        bondedRoleSetPin = FxBindings.<BondedRole, SecurityManagerView.BondedRoleListItem>bind(model.getBondedRoleListItems())
                .map(bondedRole -> new SecurityManagerView.BondedRoleListItem(bondedRole, this))
                .to(authorizedBondedRolesService.getBondedRoles());
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        bondedRoleSetPin.unbind();
        messagePin.unsubscribe();
        requireVersionForTradingPin.unsubscribe();
        minVersionPin.unsubscribe();
        selectedBondedRolePin.unsubscribe();
        alertsPin.unbind();
    }

    void onSelectAlertType(AlertType alertType) {
        if (alertType != null) {
            applySelectAlertType(alertType);
        }
    }

    void onBondedRoleListItem(SecurityManagerView.BondedRoleListItem bondedRoleListItem) {
        if (bondedRoleListItem != null) {
            model.getSelectedBondedRoleListItem().set(bondedRoleListItem);
        }
    }

    void onSendAlert() {
        AlertType alertType = model.getSelectedAlertType().get();
        String message = model.getMessage().get();
        //todo use validation framework instead (not impl yet)
        if (message != null && message.length() > AuthorizedAlertData.MAX_MESSAGE_LENGTH) {
            new Popup().warning(Res.get("authorizedRole.securityManager.alert.message.tooLong")).show();
            return;
        }
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        String profileId = userIdentity.getId();
        AuthorizedAlertData authorizedAlertData = new AuthorizedAlertData(StringUtils.createUid(),
                new Date().getTime(),
                alertType,
                StringUtils.toOptional(model.getMessage().get()),
                model.getHaltTrading().get(),
                model.getRequireVersionForTrading().get(),
                StringUtils.toOptional(model.getMinVersion().get()),
                Optional.ofNullable(model.getSelectedBondedRoleListItem().get().getBondedRole().getAuthorizedBondedRole()),
                profileId);

        KeyPair keyPair = userIdentity.getIdentity().getKeyPair();
        PublicKey authorizedPublicKey = keyPair.getPublic();
        PrivateKey authorizedPrivateKey = keyPair.getPrivate();
        securityManagerService.publishAlert(userIdentity.getNodeIdAndKeyPair().getKeyPair(),
                authorizedAlertData,
                authorizedPrivateKey,
                authorizedPublicKey);
    }

    boolean isRemoveButtonVisible(AuthorizedAlertData authorizedAlertData) {
        if (userIdentityService.getSelectedUserIdentity() == null) {
            return false;
        }
        return userIdentityService.getSelectedUserIdentity().getId().equals(authorizedAlertData.getSecurityManagerProfileId());
    }

    void onRemoveAlert(AuthorizedAlertData authorizedAlertData) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        securityManagerService.removeAlert(authorizedAlertData, userIdentity.getNodeIdAndKeyPair().getKeyPair());
    }

    String getBondedRoleShortDisplayString(BondedRole bondedRole) {
        AuthorizedBondedRole authorizedBondedRole = bondedRole.getAuthorizedBondedRole();
        String roleType = Res.get("user.bondedRoles.type." + authorizedBondedRole.getBondedRoleType().name());
        String profileId = authorizedBondedRole.getProfileId();
        String nickName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(Res.get("data.na"));
        return Res.get("authorizedRole.securityManager.selectedBondedRole", nickName, roleType, profileId);
    }

    String getBondedRoleDisplayString(AuthorizedBondedRole authorizedBondedRole) {
        String roleType = Res.get("user.bondedRoles.type." + authorizedBondedRole.getBondedRoleType().name());
        String profileId = authorizedBondedRole.getProfileId();
        String nickName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(Res.get("data.na"));
        return Res.get("authorizedRole.securityManager.alert.table.bannedRole.value", nickName, roleType, profileId);
    }

    private void applySelectAlertType(AlertType alertType) {
        model.getSelectedAlertType().set(alertType);
        switch (alertType) {
            case INFO:
            case WARN:
                model.getHaltTrading().set(false);
                model.getRequireVersionForTrading().set(false);
                model.getMinVersion().set(null);
                model.getSelectedBondedRoleListItem().set(null);
                break;
            case EMERGENCY:
                model.getSelectedBondedRoleListItem().set(null);
                break;
            case BAN:
                model.getHaltTrading().set(false);
                model.getRequireVersionForTrading().set(false);
                model.getMinVersion().set(null);
                model.getMessage().set(null);
                break;
        }
        model.getActionButtonText().set(Res.get("authorizedRole.securityManager.actionButton." + alertType.name()));
    }

    private void updateSendButtonDisabled() {
        AlertType alertType = model.getSelectedAlertType().get();
        boolean value = userIdentityService.getSelectedUserIdentity() == null || alertType == null;
        if (value) {
            model.getActionButtonDisabled().set(value);
            return;
        }
        boolean isMessageEmpty = StringUtils.isEmpty(model.getMessage().get());
        switch (alertType) {
            case INFO:
            case WARN:
                model.getActionButtonDisabled().set(isMessageEmpty);
                break;
            case EMERGENCY:
                boolean isMinVersionNeededAndEmpty = model.getRequireVersionForTrading().get() && StringUtils.isEmpty(model.getMinVersion().get());
                model.getActionButtonDisabled().set(isMessageEmpty || isMinVersionNeededAndEmpty);
                break;
            case BAN:
                model.getActionButtonDisabled().set(model.getSelectedBondedRoleListItem().get() == null);
                break;
        }
    }
}
