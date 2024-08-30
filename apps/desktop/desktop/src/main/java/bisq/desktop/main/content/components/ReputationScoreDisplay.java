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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ReputationScoreDisplay extends HBox {
    private static final double SPACING = 5;
    private static final double STAR_WIDTH = 12;
    private static final double STAR_HEIGHT = 11;
    private static final String DEFAULT_ACCEPT_STAR_ID = "star-green";
    private static final String DEFAULT_ACCEPT_HALF_STAR_ID = "star-half-hollow-green";
    private static final int STAR_SYSTEM = 5; // 5-star system
    private static final String STYLE_CLASS = "reputation-score-display";

    private final List<ImageView> stars = IntStream.range(0, STAR_SYSTEM).mapToObj(i -> getDefaultStar()).collect(Collectors.toList());
    private final Tooltip tooltip = new BisqTooltip();
    private ReputationScore reputationScore;
    private String acceptStarId = DEFAULT_ACCEPT_STAR_ID;
    private String acceptHalfStarId = DEFAULT_ACCEPT_HALF_STAR_ID;

    public ReputationScoreDisplay(ReputationScore reputationScore) {
        this();
        setReputationScore(reputationScore);
    }

    public ReputationScoreDisplay() {
        super(SPACING);

        setAlignment(Pos.CENTER_LEFT);

        tooltip.setMaxWidth(300);
        tooltip.setWrapText(true);
        Tooltip.install(this, tooltip);

        getStyleClass().add(STYLE_CLASS);
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
        acceptHalfStarId = "star-half-hollow-green";
        applyReputationScore();
    }

    public void useWhiteAcceptStar() {
        acceptStarId = "star-white";
        acceptHalfStarId = "star-half-hollow-white";
        applyReputationScore();
    }

    private void applyReputationScore() {
        double starsToFill = reputationScore != null ? reputationScore.getFiveSystemScore() : 0d;
        AtomicInteger index = new AtomicInteger();
        stars.forEach(imageView -> {
            int i = index.getAndIncrement();
            if (i < Math.floor(starsToFill)) {
                // Full star
                imageView.setOpacity(1);
                imageView.setId(acceptStarId);
            } else if (i < starsToFill) {
                // Half star
                imageView.setOpacity(1);
                imageView.setId(acceptHalfStarId);
            } else {
                // Empty star
                imageView.setId("star-grey-hollow");
            }
        });
        tooltip.setText(reputationScore != null ? reputationScore.getTooltipString() : null);
    }

    public String getTooltipString() {
        return reputationScore != null ? reputationScore.getTooltipString() : "";
    }

    public double getNumberOfStars() {
        return reputationScore != null ? reputationScore.getFiveSystemScore() : 0d;
    }

    private ImageView getDefaultStar() {
        return ImageUtil.getImageViewById("star-grey-hollow");
    }
}
