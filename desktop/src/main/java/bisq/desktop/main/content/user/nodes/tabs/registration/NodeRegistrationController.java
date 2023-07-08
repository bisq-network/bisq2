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

package bisq.desktop.main.content.user.nodes.tabs.registration;

import bisq.common.observable.Pin;
import bisq.common.util.FileUtils;
import bisq.common.util.StringUtils;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Browser;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.components.overlay.Overlay;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.user.identity.UserIdentityService;
import bisq.user.node.NodeRegistrationService;
import bisq.user.node.NodeType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class NodeRegistrationController implements Controller {
    @Getter
    private final NodeRegistrationView view;
    private final ServiceProvider serviceProvider;
    private final NodeRegistrationModel model;
    private final UserIdentityService userIdentityService;
    private final NodeRegistrationService nodeRegistrationService;
    private Pin selectedUserProfilePin;

    public NodeRegistrationController(ServiceProvider serviceProvider, NodeType nodeType) {
        this.serviceProvider = serviceProvider;
        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        nodeRegistrationService = serviceProvider.getUserService().getNodeRegistrationService();

        UserProfileSelection userProfileSelection = new UserProfileSelection(userIdentityService);
        model = new NodeRegistrationModel(nodeType);
        view = new NodeRegistrationView(model, this, userProfileSelection.getRoot());
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.subscribe(userIdentityService.getSelectedUserIdentityObservable(),
                chatUserIdentity -> {
                    model.getSelectedChatUserIdentity().set(chatUserIdentity);
                    model.getProfileId().set(chatUserIdentity.getId());
                }
        );

        model.getRequestRegistrationButtonDisabled().bind(model.getBondUserName().isEmpty().or(model.getAddressInfoJson().isEmpty()));
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
        model.getRequestRegistrationButtonDisabled().unbind();
    }

    void onRequestAuthorization() {
        ClipboardUtil.getClipboardString().ifPresent(signature -> {
            boolean success = nodeRegistrationService.requestAuthorization(model.getProfileId().get(),
                    model.getNodeType(),
                    model.getBondUserName().get(),
                    signature,
                    addressByNetworkTypeFromJson(model.getAddressInfoJson().get()));
            if (success) {
                new Popup().information(Res.get("user.reputation.request.success"))
                        .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                        .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                        .show();
            } else {
                new Popup().warning(Res.get("user.reputation.request.error", StringUtils.truncate(signature)))
                        .animationType(Overlay.AnimationType.SlideDownFromCenterTop)
                        .transitionsType(Transitions.Type.LIGHT_BLUR_LIGHT)
                        .show();
            }
        });
    }

    void onImportNodeAddress() {
        Path path = Path.of(serviceProvider.getConfig().getBaseDir());
        File file = FileChooserUtil.openFile(getView().getRoot().getScene(), path.toAbsolutePath().toString());
        if (file != null) {
            try {
                String json = FileUtils.readFromFile(file);
                checkArgument(StringUtils.isNotEmpty(json));
                model.getAddressInfoJson().set(json);
            } catch (Exception e) {
                new Popup().error(e).show();
            }
        }
    }

    void onLearnMore() {
        Browser.open("https://bisq.wiki/bisq2/nodes/" + model.getNodeType().name().toLowerCase());
    }

    void onCopyToClipboard() {
        ClipboardUtil.copyToClipboard(model.getProfileId().get());
    }


    private Map<Transport.Type, Address> addressByNetworkTypeFromJson(String json) {
        Type type = new TypeToken<HashMap<Transport.Type, Address>>() {
        }.getType();
        return new Gson().fromJson(json, type);
    }
}
