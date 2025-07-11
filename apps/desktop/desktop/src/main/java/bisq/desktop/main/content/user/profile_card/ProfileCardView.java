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

package bisq.desktop.main.content.user.profile_card;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.common.view.TabView;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.components.BondedRoleBadge;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileCardView extends TabView<ProfileCardModel, ProfileCardController> {
    public final static double SUB_VIEWS_CONTENT_HEIGHT = 307;

    private final ProfileCardController controller;
    private final TabButton offersTabButton, messagesTabButton;
    private UserProfileIcon userProfileIcon;
    private ReputationScoreDisplay reputationScoreDisplay;
    private Label userNickNameLabel, userNymLabel, totalRepScoreLabel, rankingLabel;
    private BisqMenuItem sendPrivateMsg, ignore, undoIgnore, report;
    private Button closeButton;
    private HBox userActionsBox;
    private BondedRoleBadge bondedRoleBadge;

    public ProfileCardView(ProfileCardModel model, ProfileCardController controller) {
        super(model, controller);

        this.controller = controller;

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getStyleClass().add("profile-card");

        addTab(Res.get("user.profileCard.tab.overview"), NavigationTarget.PROFILE_CARD_OVERVIEW);
        offersTabButton = addTab("", NavigationTarget.PROFILE_CARD_OFFERS);
        messagesTabButton = addTab("", NavigationTarget.PROFILE_CARD_MESSAGES);
        addTab(Res.get("user.profileCard.tab.reputation"), NavigationTarget.PROFILE_CARD_REPUTATION);
        addTab(Res.get("user.profileCard.tab.details"), NavigationTarget.PROFILE_CARD_DETAILS);
    }

    @Override
    protected void onViewAttached() {
        sendPrivateMsg.setVisible(model.isShouldShowUserActionsMenu());
        sendPrivateMsg.setManaged(model.isShouldShowUserActionsMenu());
        userActionsBox.setVisible(model.isShouldShowUserActionsMenu());
        userActionsBox.setManaged(model.isShouldShowUserActionsMenu());
        offersTabButton.getLabel().setText(model.getOffersTabButtonText());
        messagesTabButton.getLabel().setText(model.getMessagesTabButtonText());

        UserProfile userProfile = model.getUserProfile();
        userProfileIcon.setUserProfile(userProfile, false, false);

        bondedRoleBadge.applyBondedRoleTypes(model.getUserProfileBondedRoleTypes());

        String nickname = userProfile.getNickName();
        userNickNameLabel.setText(controller.isUserProfileBanned()
                ? Res.get("user.profileCard.userNickname.banned", nickname)
                : nickname);

        userNymLabel.setText(String.format("[%s]", userProfile.getNym()));
        if (controller.isUserProfileBanned()) {
            userNickNameLabel.getStyleClass().add("error");
            userNymLabel.getStyleClass().add("error");
        }

        if (model.getReputationScore() != null) {
            ReputationScore reputationScore = model.getReputationScore();
            reputationScoreDisplay.setReputationScore(reputationScore);
            totalRepScoreLabel.setText(String.valueOf(reputationScore.getTotalScore()));
            rankingLabel.setText(reputationScore.getRankingAsString());
        }

        ignore.visibleProperty().bind(model.getIgnoreUserSelected().not());
        ignore.managedProperty().bind(model.getIgnoreUserSelected().not());
        undoIgnore.visibleProperty().bind(model.getIgnoreUserSelected());
        undoIgnore.managedProperty().bind(model.getIgnoreUserSelected());

        sendPrivateMsg.setOnAction(e -> controller.onSendPrivateMessage());
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

        sendPrivateMsg.setOnAction(null);
        ignore.setOnAction(null);
        undoIgnore.setOnAction(null);
        report.setOnAction(null);
        closeButton.setOnAction(null);

        bondedRoleBadge.dispose();
    }

    @Override
    protected void setupTopBox() {
        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15 - SIDE_PADDING, 0, 0));

        double size = 100;
        userProfileIcon = new UserProfileIcon(size);

        reputationScoreDisplay = new ReputationScoreDisplay();
        reputationScoreDisplay.setScale(1.5);

        Label totalRepScoreTitleLabel = new Label(Res.get("user.profileCard.reputation.totalReputationScore"));
        totalRepScoreTitleLabel.getStyleClass().add("text-fill-grey-dimmed");
        totalRepScoreLabel = new Label();
        totalRepScoreLabel.getStyleClass().add("total-score");
        HBox totalRepScoreBox = new HBox(5, totalRepScoreTitleLabel, totalRepScoreLabel);
        totalRepScoreBox.getStyleClass().add("total-score-box");

        Label rankigTitleLabel = new Label(Res.get("user.profileCard.reputation.ranking"));
        rankigTitleLabel.getStyleClass().add("text-fill-grey-dimmed");
        rankingLabel = new Label();
        rankingLabel.getStyleClass().add("ranking");
        HBox rankingBox = new HBox(5, rankigTitleLabel, rankingLabel);
        rankingBox.getStyleClass().add("ranking-box");

        bondedRoleBadge = new BondedRoleBadge(true);

        userNickNameLabel = new Label();
        userNickNameLabel.getStyleClass().addAll("text-fill-white", "large-text");

        userNymLabel = new Label();
        userNymLabel.getStyleClass().addAll("text-fill-grey-dimmed", "normal-text");
        userNymLabel.setPadding(new Insets(7, 0, -7, 0));

        sendPrivateMsg = new BisqMenuItem("private-chat-grey", "private-chat-white",
                Res.get("user.profileCard.userActions.sendPrivateMessage"));
        ignore = new BisqMenuItem("ignore-grey", "ignore-white",
                Res.get("user.profileCard.userActions.ignore"));
        undoIgnore = new BisqMenuItem("undo-ignore-grey", "undo-ignore-white",
                Res.get("user.profileCard.userActions.undoIgnore"));
        report = new BisqMenuItem("report-grey", "report-white",
                Res.get("user.profileCard.userActions.report"));

        HBox userNameBox = new HBox(10, bondedRoleBadge, userNickNameLabel, userNymLabel);
        HBox reputationBox = new HBox(30, reputationScoreDisplay, totalRepScoreBox, rankingBox);
        reputationBox.setAlignment(Pos.BOTTOM_LEFT);
        userActionsBox = new HBox(30, sendPrivateMsg, ignore, undoIgnore, report);
        VBox userNameReputationAndActionsBox = new VBox(5, userNameBox, reputationBox, Spacer.fillVBox(), userActionsBox);
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
}
