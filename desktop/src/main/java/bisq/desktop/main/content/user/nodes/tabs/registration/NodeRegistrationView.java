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

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeRegistrationView extends View<VBox, NodeRegistrationModel, NodeRegistrationController> {
    private final Hyperlink learnMore;
    private final MaterialTextField bondHolderName, profileId;
    private final Button requestRegistrationButton, importNodeAddressButton;
    private final MaterialTextArea addressInfoJson;
    
  /*  private final Button importNodeAddressButton, registrationButton, removeRegistrationButton;
    private final Hyperlink learnMore;
    private final MaterialTextField selectedProfile, publicKey;
    private final MaterialPasswordField privateKey;
    private final MaterialTextArea addressInfoJson;*/

    public NodeRegistrationView(NodeRegistrationModel model,
                                NodeRegistrationController controller,
                                Pane userProfileSelection) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        String nodeType = model.getNodeType().name();

        String inlineAbout = Res.get("user.nodes.type." + nodeType + ".inline.about");
        Label aboutHeadline = new Label(Res.get("user.registration.headline.about", inlineAbout));
        aboutHeadline.getStyleClass().add("bisq-text-headline-2");

        MultiLineLabel aboutInfo = new MultiLineLabel(Res.get("user.nodes.registration.info.about." + nodeType));
        aboutInfo.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        String inlineHow = Res.get("user.nodes.type." + nodeType + ".inline.how");
        Label howHeadline = new Label(Res.get("user.registration.headline.how", inlineHow));
        howHeadline.getStyleClass().add("bisq-text-headline-2");

        MultiLineLabel howInfo = new MultiLineLabel(Res.get("user.registration.info.how", inlineHow, Res.get("user.nodes.registration.node.info.how")));
        howInfo.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        Label userProfileSelectLabel = new Label(Res.get("user.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);


        profileId = new MaterialTextField(Res.get("user.registration.profileId"), "");
        profileId.setEditable(false);
        profileId.setIcon(AwesomeIcon.COPY);
        profileId.setIconTooltip(Res.get("action.copyToClipboard"));

        bondHolderName = new MaterialTextField(Res.get("user.registration.bondHolderName"), Res.get("user.registration.bondHolderName.prompt"));

        addressInfoJson = new MaterialTextArea(Res.get("user.nodes.registration.node.addressInfo"));

        requestRegistrationButton = new Button(Res.get("user.registration.requestRegistration"));
        requestRegistrationButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        importNodeAddressButton = new Button(Res.get("user.nodes.registration.node.importAddress"));
        importNodeAddressButton.getStyleClass().add("outlined-button");
        HBox buttons = new HBox(20, importNodeAddressButton, requestRegistrationButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(aboutHeadline, new Insets(10, 0, 0, 0));
        VBox.setMargin(howHeadline, new Insets(20, 0, 0, 0));
        VBox.setMargin(howInfo, new Insets(0, 0, 10, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, 40, 0));
        VBox.setVgrow(aboutInfo, Priority.ALWAYS);
        VBox.setVgrow(howHeadline, Priority.ALWAYS);
        root.getChildren().addAll(aboutHeadline, aboutInfo, howHeadline, howInfo, userProfileSelectLabel,
                userProfileSelection, profileId, bondHolderName, addressInfoJson, buttons);
        
       /* 

        removeRegistrationButton = new Button(Res.get("user.registration.removeRegistration"));

        */
    }

    @Override
    protected void onViewAttached() {
        bondHolderName.textProperty().bindBidirectional(model.getBondUserName());
        profileId.textProperty().bind(model.getProfileId());
        addressInfoJson.textProperty().bindBidirectional(model.getAddressInfoJson());
        requestRegistrationButton.disableProperty().bind(model.getRequestRegistrationButtonDisabled());
        //  importNodeAddressButton.visibleProperty().bind(model.getImportNodeAddressButtonVisible());
        //  importNodeAddressButton.managedProperty().bind(model.getImportNodeAddressButtonVisible());

        requestRegistrationButton.setOnAction(e -> controller.onRequestAuthorization());
        importNodeAddressButton.setOnAction(e -> controller.onImportNodeAddress());
        profileId.getIconButton().setOnAction(e -> controller.onCopyToClipboard());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        bondHolderName.textProperty().unbindBidirectional(model.getBondUserName());
        profileId.textProperty().unbind();
        addressInfoJson.textProperty().unbindBidirectional(model.getAddressInfoJson());
        requestRegistrationButton.disableProperty().unbind();
        importNodeAddressButton.visibleProperty().unbind();
        importNodeAddressButton.managedProperty().unbind();


        requestRegistrationButton.setOnAction(null);
        importNodeAddressButton.setOnAction(null);
        profileId.getIconButton().setOnAction(null);
        learnMore.setOnAction(null);
    }
  /*  @Override
    protected void onViewAttached() {
        selectedProfile.textProperty().bind(model.getSelectedProfileUserName());
        privateKey.textProperty().bindBidirectional(model.getPrivateKey());
        publicKey.textProperty().bindBidirectional(model.getPublicKey());
        addressInfoJson.textProperty().bindBidirectional(model.getAddressInfoJson());
        registrationButton.disableProperty().bind(model.getRegistrationDisabled());
        registrationButton.managedProperty().bind(model.getRemoveRegistrationVisible().not());
        registrationButton.visibleProperty().bind(model.getRemoveRegistrationVisible().not());
        importNodeAddressButton.visibleProperty().bind(model.getRemoveRegistrationVisible().not());
        importNodeAddressButton.managedProperty().bind(model.getRemoveRegistrationVisible().not());
        removeRegistrationButton.managedProperty().bind(model.getRemoveRegistrationVisible());
        removeRegistrationButton.visibleProperty().bind(model.getRemoveRegistrationVisible());

        importNodeAddressButton.setOnAction(e -> controller.onImportNodeAddress());
        registrationButton.setOnAction(e -> controller.onRegister());
        removeRegistrationButton.setOnAction(e -> controller.onRemoveRegistration());
        learnMore.setOnAction(e -> controller.onLearnMore());
        publicKey.getIconButton().setOnAction(e -> controller.onCopy());
    }

    @Override
    protected void onViewDetached() {
        selectedProfile.textProperty().unbind();
        privateKey.textProperty().unbindBidirectional(model.getPrivateKey());
        publicKey.textProperty().unbindBidirectional(model.getPublicKey());
        addressInfoJson.textProperty().unbindBidirectional(model.getAddressInfoJson());
        registrationButton.disableProperty().unbind();
        registrationButton.managedProperty().unbind();
        registrationButton.visibleProperty().unbind();
        importNodeAddressButton.managedProperty().unbind();
        importNodeAddressButton.visibleProperty().unbind();
        removeRegistrationButton.managedProperty().unbind();
        removeRegistrationButton.visibleProperty().unbind();

        importNodeAddressButton.setOnAction(null);
        registrationButton.setOnAction(null);
        removeRegistrationButton.setOnAction(null);
        learnMore.setOnAction(null);
        publicKey.getIconButton().setOnAction(null);
    }*/
}
