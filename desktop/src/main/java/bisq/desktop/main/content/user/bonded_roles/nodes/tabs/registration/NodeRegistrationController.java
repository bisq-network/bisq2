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

package bisq.desktop.main.content.user.bonded_roles.nodes.tabs.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.encoding.Hex;
import bisq.common.util.ExceptionUtil;
import bisq.common.util.FileUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.main.content.user.bonded_roles.tabs.registration.BondedRolesRegistrationController;
import bisq.desktop.main.content.user.bonded_roles.tabs.registration.BondedRolesRegistrationModel;
import bisq.desktop.main.content.user.bonded_roles.tabs.registration.BondedRolesRegistrationView;
import bisq.network.p2p.node.transport.TransportType;
import bisq.network.p2p.vo.Address;
import bisq.network.p2p.vo.AddressByTransportTypeMap;
import bisq.user.identity.UserIdentity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NodeRegistrationController extends BondedRolesRegistrationController {
    private Subscription addressInfoPin;

    public NodeRegistrationController(ServiceProvider serviceProvider, BondedRoleType bondedRoleType) {
        super(serviceProvider, bondedRoleType);
    }

    @Override
    protected BondedRolesRegistrationModel createAndGetModel() {
        return new NodeRegistrationModel(bondedRoleType);
    }

    @Override
    protected BondedRolesRegistrationView<NodeRegistrationModel, NodeRegistrationController> createAndGetView() {
        UserProfileSelection userProfileSelection = new UserProfileSelection(serviceProvider, 20, true);
        return new NodeRegistrationView((NodeRegistrationModel) model, this, userProfileSelection);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        addressInfoPin = EasyBind.subscribe(getNodesRegistrationModel().getAddressInfoJson(), addressInfo -> {
            model.getAddressByNetworkType().clear();
            if (addressInfo != null) {
                model.getAddressByNetworkType().putAll(addressByNetworkTypeFromJson(addressInfo));
            }
        });
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        getNodesRegistrationModel().getPubKey().set(null);
        getNodesRegistrationModel().getPrivKey().set(null);
        getNodesRegistrationModel().getShowKeyPair().set(false);
        addressInfoPin.unsubscribe();
    }

    void onImportNodeAddress() {
        Path path = serviceProvider.getConfig().getBaseDir();
        File file = FileChooserUtil.openFile(getView().getRoot().getScene(), path.toAbsolutePath().toString());
        if (file != null) {
            try {
                String json = FileUtils.readStringFromFile(file);
                checkArgument(StringUtils.isNotEmpty(json));
                getNodesRegistrationModel().getAddressInfoJson().set(json);
            } catch (Exception e) {
                new Popup().error(e).show();
            }
        }
    }

    void onShowKeyPair() {
        getNodesRegistrationModel().getPubKey().set(null);
        getNodesRegistrationModel().getPrivKey().set(null);

        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentityObservable().get();
        KeyPair keyPair = userIdentity.getNodeIdAndKeyPair().getKeyPair();
        getNodesRegistrationModel().getPubKey().set(Hex.encode(keyPair.getPublic().getEncoded()));
        getNodesRegistrationModel().getPrivKey().set(Hex.encode(keyPair.getPrivate().getEncoded()));
        getNodesRegistrationModel().getShowKeyPair().set(true);
    }

    @Override
    protected void applyRequestRegistrationButtonDisabledBinding() {
        model.getRequestButtonDisabled().bind(model.getBondUserName().isEmpty()
                .or(getNodesRegistrationModel().getAddressInfoJson().isEmpty())
                .or(model.getSignature().isEmpty())
                .or(getNodesRegistrationModel().getJsonValid().not()));
    }

    private AddressByTransportTypeMap addressByNetworkTypeFromJson(String json) {
        try {
            Type type = new TypeToken<HashMap<TransportType, Address>>() {
            }.getType();
            AddressByTransportTypeMap map = new Gson().fromJson(json, type);
            getNodesRegistrationModel().getJsonValid().set(true);
            return map;
        } catch (Exception e) {
            log.error(ExceptionUtil.print(e));
            getNodesRegistrationModel().getJsonValid().set(false);
            return new AddressByTransportTypeMap();
        }
    }

    private NodeRegistrationModel getNodesRegistrationModel() {
        return (NodeRegistrationModel) model;
    }
}
