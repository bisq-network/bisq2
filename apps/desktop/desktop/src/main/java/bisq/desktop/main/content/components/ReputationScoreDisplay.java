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

package bisq.desktop.main.content.components;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.user.reputation.ReputationScore;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ReputationScoreDisplay extends HBox {
    private static final double SPACING = 5;
    private static final double STAR_WIDTH = 12;
    private static final double STAR_HEIGHT = 11;
    private static final double OPACITY = 0.2;
    private static final String DEFAULT_ACCEPT_STAR_ID = "star-green";
    private static final int STAR_SYSTEM = 5; // 5-star system

    private final List<ImageView> stars = IntStream.range(0, STAR_SYSTEM).mapToObj(i -> getDefaultStar()).collect(Collectors.toList());
    private final Tooltip tooltip = new BisqTooltip();
    private ReputationScore reputationScore;
    private String acceptStarId = DEFAULT_ACCEPT_STAR_ID;

    public ReputationScoreDisplay(ReputationScore reputationScore) {
        this();
        setReputationScore(reputationScore);
    }

    public ReputationScoreDisplay() {
        super(SPACING);

        setAlignment(Pos.CENTER_LEFT);

        tooltip.setStyle("-fx-text-fill: black; -fx-background-color: -bisq-light-grey-10;");
        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        Tooltip.install(this, tooltip);

        getChildren().addAll(stars);
    }

    public void setReputationScore(@Nullable ReputationScore reputationScore) {
        this.reputationScore = reputationScore;
        applyReputationScore();
    }

    public void setScale(double scale) {
        setSpacing(SPACING * scale);
        stars.forEach(imageView -> {
            imageView.setFitWidth(STAR_WIDTH * scale);
            imageView.setFitHeight(STAR_HEIGHT * scale);
        });
    }

    public void useGreenAcceptStar() {
        acceptStarId = "star-green";
        applyReputationScore();
    }

    public void useWhiteAcceptStar() {
        acceptStarId = "star-white";
        applyReputationScore();
    }

    private void applyReputationScore() {
        int starsToFill = calculateStars();
        for (int i = 0; i < STAR_SYSTEM; i++) {
            ImageView imageView = stars.get(i);
            if (i < starsToFill) {
                imageView.setOpacity(1);
                imageView.setId(acceptStarId);
            } else {
                imageView.setOpacity(OPACITY);
                imageView.setId("star-white");
            }
        }
        tooltip.setText(reputationScore != null ? reputationScore.getTooltipString() : null);
    }

    public String getTooltipString() {
        return reputationScore != null ? reputationScore.getTooltipString() : "";
    }

    public int getNumberOfStars() {
        return calculateStars();
    }

    private ImageView getDefaultStar() {
        ImageView imageView = ImageUtil.getImageViewById("star-white");
        imageView.setOpacity(OPACITY);
        return imageView;
    }

    private int calculateStars() {
        double relativeScore = reputationScore != null ? reputationScore.getRelativeScore() : 0;
        return (int) Math.floor(STAR_SYSTEM * relativeScore);
    }
}
