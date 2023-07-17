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

import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.main.content.user.bonded_roles.tabs.registration.BondedRolesRegistrationView;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRegistrationView extends BondedRolesRegistrationView<NodeRegistrationModel, NodeRegistrationController> {
    private final MaterialTextArea addressInfoJson;
    private final Button importNodeAddressButton, showKeyPairButton;
    private final MaterialTextField pubKey;
    private final MaterialPasswordField privKey;

    public NodeRegistrationView(NodeRegistrationModel model,
                                NodeRegistrationController controller,
                                UserProfileSelection userProfileSelection) {
        super(model, controller, userProfileSelection);
        addressInfoJson = new MaterialTextArea(Res.get("user.bondedRoles.registration.node.addressInfo"), Res.get("user.bondedRoles.registration.node.addressInfo.prompt"));

        pubKey = new MaterialTextField(Res.get("user.bondedRoles.registration.node.pubKey"));
        privKey = new MaterialPasswordField(Res.get("user.bondedRoles.registration.node.privKey"));

        importNodeAddressButton = new Button(Res.get("user.bondedRoles.registration.node.importAddress"));
        importNodeAddressButton.getStyleClass().add("outlined-button");

        showKeyPairButton = new Button(Res.get("user.bondedRoles.registration.node.showKeyPair"));

        buttons.getChildren().add(0, importNodeAddressButton);
        buttons.getChildren().add(2, showKeyPairButton);

        root.getChildren().add(root.getChildren().size() - 1, addressInfoJson);
        root.getChildren().add(root.getChildren().size() - 1, pubKey);
        root.getChildren().add(root.getChildren().size() - 1, privKey);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        addressInfoJson.textProperty().bindBidirectional(model.getAddressInfoJson());
        pubKey.textProperty().bind(model.getPubKey());
        pubKey.visibleProperty().bind(model.getShowKeyPair());
        pubKey.managedProperty().bind(model.getShowKeyPair());
        privKey.textProperty().bindBidirectional(model.getPrivKey());
        privKey.visibleProperty().bind(model.getShowKeyPair());
        privKey.managedProperty().bind(model.getShowKeyPair());

        importNodeAddressButton.setOnAction(e -> controller.onImportNodeAddress());
        showKeyPairButton.setOnAction(e -> controller.onShowKeyPair());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        addressInfoJson.textProperty().unbindBidirectional(model.getAddressInfoJson());
        pubKey.textProperty().unbind();
        pubKey.visibleProperty().unbind();
        pubKey.managedProperty().unbind();
        privKey.textProperty().unbindBidirectional(model.getPrivKey());
        privKey.visibleProperty().unbind();
        privKey.managedProperty().unbind();


        importNodeAddressButton.setOnAction(null);
        showKeyPairButton.setOnAction(null);
    }
}
