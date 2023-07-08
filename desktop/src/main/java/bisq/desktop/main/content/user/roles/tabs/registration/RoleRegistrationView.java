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

package bisq.desktop.main.content.user.roles.tabs.registration;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
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
public class RoleRegistrationView extends View<VBox, RoleRegistrationModel, RoleRegistrationController> {
    private final Hyperlink learnMore;
    private final MaterialTextField bondHolderName, profileId;
    private final Button requestRegistrationButton;

    public RoleRegistrationView(RoleRegistrationModel model,
                                RoleRegistrationController controller,
                                Pane userProfileSelection) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        String role = model.getRoleType().name();

        String inlineAbout = Res.get("user.roles.type." + role + ".inline.about");
        Label aboutHeadline = new Label(Res.get("user.registration.headline.about", inlineAbout));
        aboutHeadline.getStyleClass().add("bisq-text-headline-2");

        MultiLineLabel aboutInfo = new MultiLineLabel(Res.get("user.roles.registration.info.about." + role));
        aboutInfo.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        String inlineHow = Res.get("user.roles.type." + role + ".inline.how");
        Label howHeadline = new Label(Res.get("user.registration.headline.how", inlineHow));
        howHeadline.getStyleClass().add("bisq-text-headline-2");

        MultiLineLabel howInfo = new MultiLineLabel(Res.get("user.registration.info.how", inlineHow, Res.get("user.roles.registration.role.info.how")));
        howInfo.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        Label userProfileSelectLabel = new Label(Res.get("user.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);

        profileId = new MaterialTextField(Res.get("user.registration.profileId"), "");
        profileId.setEditable(false);
        profileId.setIcon(AwesomeIcon.COPY);
        profileId.setIconTooltip(Res.get("action.copyToClipboard"));

        bondHolderName = new MaterialTextField(Res.get("user.registration.bondHolderName"), Res.get("user.registration.bondHolderName.prompt"));

        requestRegistrationButton = new Button(Res.get("user.registration.requestRegistration"));
        requestRegistrationButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, requestRegistrationButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(aboutHeadline, new Insets(10, 0, 0, 0));
        VBox.setMargin(howHeadline, new Insets(20, 0, 0, 0));
        VBox.setMargin(howInfo, new Insets(0, 0, 10, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, 40, 0));
        VBox.setVgrow(aboutInfo, Priority.ALWAYS);
        VBox.setVgrow(howHeadline, Priority.ALWAYS);
        root.getChildren().addAll(aboutHeadline, aboutInfo, howHeadline, howInfo, userProfileSelectLabel, userProfileSelection, profileId, bondHolderName, buttons);
    }

    @Override
    protected void onViewAttached() {
        bondHolderName.textProperty().bindBidirectional(model.getBondUserName());
        profileId.textProperty().bind(model.getProfileId());
        requestRegistrationButton.disableProperty().bind(model.getRequestRegistrationButtonDisabled());

        profileId.getIconButton().setOnAction(e -> controller.onCopyToClipboard());
        learnMore.setOnAction(e -> controller.onLearnMore());
        requestRegistrationButton.setOnAction(e -> controller.onRequestAuthorization());
    }

    @Override
    protected void onViewDetached() {
        bondHolderName.textProperty().unbindBidirectional(model.getBondUserName());
        profileId.textProperty().unbind();
        requestRegistrationButton.disableProperty().unbind();

        profileId.getIconButton().setOnAction(null);
        learnMore.setOnAction(null);
        requestRegistrationButton.setOnAction(null);
    }
}
