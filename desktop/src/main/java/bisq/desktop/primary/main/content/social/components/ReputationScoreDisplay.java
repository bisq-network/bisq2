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

package bisq.desktop.primary.main.content.social.components;

import bisq.desktop.common.utils.Icons;
import bisq.i18n.Res;
import bisq.social.user.reputation.ReputationScore;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ReputationScoreDisplay extends HBox {
    final List<Label> stars;
    final Tooltip tooltip = new Tooltip();

    public ReputationScoreDisplay() {
        tooltip.setStyle("-fx-text-fill: black; -fx-background-color: -bisq-grey-11;");
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);

        String fontSize = "0.9em";
        stars = List.of(Icons.getIcon(AwesomeIcon.STAR, fontSize),
                Icons.getIcon(AwesomeIcon.STAR, fontSize),
                Icons.getIcon(AwesomeIcon.STAR, fontSize),
                Icons.getIcon(AwesomeIcon.STAR, fontSize),
                Icons.getIcon(AwesomeIcon.STAR, fontSize));
        for (Label label : stars) {
            label.setMouseTransparent(false);
        }
        setSpacing(5);
        setAlignment(Pos.CENTER);
        getChildren().addAll(stars);
    }

    public void applyReputationScore(ReputationScore reputationScore) {
       // int ranking, int score, double relativeScore
        int target = (int) Math.floor((stars.size() + 1) * reputationScore.getRelativeScore()) - 1;
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setOpacity(i <= target ? 1 : 0.1);
        }

        long percent = Math.round(reputationScore.getRelativeScore() * 10000) / 100;
        tooltip.setText(Res.get("reputation.score.tooltip",
                reputationScore.getRanking(), reputationScore.getScore(), percent));
        Tooltip.install(this, tooltip);
    }
}