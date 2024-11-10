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

package bisq.desktop.main.content.user.user_card.details;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class UserCardDetailsView extends View<VBox, UserCardDetailsModel, UserCardDetailsController> {
    private final Label botIdLabel, userIdLabel, transportAddressLabel, totalReputationScoreLabel, profileAgeLabel,
            lastUserActivityLabel, versionLabel;

    public UserCardDetailsView(UserCardDetailsModel model,
                               UserCardDetailsController controller) {
        super(new VBox(), model, controller);

        // Bot ID
        botIdLabel = new Label();
        HBox botIdBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.botId", botIdLabel);

        // User ID
        userIdLabel = new Label();
        HBox userIdBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.userId", userIdLabel);

        // Transport address
        transportAddressLabel = new Label();
        HBox transportAddressBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.transportAddress",
                transportAddressLabel);

        // Total reputation score
        totalReputationScoreLabel = new Label();
        HBox totalReputationScoreBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.totalReputationScore", totalReputationScoreLabel);

        // Profile age
        profileAgeLabel = new Label();
        HBox profileAgeBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.profileAge", profileAgeLabel);

        // Last user activity
        lastUserActivityLabel = new Label();
        HBox lastUserActivityBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.lastUserActivity", lastUserActivityLabel);

        // Version
        versionLabel = new Label();
        HBox versionBox = createAndGetTitleAndDetailsBox("user.userDetailsPopup.details.version", versionLabel);

        VBox contentBox = new VBox(16, botIdBox, userIdBox, transportAddressBox, totalReputationScoreBox, profileAgeBox,
                lastUserActivityBox, versionBox);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.CENTER);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 20, 0));
    }

    @Override
    protected void onViewAttached() {
        botIdLabel.textProperty().bind(model.getBotId());
        userIdLabel.textProperty().bind(model.getUserId());
        transportAddressLabel.textProperty().bind(model.getTransportAddress());
        totalReputationScoreLabel.textProperty().bind(model.getTotalReputationScore());
        profileAgeLabel.textProperty().bind(model.getProfileAge());
        lastUserActivityLabel.textProperty().bind(model.getLastUserActivity());
        versionLabel.textProperty().bind(model.getVersion());
    }

    @Override
    protected void onViewDetached() {
        botIdLabel.textProperty().unbind();
        userIdLabel.textProperty().unbind();
        transportAddressLabel.textProperty().unbind();
        totalReputationScoreLabel.textProperty().unbind();
        profileAgeLabel.textProperty().unbind();
        lastUserActivityLabel.textProperty().unbind();
        versionLabel.textProperty().unbind();
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel) {
        return createAndGetTitleAndDetailsBox(title, detailsLabel, Optional.empty());
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel, Optional<BisqMenuItem> button) {
        Label titleLabel = new Label(Res.get(title));
        double width = 180;
        titleLabel.setMaxWidth(width);
        titleLabel.setMinWidth(width);
        titleLabel.setPrefWidth(width);
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "medium-text");

        detailsLabel.getStyleClass().addAll("text-fill-white", "normal-text");

        HBox hBox = new HBox(titleLabel, detailsLabel);
        hBox.setAlignment(Pos.BASELINE_LEFT);

        if (button.isPresent()) {
            button.get().useIconOnly(17);
            HBox.setMargin(button.get(), new Insets(0, 0, 0, 40));
            hBox.getChildren().addAll(Spacer.fillHBox(), button.get());
        }
        return hBox;
    }
}
