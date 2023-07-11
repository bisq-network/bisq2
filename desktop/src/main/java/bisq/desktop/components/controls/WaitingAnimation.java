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

package bisq.desktop.components.controls;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.utils.ImageUtil;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WaitingAnimation extends StackPane {
    private final static double DURATION = 600;
    private final List<CircleAnimation> circleAnimations = new ArrayList<>();
    private final ImageView hourGlass;
    private final double duration;
    private UIScheduler scheduler;
    private Timeline hourGlassTimeline;

    public WaitingAnimation() {
        this(DURATION);
    }

    public WaitingAnimation(double duration) {
        this.duration = duration;
        setAlignment(Pos.TOP_LEFT);
        circleAnimations.add(new CircleAnimation(duration, 0));
        circleAnimations.add(new CircleAnimation(duration, 1 / (double) 3));
        circleAnimations.add(new CircleAnimation(duration, 2 / (double) 3));
        getChildren().addAll(circleAnimations);

        // size 140 px
        hourGlass = ImageUtil.getImageViewById("hour_glass");
        getChildren().add(hourGlass);
    }

    public void play() {
        stop();
        hourGlassTimeline = Transitions.pulse(hourGlass, duration, 0,
                0, 0.05, 0.1,
                1.1, 1);
        scheduler = UIScheduler.run(() -> circleAnimations.forEach(CircleAnimation::play))
                .periodically(0, Math.round(10 * duration), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (hourGlassTimeline != null) {
            hourGlassTimeline.stop();
            hourGlassTimeline = null;
        }
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        circleAnimations.forEach(CircleAnimation::stop);
    }

    private static class CircleAnimation extends Circle {
        private final double duration;
        private final double delay;
        private Timeline timeline;

        public CircleAnimation(double duration, double offset) {
            super(0, 0, 69);
            this.duration = duration;
            this.delay = duration * offset;
            setFill(Color.TRANSPARENT);
            setStroke(Color.valueOf("#56ae48"));
            setStrokeWidth(1);
            setOpacity(0);
        }

        public void play() {
            stop();
            timeline = Transitions.pulse(this, duration, delay,
                    0, 0.1, 0,
                    1, 1.5);
        }

        public void stop() {
            if (timeline != null) {
                timeline.stop();
                timeline = null;
            }
        }
    }
}