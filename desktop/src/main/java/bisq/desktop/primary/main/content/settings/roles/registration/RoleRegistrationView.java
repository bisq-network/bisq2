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

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoleRegistrationView extends View<VBox, RoleRegistrationModel, RoleRegistrationController> {
    private final Button copyButton, registrationButton, removeRegistrationButton;
    private final Hyperlink learnMore;
    private final Label info;
    private final MaterialTextField selectedProfile, publicKey;
    private final MaterialPasswordField privateKey;


    public RoleRegistrationView(RoleRegistrationModel model,
                                RoleRegistrationController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        String role = Res.get("roles.type." + model.getRoleType().name() + ".inline");
        info = new MultiLineLabel(Res.get("roles.registration.info", role, role));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        selectedProfile = new MaterialTextField(Res.get("roles.registration.selectedProfile"));
        selectedProfile.setEditable(false);

        privateKey = new MaterialPasswordField(Res.get("roles.registration.privateKey"));
        privateKey.setEditable(false);

        publicKey = new MaterialTextField(Res.get("roles.registration.publicKey"));
        publicKey.setEditable(false);

        registrationButton = new Button(Res.get("roles.registration.register"));
        removeRegistrationButton = new Button(Res.get("roles.registration.removeRegistration"));
        copyButton = new Button(Res.get("roles.registration.copyPubKey"));
        copyButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("reputation.learnMore"));

        HBox buttons = new HBox(20, learnMore, Spacer.fillHBox(), registrationButton, removeRegistrationButton, copyButton);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(info, new Insets(0, 0, 10, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(info, selectedProfile, privateKey, publicKey, buttons);
    }

    @Override
    protected void onViewAttached() {
        selectedProfile.textProperty().bind(model.getSelectedProfileUserName());
        privateKey.textProperty().bindBidirectional(model.getPrivateKey());
        publicKey.textProperty().bindBidirectional(model.getPublicKey());
        registrationButton.disableProperty().bind(model.getRegistrationDisabled());
        removeRegistrationButton.managedProperty().bind(model.getRemoveRegistrationVisible());
        removeRegistrationButton.visibleProperty().bind(model.getRemoveRegistrationVisible());

        registrationButton.setOnAction(e -> controller.onRegister());
        removeRegistrationButton.setOnAction(e -> controller.onRemoveRegistration());
        learnMore.setOnAction(e -> controller.onLearnMore());
        copyButton.setOnAction(e -> controller.onCopy());
    }

    @Override
    protected void onViewDetached() {
        selectedProfile.textProperty().unbind();
        privateKey.textProperty().unbindBidirectional(model.getPrivateKey());
        publicKey.textProperty().unbindBidirectional(model.getPublicKey());
        registrationButton.disableProperty().unbind();
        removeRegistrationButton.managedProperty().unbind();
        removeRegistrationButton.visibleProperty().unbind();

        registrationButton.setOnAction(null);
        removeRegistrationButton.setOnAction(null);
        learnMore.setOnAction(null);
        copyButton.setOnAction(null);
    }
}
