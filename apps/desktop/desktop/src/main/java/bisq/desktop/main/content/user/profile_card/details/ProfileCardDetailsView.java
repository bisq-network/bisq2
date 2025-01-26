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

package bisq.desktop.main.content.user.profile_card.details;

import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.user.profile_card.ProfileCardView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ProfileCardDetailsView extends View<VBox, ProfileCardDetailsModel, ProfileCardDetailsController> {
    private final Label nickNameLabel, botIdLabel, userIdLabel, transportAddressLabel, totalReputationScoreLabel,
            profileAgeLabel, versionLabel;
    private final BisqMenuItem nickNameCopyButton, botIdCopyButton, userIdCopyButton, transportAddressCopyButton;

    public ProfileCardDetailsView(ProfileCardDetailsModel model,
                                  ProfileCardDetailsController controller) {
        super(new VBox(), model, controller);

        // Bot ID
        nickNameLabel = new Label();
        nickNameCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        HBox nickNameIdBox = createAndGetTitleAndDetailsBox("onboarding.createProfile.nickName",
                nickNameLabel, Optional.of(nickNameCopyButton));

        // Bot ID
        botIdLabel = new Label();
        botIdCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        HBox botIdBox = createAndGetTitleAndDetailsBox("user.profileCard.details.botId",
                botIdLabel, Optional.of(botIdCopyButton));

        // User ID
        userIdLabel = new Label();
        userIdCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        HBox userIdBox = createAndGetTitleAndDetailsBox("user.profileCard.details.userId",
                userIdLabel, Optional.of(userIdCopyButton));

        // Transport address
        transportAddressLabel = new Label();
        transportAddressCopyButton = new BisqMenuItem("copy-grey", "copy-white");
        HBox transportAddressBox = createAndGetTitleAndDetailsBox("user.profileCard.details.transportAddress",
                transportAddressLabel, Optional.of(transportAddressCopyButton));

        // Total reputation score
        totalReputationScoreLabel = new Label();
        HBox totalReputationScoreBox = createAndGetTitleAndDetailsBox("user.profileCard.details.totalReputationScore", totalReputationScoreLabel);

        // Profile age
        profileAgeLabel = new Label();
        HBox profileAgeBox = createAndGetTitleAndDetailsBox("user.profileCard.details.profileAge", profileAgeLabel);

        // Version
        versionLabel = new Label();
        HBox versionBox = createAndGetTitleAndDetailsBox("user.profileCard.details.version", versionLabel);

        VBox contentBox = new VBox(16, nickNameIdBox, botIdBox, userIdBox, transportAddressBox,
                totalReputationScoreBox, profileAgeBox, versionBox);

        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);
        contentBox.setMinHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);


        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 20, 0));
    }

    @Override
    protected void onViewAttached() {
        nickNameLabel.setText(model.getNickName());
        botIdLabel.setText(model.getBotId());
        userIdLabel.setText(model.getUserId());
        transportAddressLabel.setText(model.getTransportAddress());
        profileAgeLabel.setText(model.getProfileAge());
        versionLabel.setText(model.getVersion());

        totalReputationScoreLabel.textProperty().bind(model.getTotalReputationScore());

        nickNameCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getNickName()));
        botIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getBotId()));
        userIdCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getUserId()));
        transportAddressCopyButton.setOnAction(e -> ClipboardUtil.copyToClipboard(model.getTransportAddress()));
    }

    @Override
    protected void onViewDetached() {
        totalReputationScoreLabel.textProperty().unbind();
        nickNameCopyButton.setOnAction(null);
        botIdCopyButton.setOnAction(null);
        userIdCopyButton.setOnAction(null);
        transportAddressCopyButton.setOnAction(null);
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel) {
        return createAndGetTitleAndDetailsBox(title, detailsLabel, Optional.empty());
    }

    private HBox createAndGetTitleAndDetailsBox(String title, Label detailsLabel, Optional<BisqMenuItem> button) {
        Label titleLabel = new Label(Res.get(title).toUpperCase());
        double width = 200;
        titleLabel.setMaxWidth(width);
        titleLabel.setMinWidth(width);
        titleLabel.setPrefWidth(width);
        titleLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text", "font-light");

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
