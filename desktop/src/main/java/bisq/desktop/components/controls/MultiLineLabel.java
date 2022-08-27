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

/**
 * Set the minHeight of the label so that it does not get truncated.
 * todo still not working in all situations correctly ;-(
 */
@Slf4j
public class MultiLineLabel extends Label {
    private ChangeListener<Number> heightListener, widthListener;
    private ChangeListener<Scene> sceneListener;
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

        widthListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0 && getHeight() > 0) {
                minHeight = initialHeight;
                adjustMinHeight();
            }
        };

        heightListener = (observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > 0) {
                heightProperty().removeListener(heightListener);
                initialHeight = newValue.doubleValue();
                adjustMinHeight();
            }
        };

        sceneListener = (observable, oldValue, newValue) -> {
            if (newValue == null) {
                widthProperty().removeListener(widthListener);
            } else {
                widthProperty().addListener(widthListener);
                heightProperty().addListener(heightListener);
            }
        };
        sceneProperty().addListener(sceneListener);

        widthProperty().addListener(widthListener);
        heightProperty().addListener(heightListener);
    }


    private void adjustMinHeight() {
        if (getParent() instanceof Pane) {
            Pane parentPane = (Pane) getParent();
            parentPane.setMinHeight(2000);
            UIThread.runOnNextRenderFrame(() -> {
                if (initialHeight == getHeight() && numRecursions < 500) {
                    numRecursions++;
                    adjustMinHeight();
                } else {
                    minHeight = getHeight();
                    parentPane.setMinHeight(Region.USE_COMPUTED_SIZE);
                }
            });
        }
    }
}