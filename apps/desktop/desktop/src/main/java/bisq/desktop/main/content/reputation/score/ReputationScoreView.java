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

package bisq.desktop.main.content.reputation.score;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReputationScoreView extends View<VBox, ReputationScoreModel, ReputationScoreController> {
    public ReputationScoreView(ReputationScoreModel model, ReputationScoreController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("reputation.reputationScore.headline"));
        headlineLabel.getStyleClass().add("reputation-headline");

        Label introLabel = new Label(Res.get("reputation.reputationScore.intro"));

        Label sellerReputationLabel = new Label(Res.get("reputation.reputationScore.sellerReputation"));
        sellerReputationLabel.setAlignment(Pos.TOP_LEFT);
        ImageView offerImage = ImageUtil.getImageViewById("offer-reputation");
        VBox offerBox = new VBox(offerImage);
        offerBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox sellerReputationBox = new HBox(sellerReputationLabel, Spacer.fillHBox(), offerBox);
        sellerReputationBox.getStyleClass().addAll("reputation-card-large", "bisq-card-bg");

        Label explanationIntroLabel = new Label(Res.get("reputation.reputationScore.explanation.intro"));

        Label scoreTitleLabel = new Label(Res.get("reputation.reputationScore.explanation.score.title"));
        scoreTitleLabel.getStyleClass().add("card-title");
        Label scoreDescriptionLabel = new Label(Res.get("reputation.reputationScore.explanation.score.description"));
        VBox scoreBox = new VBox(20, scoreTitleLabel, scoreDescriptionLabel);
        scoreBox.getStyleClass().addAll("reputation-card-small", "bisq-card-bg");
        Label rankingTitleLabel = new Label(Res.get("reputation.reputationScore.explanation.ranking.title"));
        rankingTitleLabel.getStyleClass().add("card-title");
        Label rankingDescriptionLabel = new Label(Res.get("reputation.reputationScore.explanation.ranking.description"));
        VBox rankingBox = new VBox(20, rankingTitleLabel, rankingDescriptionLabel);
        rankingBox.getStyleClass().addAll("reputation-card-small", "bisq-card-bg");
        HBox scoreAndReputationBox = new HBox(20, scoreBox, rankingBox);

        Label starsTitleLabel = new Label(Res.get("reputation.reputationScore.explanation.stars.title"));
        starsTitleLabel.getStyleClass().add("card-title");
        Label starsDescriptionLabel = new Label(Res.get("reputation.reputationScore.explanation.stars.description"));
        ImageView starsTableImage = ImageUtil.getImageViewById("stars-reputation-table");
        VBox starsTableBox = new VBox(starsTableImage);
        starsTableBox.setAlignment(Pos.CENTER);
        VBox starsBox = new VBox(20, starsTitleLabel, starsDescriptionLabel, starsTableBox);
        starsBox.getStyleClass().addAll("reputation-card-large", "bisq-card-bg");

        Label closingLabel = new Label(Res.get("reputation.reputationScore.closing"));

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(headlineLabel, introLabel, sellerReputationBox, explanationIntroLabel,
                scoreAndReputationBox, starsBox, closingLabel);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
        root.getStyleClass().add("reputation");
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }
}
