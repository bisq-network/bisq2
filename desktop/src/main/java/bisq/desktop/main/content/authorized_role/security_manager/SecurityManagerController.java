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

import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.support.alert.AlertService;
import bisq.support.alert.AlertType;
import bisq.support.alert.AuthorizedAlertData;
import bisq.support.security_manager.SecurityManagerService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.security.KeyPair;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SecurityManagerController implements Controller {
    @Getter
    private final SecurityManagerView view;
    private final SecurityManagerModel model;
    private final UserIdentityService userIdentityService;
    private final SecurityManagerService securityManagerService;
    private final AlertService alertService;
    private Pin userIdentityPin;
    private Subscription messagePin, requireVersionForTradingPin, minVersionPin, bannedRoleProfileIdPin;
    private Pin alertsPin;

    public SecurityManagerController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        alertService = serviceProvider.getSupportService().getAlertService();
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
        bannedRoleProfileIdPin = EasyBind.subscribe(model.getBannedRoleProfileId(), e -> updateSendButtonDisabled());

        alertsPin = FxBindings.<AuthorizedData, SecurityManagerView.AlertListItem>bind(model.getBondedRolesListItems())
                .map(SecurityManagerView.AlertListItem::new)
                .to(alertService.getAuthorizedDataSet());
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        messagePin.unsubscribe();
        requireVersionForTradingPin.unsubscribe();
        minVersionPin.unsubscribe();
        bannedRoleProfileIdPin.unsubscribe();
        alertsPin.unbind();
    }

    void onSelectAlertType(AlertType alertType) {
        if (alertType != null) {
            applySelectAlertType(alertType);
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

        AuthorizedAlertData authorizedAlertData = new AuthorizedAlertData(StringUtils.createUid(),
                new Date().getTime(),
                alertType,
                StringUtils.toOptional(model.getMessage().get()),
                model.getHaltTrading().get(),
                model.getRequireVersionForTrading().get(),
                StringUtils.toOptional(model.getMinVersion().get()),
                StringUtils.toOptional(model.getBannedRoleProfileId().get()));
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        KeyPair keyPair = userIdentity.getIdentity().getKeyPair();
        securityManagerService.publishAlert(userIdentity.getNodeIdAndKeyPair(),
                authorizedAlertData,
                keyPair.getPrivate(),
                keyPair.getPublic());
    }

    void onRemoveAlert(AuthorizedData authorizedData) {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        securityManagerService.removeAlert(authorizedData, userIdentity.getNodeIdAndKeyPair());
    }

    private void applySelectAlertType(AlertType alertType) {
        model.getSelectedAlertType().set(alertType);
        switch (alertType) {
            case INFO:
            case WARN:
                model.getHaltTrading().set(false);
                model.getRequireVersionForTrading().set(false);
                model.getMinVersion().set(null);
                model.getBannedRoleProfileId().set(null);
                break;
            case EMERGENCY:
                model.getBannedRoleProfileId().set(null);
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
                model.getActionButtonDisabled().set(StringUtils.isEmpty(model.getBannedRoleProfileId().get()));
                break;
        }
    }
}
