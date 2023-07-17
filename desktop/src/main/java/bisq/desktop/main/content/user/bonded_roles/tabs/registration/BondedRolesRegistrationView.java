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

package bisq.desktop.main.content.user.bonded_roles.tabs.registration;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.desktop.components.controls.OrderedList;
import bisq.desktop.main.content.components.MaterialUserProfileSelection;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class BondedRolesRegistrationView<M extends BondedRolesRegistrationModel, C extends BondedRolesRegistrationController> extends View<VBox, M, C> {
    protected final Hyperlink learnMore;
    protected final MaterialTextField bondHolderName, profileId, signature;
    protected final Button requestRegistrationButton, requestCancellationButton;
    protected final Label aboutHeadline, howHeadline;
    protected final HBox headerHBox, buttons;
    protected final MultiLineLabel aboutInfo;
    protected final OrderedList howInfo;
    private final Hyperlink showInfo, hideInfo;
    protected Subscription isCollapsedPin;

    public BondedRolesRegistrationView(M model,
                                       C controller,
                                       UserProfileSelection userProfileSelection) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        String bondedRoleType = "user.bondedRoles.type." + model.getBondedRoleType().name();
        String inlineAbout = Res.get(bondedRoleType + ".about.inline");
        aboutHeadline = new Label(Res.get("user.bondedRoles.registration.about.headline", inlineAbout));
        aboutHeadline.getStyleClass().add("user-bonded-roles-info-headline");

        showInfo = new Hyperlink(Res.get("user.bondedRoles.registration.showInfo"));
        hideInfo = new Hyperlink(Res.get("user.bondedRoles.registration.hideInfo"));

        HBox.setMargin(aboutHeadline, new Insets(2.5, 0, 0, -2));
        headerHBox = new HBox(aboutHeadline, Spacer.fillHBox(), hideInfo, showInfo);
        headerHBox.setCursor(Cursor.HAND);
        BisqTooltip tooltip = new BisqTooltip(Res.get("action.expandOrCollapse"));
        tooltip.setStyle("-fx-show-delay: 500ms;");
        Tooltip.install(headerHBox, tooltip);

        aboutInfo = new MultiLineLabel(Res.get(bondedRoleType + ".about.info"));
        aboutInfo.getStyleClass().add("user-bonded-roles-info-text");


        String inlineHow = Res.get(bondedRoleType + ".how.inline");
        howHeadline = new Label(Res.get("user.bondedRoles.registration.how.headline", inlineHow));
        howHeadline.getStyleClass().add("user-bonded-roles-info-headline");

        String typeSpecific = model.getBondedRoleType().isRole() ? Res.get("user.bondedRoles.registration.how.info.role") :
                Res.get("user.bondedRoles.registration.how.info.node");
        String howInfoString = Res.get("user.bondedRoles.registration.how.info", inlineHow, typeSpecific);
        howInfo = new OrderedList(howInfoString, "user-bonded-roles-info-text");


        Label registerHeadline = new Label(Res.get("user.bondedRoles.registration.headline"));
        registerHeadline.getStyleClass().add("user-bonded-roles-info-headline");

        MaterialUserProfileSelection materialUserProfileSelection = new MaterialUserProfileSelection(userProfileSelection, Res.get("user.bondedRoles.userProfile.select"));

        profileId = new MaterialTextField(Res.get("user.bondedRoles.registration.profileId"), "");
        profileId.setEditable(false);
        profileId.showCopyIcon();

        bondHolderName = new MaterialTextField(Res.get("user.bondedRoles.registration.bondHolderName"), Res.get("user.bondedRoles.registration.bondHolderName.prompt"));
        signature = new MaterialTextField(Res.get("user.bondedRoles.registration.signature"), Res.get("user.bondedRoles.registration.signature.prompt"));

        requestRegistrationButton = new Button(Res.get("user.bondedRoles.registration.requestRegistration"));
        requestRegistrationButton.setDefaultButton(true);

        requestCancellationButton = new Button(Res.get("user.bondedRoles.cancellation.requestCancellation"));

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        buttons = new HBox(20, requestRegistrationButton, requestCancellationButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headerHBox, new Insets(10, 0, 0, 0));
        VBox.setMargin(howHeadline, new Insets(20, 0, 0, 0));
        VBox.setMargin(registerHeadline, new Insets(20, 0, 0, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox.setVgrow(aboutInfo, Priority.ALWAYS);
        VBox.setVgrow(howHeadline, Priority.ALWAYS);
        root.getChildren().addAll(headerHBox, aboutInfo,
                howHeadline, howInfo,
                registerHeadline, materialUserProfileSelection, profileId, bondHolderName, signature,
                buttons);
    }

    @Override
    protected void onViewAttached() {
        bondHolderName.textProperty().bindBidirectional(model.getBondUserName());
        signature.textProperty().bindBidirectional(model.getSignature());
        profileId.textProperty().bind(model.getProfileId());
        requestRegistrationButton.disableProperty().bind(model.getRequestButtonDisabled());
        requestCancellationButton.disableProperty().bind(model.getRequestButtonDisabled());
        requestCancellationButton.visibleProperty().bind(model.getRequestCancellationButtonVisible());
        requestCancellationButton.managedProperty().bind(model.getRequestCancellationButtonVisible());
        profileId.getIconButton().setOnAction(e -> controller.onCopyToClipboard());
        learnMore.setOnAction(e -> controller.onLearnMore());
        requestRegistrationButton.setOnAction(e -> controller.onRequestAuthorization());
        requestCancellationButton.setOnAction(e -> controller.onRequestCancellation());
        hideInfo.setOnMouseClicked(e -> controller.onCollapse());
        showInfo.setOnMouseClicked(e -> controller.onExpand());
        headerHBox.setOnMouseClicked(e -> controller.onHeaderClicked());
        isCollapsedPin = EasyBind.subscribe(model.getIsCollapsed(),
                isCollapsed -> {
                    VBox.setMargin(headerHBox, new Insets(0, 0, isCollapsed ? -50 : 0, 0));
                    aboutHeadline.setVisible(!isCollapsed);
                    aboutInfo.setVisible(!isCollapsed);
                    howHeadline.setVisible(!isCollapsed);
                    howInfo.setVisible(!isCollapsed);
                    hideInfo.setVisible(!isCollapsed);
                    showInfo.setVisible(isCollapsed);

                    aboutHeadline.setManaged(!isCollapsed);
                    aboutInfo.setManaged(!isCollapsed);
                    howHeadline.setManaged(!isCollapsed);
                    howInfo.setManaged(!isCollapsed);
                    hideInfo.setManaged(!isCollapsed);
                    showInfo.setManaged(isCollapsed);
                });

    }

    @Override
    protected void onViewDetached() {
        bondHolderName.textProperty().unbindBidirectional(model.getBondUserName());
        signature.textProperty().unbindBidirectional(model.getSignature());
        profileId.textProperty().unbind();
        requestRegistrationButton.disableProperty().unbind();
        requestCancellationButton.disableProperty().unbind();
        requestCancellationButton.visibleProperty().unbind();
        requestCancellationButton.managedProperty().unbind();

        profileId.getIconButton().setOnAction(null);
        learnMore.setOnAction(null);
        requestRegistrationButton.setOnAction(null);
        requestCancellationButton.setOnAction(null);
        hideInfo.setOnMouseClicked(null);
        showInfo.setOnMouseClicked(null);
        headerHBox.setOnMouseClicked(null);

        isCollapsedPin.unsubscribe();
    }
}
