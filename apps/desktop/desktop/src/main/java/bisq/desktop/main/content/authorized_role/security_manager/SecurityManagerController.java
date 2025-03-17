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

import bisq.bonded_roles.bonded_role.AuthorizedBondedRole;
import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.bonded_roles.bonded_role.BondedRole;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.bonded_roles.security_manager.difficulty_adjustment.DifficultyAdjustmentService;
import bisq.common.network.Address;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final DifficultyAdjustmentService difficultyAdjustmentService;
    private Pin userIdentityPin, alertsPin, bondedRoleSetPin, difficultyAdjustmentListItemsPin;
    private Subscription messagePin, requireVersionForTradingPin, minVersionPin, selectedBondedRolePin,
            difficultyAdjustmentPin, bannedAccountDataPin;

    public SecurityManagerController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        difficultyAdjustmentService = serviceProvider.getBondedRolesService().getDifficultyAdjustmentService();
        authorizedBondedRolesService = serviceProvider.getBondedRolesService().getAuthorizedBondedRolesService();
        RoleInfo roleInfo = new RoleInfo(serviceProvider);
        model = new SecurityManagerModel();
        view = new SecurityManagerView(model, this, roleInfo.getRoot());
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

        model.getBondedRoleSortedList().setComparator((o1, o2) -> getBondedRoleDisplayString(o1.getBondedRole())
                .compareTo(getBondedRoleDisplayString(o2.getBondedRole())));

        difficultyAdjustmentListItemsPin = FxBindings.<AuthorizedDifficultyAdjustmentData, SecurityManagerView.DifficultyAdjustmentListItem>bind(model.getDifficultyAdjustmentListItems())
                .map(SecurityManagerView.DifficultyAdjustmentListItem::new)
                .to(difficultyAdjustmentService.getAuthorizedDifficultyAdjustmentDataSet());

        double difficultyAdjustmentFactor = difficultyAdjustmentService.getMostRecentValueOrDefault().get();
        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentFactor);
        difficultyAdjustmentPin = EasyBind.subscribe(model.getDifficultyAdjustmentFactor(), factor ->
                model.getDifficultyAdjustmentFactorButtonDisabled().set(factor == null ||
                        !isValidDifficultyAdjustmentFactor(factor.doubleValue())));
        bannedAccountDataPin = EasyBind.subscribe(model.getBannedAccountData(), e -> updateSendButtonDisabled());

        KeyPair keyPair = userIdentityService.getSelectedUserIdentity().getIdentity().getKeyBundle().getKeyPair();
        alertService.getAuthorizedAlertDataSet().forEach(authorizedAlert ->
                securityManagerService.rePublishAlert(authorizedAlert, keyPair));

        if (difficultyAdjustmentFactor != NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT) {
            securityManagerService.publishDifficultyAdjustment(difficultyAdjustmentFactor);
        }
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        bondedRoleSetPin.unbind();
        alertsPin.unbind();
        difficultyAdjustmentListItemsPin.unbind();
        messagePin.unsubscribe();
        requireVersionForTradingPin.unsubscribe();
        minVersionPin.unsubscribe();
        selectedBondedRolePin.unsubscribe();
        difficultyAdjustmentPin.unsubscribe();
        bannedAccountDataPin.unsubscribe();
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
        String message = model.getMessage().get();
        //todo (refactor, low prio) use validation framework instead (not impl yet)
        if (message != null && message.length() > AuthorizedAlertData.MAX_MESSAGE_LENGTH) {
            new Popup().warning(Res.get("authorizedRole.securityManager.alert.message.tooLong")).show();
            return;
        }
        String bannedAccountData = model.getBannedAccountData().get();
        if (bannedAccountData != null && bannedAccountData.length() > AuthorizedAlertData.MAX_BANNED_ACCOUNT_DATA_LENGTH) {
            new Popup().warning(Res.get("authorizedRole.securityManager.bannedAccounts.data.tooLong")).show();
            return;
        }

        SecurityManagerView.BondedRoleListItem bondedRoleListItem = model.getSelectedBondedRoleListItem().get();
        Optional<AuthorizedBondedRole> bannedRole = bondedRoleListItem == null ? Optional.empty() :
                Optional.ofNullable(bondedRoleListItem.getBondedRole().getAuthorizedBondedRole());
        securityManagerService.publishAlert(model.getSelectedAlertType().get(),
                        StringUtils.toOptional(model.getHeadline().get()),
                        StringUtils.toOptional(message),
                        model.getHaltTrading().get(),
                        model.getRequireVersionForTrading().get(),
                        StringUtils.toOptional(model.getMinVersion().get()),
                        bannedRole,
                        StringUtils.toOptional(bannedAccountData))
                .whenComplete((result, throwable) -> UIThread.run(() -> {
                    if (throwable != null) {
                        new Popup().error(throwable).show();
                    }

                    model.getSelectedAlertType().set(null);
                    model.getHeadline().set(null);
                    model.getMessage().set(null);
                    model.getHaltTrading().set(false);
                    model.getRequireVersionForTrading().set(false);
                    model.getMinVersion().set(null);
                    model.getSelectedBondedRoleListItem().set(null);
                    model.getBannedAccountData().set(null);
                }));
    }

    void onRemoveAlert(AuthorizedAlertData authorizedAlertData) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        securityManagerService.removeAlert(authorizedAlertData, userIdentity.getNetworkIdWithKeyPair().getKeyPair());
    }

    void onPublishDifficultyAdjustmentFactor() {
        double difficultyAdjustmentFactor = model.getDifficultyAdjustmentFactor().get();
        if (isValidDifficultyAdjustmentFactor(difficultyAdjustmentFactor)) {
            securityManagerService.publishDifficultyAdjustment(difficultyAdjustmentFactor)
                    .whenComplete((result, throwable) -> UIThread.run(() -> {
                        if (throwable != null) {
                            new Popup().error(throwable).show();
                        } else {
                            model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
                        }
                    }));
        }
    }

    void onRemoveDifficultyAdjustmentListItem(SecurityManagerView.DifficultyAdjustmentListItem item) {
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        securityManagerService.removeDifficultyAdjustment(item.getData(), userIdentity.getNetworkIdWithKeyPair().getKeyPair());
        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
    }


    boolean isRemoveDifficultyAdjustmentButtonVisible(AuthorizedDifficultyAdjustmentData data) {
        return userIdentityService.getSelectedUserIdentity().getId().equals(data.getSecurityManagerProfileId());
    }

    boolean isRemoveDifficultyAdjustmentButtonVisible(AuthorizedAlertData authorizedAlertData) {
        return userIdentityService.getSelectedUserIdentity().getId().equals(authorizedAlertData.getSecurityManagerProfileId());
    }

    String getBondedRoleDisplayString(BondedRole bondedRole) {
        AuthorizedBondedRole authorizedBondedRole = bondedRole.getAuthorizedBondedRole();
        String roleType = authorizedBondedRole.getBondedRoleType().getDisplayString().toUpperCase();
        String profileId = authorizedBondedRole.getProfileId();
        String nickNameOrBondName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(authorizedBondedRole.getBondUserName());
        Optional<String> addresses = authorizedBondedRole.getAddressByTransportTypeMap()
                .map(e -> e.values().stream()
                        .map(Address::getFullAddress)
                        .collect(Collectors.joining(", ")));
        return addresses.map(address -> Res.get("authorizedRole.securityManager.selectedBondedNode", roleType, nickNameOrBondName, profileId, address))
                .orElseGet(() -> Res.get("authorizedRole.securityManager.selectedBondedRole", roleType, nickNameOrBondName, profileId));
    }

    String getBannedBondedRoleDisplayString(AuthorizedBondedRole authorizedBondedRole) {
        String roleType = authorizedBondedRole.getBondedRoleType().getDisplayString();
        String profileId = authorizedBondedRole.getProfileId();
        String nickNameOrBondName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(authorizedBondedRole.getBondUserName());
        return Res.get("authorizedRole.securityManager.alert.table.bannedRole.value", roleType, nickNameOrBondName, profileId);
    }

    private void applySelectAlertType(AlertType alertType) {
        model.getAlertsVisible().set(false);
        model.getBondedRoleSelectionVisible().set(false);
        model.getBannedAccountDataVisible().set(false);

        model.getSelectedAlertType().set(alertType);
        switch (alertType) {
            case INFO:
            case WARN:
                model.getHaltTrading().set(false);
                model.getRequireVersionForTrading().set(false);
                model.getMinVersion().set(null);
                model.getSelectedBondedRoleListItem().set(null);
                model.getAlertsVisible().set(true);
                model.getBannedAccountData().set(null);
                break;
            case EMERGENCY:
                model.getSelectedBondedRoleListItem().set(null);
                model.getAlertsVisible().set(true);
                model.getBannedAccountData().set(null);
                break;
            case BAN:
                model.getHaltTrading().set(false);
                model.getRequireVersionForTrading().set(false);
                model.getMinVersion().set(null);
                model.getHeadline().set(null);
                model.getMessage().set(null);
                model.getBondedRoleSelectionVisible().set(true);
                model.getBannedAccountData().set(null);
                break;
            case BANNED_ACCOUNT_DATA:
                model.getBannedAccountDataVisible().set(true);
                break;
        }
        model.getActionButtonText().set(Res.get("authorizedRole.securityManager.actionButton." + alertType.name()));
    }

    private void updateSendButtonDisabled() {
        AlertType alertType = model.getSelectedAlertType().get();
        boolean isInvalid = alertType == null;
        if (isInvalid) {
            model.getActionButtonDisabled().set(true);
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
            case BANNED_ACCOUNT_DATA:
                boolean isBannedAccountDataEmpty = StringUtils.isEmpty(model.getBannedAccountData().get());
                model.getActionButtonDisabled().set(isBannedAccountDataEmpty);
                break;
        }
    }

    private static boolean isValidDifficultyAdjustmentFactor(double difficultyAdjustmentFactor) {
        return difficultyAdjustmentFactor >= 0 && difficultyAdjustmentFactor <= NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT;
    }

}
