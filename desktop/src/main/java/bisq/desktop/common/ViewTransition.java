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

package bisq.desktop.common;

import bisq.common.util.MathUtils;
import bisq.desktop.common.threading.UIScheduler;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.TransitionedView;
import bisq.desktop.common.view.View;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class ViewTransition {
    @Nullable
    private Region oldViewRoot, newViewRoot;
    @Nullable
    private View<? extends Parent, ? extends Model, ? extends Controller> newView;
    @Nullable
    private Timeline slideOutTimeline;
    @Nullable
    private UIScheduler scheduler;
    @Nullable
    private FadeTransition fadeinTransition;
    private double oldViewX;

    public ViewTransition(@Nullable Region oldViewRoot,
                          View<? extends Parent, ? extends Model, ? extends Controller> newView) {
        this.oldViewRoot = oldViewRoot;
        this.newView = newView;
        newViewRoot = newView.getRoot();
        if (!Transitions.getUseAnimations()) {
            newViewRoot.setOpacity(1);
            remove(oldViewRoot);
            return;
        }
        int defaultDuration = Transitions.DEFAULT_DURATION;
        newViewRoot.setOpacity(0);
        if (oldViewRoot == null) {
            fadeIn(defaultDuration);
        } else {
            oldViewX = oldViewRoot.getTranslateX();
            scheduler = UIScheduler.run(() -> fadeIn(MathUtils.roundDoubleToInt(defaultDuration / 2d))).after(defaultDuration / 2);
            if (slideOutTimeline != null) {
                slideOutTimeline.stop();
            }
            slideOutTimeline = Transitions.slideOutRight(oldViewRoot, () -> {
                remove(this.oldViewRoot);
                this.oldViewRoot = null;
            });
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (slideOutTimeline != null && slideOutTimeline.getStatus() == Animation.Status.RUNNING) {
            slideOutTimeline.stop();

        }
        if (oldViewRoot != null) {
            oldViewRoot.setTranslateX(oldViewX);
            remove(oldViewRoot);
            oldViewRoot = null;
        }

        if (fadeinTransition != null && fadeinTransition.getStatus() == Animation.Status.RUNNING) {
            fadeinTransition.stop();
            if (newViewRoot != null) {
                newViewRoot.setOpacity(1);
                remove(newViewRoot);
                this.newView = null;
                newViewRoot = null;
            }
        }
    }

    private void remove(Region region) {
        if (region != null) {
            Parent parent = region.getParent();
            if (parent instanceof Pane) {
                Pane pane = (Pane) parent;
                pane.getChildren().remove(region);
            }
        }
    }

    private void fadeIn(int duration) {
        if (newView instanceof TransitionedView) {
            ((TransitionedView) newView).onStartTransition();
        }
        if (fadeinTransition != null) {
            fadeinTransition.stop();
        }
        fadeinTransition = Transitions.fadeIn(newViewRoot,
                duration,
                () -> {
                    if (newView instanceof TransitionedView) {
                        ((TransitionedView) newView).onTransitionCompleted();
                    }
                });
    }
}