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

import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.desktop.main.content.user.bonded_roles.tabs.registration.BondedRolesRegistrationView;
import bisq.i18n.Res;
import javafx.scene.control.Button;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRegistrationView extends BondedRolesRegistrationView<NodeRegistrationModel, NodeRegistrationController> {
    private final MaterialTextArea addressInfoJson;
    private final Button importNodeAddressButton;

    public NodeRegistrationView(NodeRegistrationModel model,
                                NodeRegistrationController controller,
                                UserProfileSelection userProfileSelection) {
        super(model, controller, userProfileSelection);
        addressInfoJson = new MaterialTextArea(Res.get("user.bondedRoles.registration.node.addressInfo"), Res.get("user.bondedRoles.registration.node.addressInfo.prompt"));
        importNodeAddressButton = new Button(Res.get("user.bondedRoles.registration.node.importAddress"));
        importNodeAddressButton.getStyleClass().add("outlined-button");

        buttons.getChildren().add(0, importNodeAddressButton);

        root.getChildren().add(root.getChildren().size() - 1, addressInfoJson);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        addressInfoJson.textProperty().bindBidirectional(model.getAddressInfoJson());
        importNodeAddressButton.setOnAction(e -> controller.onImportNodeAddress());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        addressInfoJson.textProperty().unbindBidirectional(model.getAddressInfoJson());
        importNodeAddressButton.setOnAction(null);
    }
}
