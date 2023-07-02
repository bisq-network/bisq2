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

package bisq.desktop.primary.main.content.user.nodes.tabs.registration;

import bisq.application.DefaultApplicationService;
import bisq.common.application.DevMode;
import bisq.common.encoding.Hex;
import bisq.common.observable.Pin;
import bisq.common.util.FileUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.common.Browser;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.security.KeyGeneration;
import bisq.user.identity.UserIdentityService;
import bisq.user.node.AuthorizedNodeRegistrationData;
import bisq.user.node.NodeRegistrationService;
import bisq.user.node.NodeType;
import bisq.user.profile.UserProfile;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;
import org.fxmisc.easybind.monadic.MonadicBinding;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NodeRegistrationController implements Controller {
    @Getter
    private final NodeRegistrationView view;
    private final DefaultApplicationService applicationService;
    private final NodeRegistrationModel model;
    private final UserIdentityService userIdentityService;
    private final NodeRegistrationService nodeRegistrationService;
    private Pin userIdentityPin;
    private Subscription updateRegistrationStatePin;

    public NodeRegistrationController(DefaultApplicationService applicationService, NodeType nodeType) {
        userIdentityService = applicationService.getUserService().getUserIdentityService();
        nodeRegistrationService = applicationService.getUserService().getNodeRegistrationService();
        this.applicationService = applicationService;
        model = new NodeRegistrationModel(nodeType);
        view = new NodeRegistrationView(model, this);
    }

    @Override
    public void onActivate() {
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
                KeyPair keyPair = nodeRegistrationService.findOrCreateNodeRegistrationKey(model.getNodeType(), userProfileId);
                model.setKeyPair(keyPair);
                model.getPrivateKey().set(Hex.encode(keyPair.getPrivate().getEncoded()));
                String publicKeyAsHex = Hex.encode(keyPair.getPublic().getEncoded());
                model.getPublicKey().set(publicKeyAsHex);
            }

            updateRegistrationState();
        });

        MonadicBinding<Boolean> binding = EasyBind.combine(model.getPrivateKey(), model.getPublicKey(), model.getAddressInfo(),
                (priv, pub, address) ->
                        StringUtils.isNotEmpty(priv) &&
                                StringUtils.isNotEmpty(pub) &&
                                StringUtils.isNotEmpty(address));
        updateRegistrationStatePin = EasyBind.subscribe(binding, e -> updateRegistrationState());
        updateRegistrationState();
    }

    @Override
    public void onDeactivate() {
        userIdentityPin.unbind();
        updateRegistrationStatePin.unsubscribe();
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisq2/nodes/" + model.getNodeType().name().toLowerCase());
    }

    void onRegister() {
        nodeRegistrationService.registerNode(model.getUserIdentity(),
                        model.getNodeType(),
                        model.getKeyPair(),
                        model.getAddressByNetworkType())
                .whenComplete((result, throwable) -> {
                    UIThread.run(() -> {
                        updateRegistrationState();
                        if (throwable == null) {
                            new Popup().feedback(Res.get("user.registration.success")).show();
                        } else {
                            new Popup().warning(Res.get("user.registration.failed", throwable.getMessage())).show();
                        }
                    });
                });
    }

    void onRemoveRegistration() {
        nodeRegistrationService.removeNodeRegistration(model.getUserIdentity(),
                        model.getNodeType(),
                        model.getPublicKey().get())
                .whenComplete((result, throwable) -> {
                    UIThread.run(() -> {
                        updateRegistrationState();
                        if (throwable == null) {
                            new Popup().feedback(Res.get("user.removeRegistration.success")).show();
                        } else {
                            new Popup().warning(Res.get("user.removeRegistration.failed", throwable.getMessage())).show();
                        }
                    });
                });
    }

    void onCopy() {
        ClipboardUtil.copyToClipboard(model.getPublicKey().get());
    }

    void onImportNodeAddress() {
        Path path = Path.of(applicationService.getConfig().getBaseDir());
        File file = FileChooserUtil.openFile(getView().getRoot().getScene(), path.toAbsolutePath().toString());
        if (file != null) {
            try {
                String json = FileUtils.readFromFile(file);
                checkArgument(StringUtils.isNotEmpty(json));
                Type type = new TypeToken<HashMap<Transport.Type, Address>>() {
                }.getType();
                Map<Transport.Type, Address> addressByNetworkType = new Gson().fromJson(json, type);
                model.setAddressByNetworkType(addressByNetworkType);
                model.getAddressInfo().set(addressByNetworkTypeToDisplayString(addressByNetworkType));
                updateRegistrationState();
            } catch (Exception e) {
                new Popup().error(e).show();
            }
        }
    }

    private void updateRegistrationState() {
        String publicKeyAsHex = model.getPublicKey().get();
        boolean isAuthorizedPublicKey = DevMode.isDevMode() ? DevMode.AUTHORIZED_DEV_PUBLIC_KEYS.contains(publicKeyAsHex) :
                AuthorizedNodeRegistrationData.authorizedPublicKeys.contains(publicKeyAsHex);
        boolean isNodeRegistered = nodeRegistrationService.isNodeRegistered(model.getUserIdentity().getUserProfile().getId(),
                model.getNodeType(),
                publicKeyAsHex);
        model.getRegistrationDisabled().set(!isAuthorizedPublicKey ||
                !StringUtils.isNotEmpty(model.getPrivateKey().get()) ||
                model.getAddressByNetworkType() == null);
        model.getRemoveRegistrationVisible().set(isNodeRegistered);
    }

    public static String addressByNetworkTypeToDisplayString(Map<Transport.Type, Address> addressByNetworkType) {
        List<String> list = addressByNetworkType.entrySet().stream()
                .map(e -> e.getKey().name() + ": " + e.getValue().getFullAddress())
                .collect(Collectors.toList());
        return Joiner.on("\n").join(list);
    }
}
