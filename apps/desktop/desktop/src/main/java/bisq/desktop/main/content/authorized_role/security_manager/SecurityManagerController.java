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
import bisq.bonded_roles.security_manager.min_reputation_score.AuthorizedMinRequiredReputationScoreData;
import bisq.bonded_roles.security_manager.min_reputation_score.MinRequiredReputationScoreService;
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
import bisq.user.reputation.ReputationScore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

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
    private final DifficultyAdjustmentService difficultyAdjustmentService;
    private final MinRequiredReputationScoreService minRequiredReputationScoreService;
    private Pin userIdentityPin, alertsPin, bondedRoleSetPin, difficultyAdjustmentListItemsPin,
            minRequiredReputationScoreListItemsPin;
    private Subscription messagePin, requireVersionForTradingPin, minVersionPin, selectedBondedRolePin,
            difficultyAdjustmentPin, minRequiredReputationScorePin;

    public SecurityManagerController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();
        alertService = serviceProvider.getBondedRolesService().getAlertService();
        difficultyAdjustmentService = serviceProvider.getBondedRolesService().getDifficultyAdjustmentService();
        minRequiredReputationScoreService = serviceProvider.getBondedRolesService().getMinRequiredReputationScoreService();
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

        difficultyAdjustmentListItemsPin = FxBindings.<AuthorizedDifficultyAdjustmentData, SecurityManagerView.DifficultyAdjustmentListItem>bind(model.getDifficultyAdjustmentListItems())
                .map(SecurityManagerView.DifficultyAdjustmentListItem::new)
                .to(difficultyAdjustmentService.getAuthorizedDifficultyAdjustmentDataSet());

        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
        difficultyAdjustmentPin = EasyBind.subscribe(model.getDifficultyAdjustmentFactor(), difficultyAdjustmentFactor ->
                model.getDifficultyAdjustmentFactorButtonDisabled().set(difficultyAdjustmentFactor == null ||
                        !isValidDifficultyAdjustmentFactor(difficultyAdjustmentFactor.doubleValue())));

        minRequiredReputationScoreListItemsPin = FxBindings.<AuthorizedMinRequiredReputationScoreData, SecurityManagerView.MinRequiredReputationScoreListItem>bind(model.getMinRequiredReputationScoreListItems())
                .map(SecurityManagerView.MinRequiredReputationScoreListItem::new)
                .to(minRequiredReputationScoreService.getAuthorizedMinRequiredReputationScoreDataSet());

        model.getMinRequiredReputationScore().set(minRequiredReputationScoreService.getMostRecentValueOrDefault().get());
        minRequiredReputationScorePin = EasyBind.subscribe(model.getMinRequiredReputationScore(), minRequiredReputationScore ->
                model.getMinRequiredReputationScoreButtonDisabled().set(minRequiredReputationScore == null ||
                        !isValidMinRequiredRequiredReputationScore(minRequiredReputationScore.doubleValue())));
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        bondedRoleSetPin.unbind();
        alertsPin.unbind();
        difficultyAdjustmentListItemsPin.unbind();
        minRequiredReputationScoreListItemsPin.unbind();
        messagePin.unsubscribe();
        requireVersionForTradingPin.unsubscribe();
        minVersionPin.unsubscribe();
        selectedBondedRolePin.unsubscribe();
        difficultyAdjustmentPin.unsubscribe();
        minRequiredReputationScorePin.unsubscribe();
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
        SecurityManagerView.BondedRoleListItem bondedRoleListItem = model.getSelectedBondedRoleListItem().get();
        Optional<AuthorizedBondedRole> bannedRole = bondedRoleListItem == null ? Optional.empty() :
                Optional.ofNullable(bondedRoleListItem.getBondedRole().getAuthorizedBondedRole());
        securityManagerService.publishAlert(model.getSelectedAlertType().get(),
                        StringUtils.toOptional(model.getHeadline().get()),
                        StringUtils.toOptional(message),
                        model.getHaltTrading().get(),
                        model.getRequireVersionForTrading().get(),
                        StringUtils.toOptional(model.getMinVersion().get()),
                        bannedRole)
                .whenComplete((result, throwable) -> {
                    UIThread.run(() -> {
                        if (throwable != null) {
                            new Popup().error(throwable).show();
                        } else {
                            model.getSelectedAlertType().set(null);
                            model.getHeadline().set(null);
                            model.getMessage().set(null);
                            model.getHaltTrading().set(false);
                            model.getRequireVersionForTrading().set(false);
                            model.getMinVersion().set(null);
                            model.getSelectedBondedRoleListItem().set(null);
                        }
                    });
                });
    }

    boolean isRemoveDifficultyAdjustmentButtonVisible(AuthorizedAlertData authorizedAlertData) {
        if (userIdentityService.getSelectedUserIdentity() == null) {
            return false;
        }
        return userIdentityService.getSelectedUserIdentity().getId().equals(authorizedAlertData.getSecurityManagerProfileId());
    }

    void onRemoveAlert(AuthorizedAlertData authorizedAlertData) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        securityManagerService.removeAlert(authorizedAlertData, userIdentity.getNetworkIdWithKeyPair().getKeyPair());
    }

    String getBondedRoleShortDisplayString(BondedRole bondedRole) {
        AuthorizedBondedRole authorizedBondedRole = bondedRole.getAuthorizedBondedRole();
        String roleType = authorizedBondedRole.getBondedRoleType().getDisplayString();
        String profileId = authorizedBondedRole.getProfileId();
        String nickName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(Res.get("data.na"));
        return Res.get("authorizedRole.securityManager.selectedBondedRole", nickName, roleType, profileId);
    }

    String getBondedRoleDisplayString(AuthorizedBondedRole authorizedBondedRole) {
        String roleType = authorizedBondedRole.getBondedRoleType().getDisplayString();
        String profileId = authorizedBondedRole.getProfileId();
        String nickName = userProfileService.findUserProfile(profileId)
                .map(UserProfile::getNickName)
                .orElse(Res.get("data.na"));
        return Res.get("authorizedRole.securityManager.alert.table.bannedRole.value", roleType, nickName, profileId);
    }

    void onPublishDifficultyAdjustmentFactor() {
        double difficultyAdjustmentFactor = model.getDifficultyAdjustmentFactor().get();
        if (isValidDifficultyAdjustmentFactor(difficultyAdjustmentFactor)) {
            securityManagerService.publishDifficultyAdjustment(difficultyAdjustmentFactor)
                    .whenComplete((result, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable != null) {
                                new Popup().error(throwable).show();
                            } else {
                                model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
                            }
                        });
                    });
        }
    }

    void onPublishMinRequiredReputationScore() {
        long minRequiredReputationScore = model.getMinRequiredReputationScore().get();
        if (isValidMinRequiredRequiredReputationScore(minRequiredReputationScore)) {
            securityManagerService.publishMinRequiredReputationScore(minRequiredReputationScore)
                    .whenComplete((result, throwable) -> {
                        UIThread.run(() -> {
                            if (throwable != null) {
                                new Popup().error(throwable).show();
                            } else {
                                model.getMinRequiredReputationScore().set(minRequiredReputationScoreService.getMostRecentValueOrDefault().get());
                            }
                        });
                    });
        }
    }

    private static boolean isValidDifficultyAdjustmentFactor(double difficultyAdjustmentFactor) {
        return difficultyAdjustmentFactor >= 0 && difficultyAdjustmentFactor <= NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT;
    }

    private static boolean isValidMinRequiredRequiredReputationScore(double minRequiredReputationScore) {
        return minRequiredReputationScore >= 0 && minRequiredReputationScore <= ReputationScore.MAX_VALUE;
    }

    boolean isRemoveDifficultyAdjustmentButtonVisible(AuthorizedDifficultyAdjustmentData data) {
        if (userIdentityService.getSelectedUserIdentity() == null) {
            return false;
        }
        return userIdentityService.getSelectedUserIdentity().getId().equals(data.getSecurityManagerProfileId());
    }

    boolean isRemoveMinRequiredReputationScoreButtonVisible(AuthorizedMinRequiredReputationScoreData data) {
        if (userIdentityService.getSelectedUserIdentity() == null) {
            return false;
        }
        return userIdentityService.getSelectedUserIdentity().getId().equals(data.getSecurityManagerProfileId());
    }

    void onRemoveDifficultyAdjustmentListItem(SecurityManagerView.DifficultyAdjustmentListItem item) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        securityManagerService.removeDifficultyAdjustment(item.getData(), userIdentity.getNetworkIdWithKeyPair().getKeyPair());
        model.getDifficultyAdjustmentFactor().set(difficultyAdjustmentService.getMostRecentValueOrDefault().get());
    }

    void onRemoveMinRequiredReputationScoreListItem(SecurityManagerView.MinRequiredReputationScoreListItem item) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        securityManagerService.removeMinRequiredReputationScore(item.getData(), userIdentity.getNetworkIdWithKeyPair().getKeyPair());
        model.getMinRequiredReputationScore().set(minRequiredReputationScoreService.getMostRecentValueOrDefault().get());
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
                model.getHeadline().set(null);
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
