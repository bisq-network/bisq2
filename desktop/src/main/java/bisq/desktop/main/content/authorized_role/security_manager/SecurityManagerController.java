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

import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
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
    private Pin userIdentityPin;
    private Subscription messagePin;

    public SecurityManagerController(ServiceProvider serviceProvider) {
        securityManagerService = serviceProvider.getSupportService().getSecurityManagerService();
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        model = new SecurityManagerModel();
        view = new SecurityManagerView(model, this);
    }

    @Override
    public void onActivate() {
        model.getSelectedAlertType().set(AlertType.INFO);
        model.getAlertTypes().setAll(AlertType.values());

        userIdentityPin = userIdentityService.getSelectedUserIdentityObservable().addObserver(userIdentity -> {
            model.setUserIdentity(userIdentity);
            updateSendButtonDisabled();
        });
        messagePin = EasyBind.subscribe(model.getMessage(), message -> updateSendButtonDisabled());
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        messagePin.unsubscribe();
    }

    void onSendAlert() {
        String message = model.getMessage().get();
        if (message.length() > AuthorizedAlertData.MAX_MESSAGE_LENGTH) {
            new Popup().warning(Res.get("authorizedRole.securityManager.alert.message.tooLong")).show();
            return;
        }
        try {
            AlertType alertType = model.getSelectedAlertType().get();
            AuthorizedAlertData authorizedAlertData = new AuthorizedAlertData(StringUtils.createUid(), message, new Date().getTime(), alertType);
            UserIdentity userIdentity = checkNotNull(model.getUserIdentity());
            KeyPair keyPair = userIdentity.getIdentity().getKeyPair();
            String privateKey = Hex.encode(keyPair.getPrivate().getEncoded());
            String publicKey = Hex.encode(keyPair.getPublic().getEncoded());
            securityManagerService.publishAlert(userIdentity.getNodeIdAndKeyPair(),
                    authorizedAlertData,
                    privateKey,
                    publicKey);
        } catch (Exception e) {
            new Popup().error(e).show();
        }
    }

    private void updateSendButtonDisabled() {
        model.getSendButtonDisabled().set(model.getUserIdentity() == null ||
                StringUtils.isEmpty(model.getMessage().get()));
    }
}
