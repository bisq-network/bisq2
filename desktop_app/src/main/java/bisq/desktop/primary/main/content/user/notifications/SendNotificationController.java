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

package bisq.desktop.primary.main.content.user.notifications;

import bisq.application.DefaultApplicationService;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.security.KeyGeneration;
import bisq.support.alert.AlertService;
import bisq.support.alert.AlertType;
import bisq.support.alert.AuthorizedAlertData;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.role.RoleRegistrationService;
import bisq.user.role.RoleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class SendNotificationController implements Controller {
    @Getter
    private final SendNotificationView view;
    private final SendNotificationModel model;
    private final AlertService alertService;
    private final UserIdentityService userIdentityService;
    private final RoleRegistrationService roleRegistrationService;
    private Pin userIdentityPin;

    public SendNotificationController(DefaultApplicationService applicationService) {
        alertService = applicationService.getSupportService().getAlertService();
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        roleRegistrationService = applicationService.getUserService().getRoleRegistrationService();
        model = new SendNotificationModel();
        view = new SendNotificationView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSelectedAlertType().set(AlertType.INFO);
        model.getAlertTypes().setAll(AlertType.values());
        
        model.getSendButtonDisabled().bind(model.getMessage().isEmpty()
                .or(model.getPublicKey().isEmpty()
                        .or(model.getPrivateKey().isEmpty())));

        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> {
            model.setUserIdentity(userIdentity);
            UserProfile userProfile = userIdentity.getUserProfile();
            String userProfileId = userProfile.getId();
            model.getSelectedProfileUserName().set(userProfile.getUserName());
            if (DevMode.isDevMode()) {
                // Keypair matching pubKey from DevMode.AUTHORIZED_DEV_PUBLIC_KEYS
                String privateKeyAsHex = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
                model.getPrivateKey().set(privateKeyAsHex);
                String publicKeyAsHex = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
                model.getPublicKey().set(publicKeyAsHex);
                try {
                    PrivateKey privateKey = KeyGeneration.generatePrivate(Hex.decode(privateKeyAsHex));
                    PublicKey publicKey = KeyGeneration.generatePublic(Hex.decode(publicKeyAsHex));
                    KeyPair keyPair = new KeyPair(publicKey, privateKey);
                    model.setKeyPair(keyPair);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            } else {
                KeyPair keyPair = roleRegistrationService.findOrCreateRoleRegistrationKey(RoleType.SECURITY_MANAGER, userProfileId);
                model.setKeyPair(keyPair);
                model.getPrivateKey().set(Hex.encode(keyPair.getPrivate().getEncoded()));
                String publicKeyAsHex = Hex.encode(keyPair.getPublic().getEncoded());
                model.getPublicKey().set(publicKeyAsHex);
            }
        });
    }


    @Override
    public void onDeactivate() {
        model.getSendButtonDisabled().unbind();
        userIdentityPin.unbind();
    }

    public void onSendAlert() {
        String message = model.getMessage().get();
        if (message.length() > AuthorizedAlertData.MAX_MESSAGE_LENGTH) {
            new Popup().warning(Res.get("user.sendAlert.message.tooLong")).show();
            return;
        }
        try {
            AlertType alertType = model.getSelectedAlertType().get();
            AuthorizedAlertData alert = new AuthorizedAlertData(StringUtils.createUid(), message, new Date().getTime(), alertType);
            UserIdentity selectedUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            alertService.publishAlert(selectedUserIdentity.getNodeIdAndKeyPair(),
                    alert,
                    model.getPrivateKey().get(),
                    model.getPublicKey().get());
        } catch (Exception e) {
            new Popup().error(e).show();
        }
    }
}
