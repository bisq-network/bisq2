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
import bisq.desktop.main.content.components.UserProfileDisplay;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

public class UserCardView extends TabView<UserCardModel, UserCardController> {
    private Button closeButton;
    private UserProfileDisplay userProfileDisplay;
    private Subscription reputationScorePin;

    public UserCardView(UserCardModel model, UserCardController controller) {
        super(model, controller);

        addTab(Res.get("user.userDetailsPopup.tab.overview"), NavigationTarget.USER_CARD_OVERVIEW);
        addTab(Res.get("user.userDetailsPopup.tab.details"), NavigationTarget.USER_CARD_DETAILS);
//        addTab(Res.get("user.userDetailsPopup.tab.offers"), NavigationTarget.USER_CARD_OFFERS);
//        addTab(Res.get("user.userDetailsPopup.tab.reputation"), NavigationTarget.USER_CARD_REPUTATION);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);
        root.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        root.getStyleClass().add("user-details-popup");
    }

    @Override
    protected void onViewAttached() {
        userProfileDisplay.setUserProfile(model.getUserProfile());
        userProfileDisplay.setReputationScoreDisplayScale(1.5);

        reputationScorePin = EasyBind.subscribe(model.getReputationScore(), reputationScore -> {
            if (reputationScore != null) {
                userProfileDisplay.setReputationScore(reputationScore);
            }
        });

        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        reputationScorePin.unsubscribe();
        closeButton.setOnAction(null);
    }

    @Override
    protected void setupTopBox() {
        closeButton = BisqIconButton.createIconButton("close");
        HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeButton);
        closeButtonRow.setPadding(new Insets(15, 15-SIDE_PADDING, 0, 0));

        userProfileDisplay = new UserProfileDisplay(100);
        userProfileDisplay.setPadding(new Insets(0, 0, 20, 0));

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        tabs.setMinHeight(35);

        topBox = new VBox();
        topBox.getChildren().addAll(closeButtonRow, userProfileDisplay, tabs);
    }

    @Override
    protected void setupLineAndMarker() {
        super.setupLineAndMarker();

        lineSidePadding = SIDE_PADDING;
    }
}
