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

import bisq.desktop.common.threading.UIThread;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

/**
 * Set the minHeight of the label so that it does not get truncated.
 * todo still not working in all situations correctly ;-(
 */
@Slf4j
public class MultiLineLabel extends Label {
    private ChangeListener<Number> heightListener, widthListener;
    private double initialHeight = 0;
    private int numRecursions = 0;
    private double minHeight;

    public MultiLineLabel() {
        initialize();
    }

    public MultiLineLabel(String text) {
        super(text);
        initialize();
    }

    public MultiLineLabel(String text, Node graphic) {
        super(text, graphic);
        initialize();
    }

    @Override
    protected double computeMinHeight(final double width) {
        return minHeight;
    }

    private void initialize() {
        setWrapText(true);

        widthListener = new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                onWidthChanged(newValue.doubleValue())).get();
        heightListener = new WeakReference<ChangeListener<Number>>((observable, oldValue, newValue) ->
                onHeightChanged(newValue.doubleValue())).get();

        sceneProperty().addListener(new WeakReference<ChangeListener<Scene>>((observable, oldValue, newValue) ->
                onSceneChanged(newValue)).get());
        widthProperty().addListener(widthListener);
        heightProperty().addListener(heightListener);

        UIThread.runOnNextRenderFrame(this::adjustMinHeight);
    }

    private void onWidthChanged(double width) {
        if (width > 0 && getHeight() > 0) {
            minHeight = initialHeight;
            adjustMinHeight();
        }
    }

    private void onHeightChanged(double height) {
        if (height > 0) {
            UIThread.runOnNextRenderFrame(() -> {
                heightProperty().removeListener(heightListener);
                widthProperty().removeListener(widthListener);
            });
            initialHeight = height;
            adjustMinHeight();
        }
    }

    private void onSceneChanged(Scene newValue) {
        if (newValue == null) {
            widthProperty().removeListener(widthListener);
        } else {
            widthProperty().addListener(widthListener);
            heightProperty().addListener(heightListener);
        }
    }

    private void adjustMinHeight() {
        if (getParent() instanceof Pane) {
            Pane parentPane = (Pane) getParent();
            if (getText() != null && !getText().isEmpty()) {
                parentPane.setMinHeight(2000);
            }
            trySetMinHeight(parentPane);
            // UIThread.runOnNextRenderFrame(() -> trySetMinHeight(parentPane));
        }
    }

    private void trySetMinHeight(Pane parentPane) {
        double height = getHeight();
        if (height > 0 && getText() != null && !getText().isEmpty()) {
            if (initialHeight == height && numRecursions < 500) {
                numRecursions++;
                adjustMinHeight();
                // UIThread.runOnNextRenderFrame(this::adjustMinHeight);
            } else {
                minHeight = height;
                parentPane.setMinHeight(Region.USE_COMPUTED_SIZE);
            }
        }
    }
}