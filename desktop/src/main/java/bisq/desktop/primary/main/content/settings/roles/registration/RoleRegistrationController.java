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

package bisq.desktop.primary.main.content.settings.roles.registration;

import bisq.application.DefaultApplicationService;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.security.KeyGeneration;
import bisq.user.identity.UserIdentityService;
import bisq.user.role.AuthorizedRoleRegistrationData;
import bisq.user.role.RoleRegistrationService;
import bisq.user.role.RoleType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
public class RoleRegistrationController implements Controller {
    @Getter
    private final RoleRegistrationView view;
    private final RoleRegistrationModel model;
    private final UserIdentityService userIdentityService;
    private final RoleRegistrationService roleRegistrationService;
    private Pin userIdentityPin;

    public RoleRegistrationController(DefaultApplicationService applicationService, RoleType roleType) {
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        roleRegistrationService = applicationService.getUserService().getRoleRegistrationService();
        model = new RoleRegistrationModel(roleType);
        view = new RoleRegistrationView(model, this);
    }

    @Override
    public void onActivate() {
        userIdentityPin = userIdentityService.getSelectedUserIdentity().addObserver(userIdentity -> {
            model.getSelectedProfile().set(userIdentity.getUserProfile().getUserName());
            String publicKeyAsHex;
            if (DevMode.isDevMode()) {
                // Keypair matching pubKey from DevMode.AUTHORIZED_DEV_PUBLIC_KEYS
                String privateKeyAsHex = "30818d020100301006072a8648ce3d020106052b8104000a0476307402010104205b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
                model.getPrivateKey().set(privateKeyAsHex);
                publicKeyAsHex = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8afa8e8091";
                model.getPublicKey().set(publicKeyAsHex);
                try {
                    PrivateKey privateKey = KeyGeneration.generatePrivate(Hex.decode(privateKeyAsHex));
                    PublicKey publicKey = KeyGeneration.generatePublic(Hex.decode(publicKeyAsHex));
                    KeyPair keyPair = new KeyPair(publicKey, privateKey);
                    model.setKeyPair(keyPair);
                    model.getRegistrationDisabled().set(!DevMode.AUTHORIZED_DEV_PUBLIC_KEYS.contains(publicKeyAsHex));
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            } else {
                KeyPair keyPair = roleRegistrationService.findOrCreateRegistrationKey(model.getRoleType(), userIdentity.getUserProfile().getId());
                model.setKeyPair(keyPair);
                model.getPrivateKey().set(Hex.encode(keyPair.getPrivate().getEncoded()));
                publicKeyAsHex = Hex.encode(keyPair.getPublic().getEncoded());
                model.getPublicKey().set(publicKeyAsHex);
                model.getRegistrationDisabled().set(!AuthorizedRoleRegistrationData.authorizedPublicKeys.contains(publicKeyAsHex));
            }
            model.getPrivateKeyDisplay().set("•••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••");
        });
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisq2/roles/" + model.getRoleType().name().toLowerCase());
    }

    void onRegister() {
        roleRegistrationService.registerAgent(userIdentityService.getSelectedUserIdentity().get(),
                        model.getRoleType(),
                        model.getKeyPair())
                .whenComplete((result, throwable) -> {
                    UIThread.run(() -> {
                        if (throwable == null) {
                            new Popup().feedback(Res.get("roles.registration.success")).show();
                        } else {
                            new Popup().warning(Res.get("roles.registration.failed", throwable.getMessage())).show();
                        }
                    });
                });
    }

    void onCopy() {
        ClipboardUtil.copyToClipboard(model.getPublicKey().get());
    }

    void onShowPrivateKey() {
        model.getPrivateKeyDisplay().set(model.getPrivateKey().get());
    }
}
