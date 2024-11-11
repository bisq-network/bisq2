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

package bisq.desktop.main.content.user.user_card;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class UserCardView extends TabView<UserCardModel, UserCardController> {
    private final UserCardController controller;
    private UserProfileIcon userProfileIcon;
    private ReputationScoreDisplay reputationScoreDisplay;
    private Label userNickNameLabel, userNymLabel;
    private BisqMenuItem privateMsg, ignore, undoIgnore, report;
    private Button closeButton;
    private Subscription userProfilePin, reputationScorePin;

    public UserCardView(UserCardModel model, UserCardController controller) {
        super(model, controller);

        this.controller = controller;

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getStyleClass().add("user-card");

        addTab(Res.get("user.userCard.tab.overview"), NavigationTarget.USER_CARD_OVERVIEW);
        addTab(Res.get("user.userCard.tab.details"), NavigationTarget.USER_CARD_DETAILS);
//        addTab(Res.get("user.userCard.tab.offers"), NavigationTarget.USER_CARD_OFFERS);
//        addTab(Res.get("user.userCard.tab.reputation"), NavigationTarget.USER_CARD_REPUTATION);
    }

    @Override
    protected void onViewAttached() {
        ignore.visibleProperty().bind(model.getIgnoreUserSelected().not());
        ignore.managedProperty().bind(model.getIgnoreUserSelected().not());
        undoIgnore.visibleProperty().bind(model.getIgnoreUserSelected());
        undoIgnore.managedProperty().bind(model.getIgnoreUserSelected());
        report.visibleProperty().bind(model.getShouldShowReportButton());
        report.managedProperty().bind(model.getShouldShowReportButton());

        userProfilePin = EasyBind.subscribe(model.getUserProfile(), this::updateUserProfile);
        reputationScorePin = EasyBind.subscribe(model.getReputationScore(), reputationScore -> {
            if (reputationScore != null) {
                reputationScoreDisplay.setReputationScore(reputationScore);
            }
        });

        privateMsg.setOnAction(e -> controller.onSendPrivateMessage());
        ignore.setOnAction(e -> controller.onToggleIgnoreUser());
        undoIgnore.setOnAction(e -> controller.onToggleIgnoreUser());
        report.setOnAction(e -> controller.onReportUser());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        ignore.visibleProperty().unbind();
        ignore.managedProperty().unbind();
        undoIgnore.visibleProperty().unbind();
        undoIgnore.managedProperty().unbind();
        report.visibleProperty().unbind();
        report.managedProperty().unbind();

        userProfilePin.unsubscribe();
        reputationScorePin.unsubscribe();

        privateMsg.setOnAction(null);
        ignore.setOnAction(null);
        undoIgnore.setOnAction(null);
        report.setOnAction(null);
        closeButton.setOnAction(null);
    }

    @Override
    protected void setupTopBox() {
        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15-SIDE_PADDING, 0, 0));

        userProfileIcon = new UserProfileIcon(100);
        reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setScale(1.5);

        userNickNameLabel = new Label();
        userNickNameLabel.getStyleClass().addAll("text-fill-white", "large-text");
        userNymLabel = new Label();
        userNymLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");

        privateMsg = new BisqMenuItem("private-chat-grey", "private-chat-white",
                Res.get("chat.sideBar.userProfile.sendPrivateMessage"));
        ignore = new BisqMenuItem("ignore-grey", "ignore-white",
                Res.get("chat.sideBar.userProfile.ignore"));
        undoIgnore = new BisqMenuItem("undo-ignore-grey", "undo-ignore-white",
                Res.get("chat.sideBar.userProfile.undoIgnore"));
        report = new BisqMenuItem("report-grey", "report-white",
                Res.get("chat.sideBar.userProfile.report"));

        HBox userNameBox = new HBox(10, userNickNameLabel, userNymLabel);
        userNameBox.setAlignment(Pos.BASELINE_LEFT);
        HBox userActionsBox = new HBox(30, privateMsg, ignore, undoIgnore, report);
        VBox userNameReputationAndActionsBox = new VBox(5, userNameBox, reputationScoreDisplay, Spacer.fillVBox(), userActionsBox);
        userNameReputationAndActionsBox.getStyleClass().add("header-content");
        HBox header = new HBox(40, userProfileIcon, userNameReputationAndActionsBox);
        header.setPadding(new Insets(0, 0, 20, 0));

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        topBox.getChildren().addAll(closeButtonRow, header, tabs);
    }

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        lineSidePadding = SIDE_PADDING;
    }

    private void updateUserProfile(UserProfile userProfile) {
        userProfileIcon.setUserProfile(userProfile);
        String nickname = userProfile.getNickName();
        userNickNameLabel.setText(controller.isUserProfileBanned()
                ? Res.get("user.userCard.userNickname.banned", nickname)
                : nickname);
        userNymLabel.setText(String.format("[%s]", userProfile.getNym()));
        if (controller.isUserProfileBanned()) {
            userNickNameLabel.getStyleClass().add("error");
            userNymLabel.getStyleClass().add("error");
        }
    }
}
