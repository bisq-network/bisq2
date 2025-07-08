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

package bisq.desktop.components.controls.skins;

import bisq.desktop.components.controls.RangeSlider;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;

public class RangeSliderSkin extends SkinBase<RangeSlider> {
    private static final double TRACK_PADDING = 5;
    private static final double EDGE_OFFSET = 4;
    private static final double THUMB_SIZE = 16;

    private final RangeSlider rangeSlider;
    private final StackPane track, lowThumb, highThumb;
    private final Pane container;
    private final ChangeListener<Number> changeListener = (obs, o, n) -> updateUI();
    private boolean draggingLow = false;
    private boolean draggingHigh = false;
    private double dragOffsetX = 0;

    public RangeSliderSkin(RangeSlider control) {
        super(control);

        rangeSlider = control;
        track = new StackPane();
        track.getStyleClass().add("track");

        lowThumb = new StackPane();
        lowThumb.getStyleClass().add("range-slider-low-thumb");
        highThumb = new StackPane();
        highThumb.getStyleClass().add("range-slider-high-thumb");
        container = new Pane(track, lowThumb, highThumb);
        getChildren().add(container);

        updateUI();
        registerListeners();
    }

    @Override
    public void dispose() {
        super.dispose();

        lowThumb.setOnMousePressed(null);
        lowThumb.setOnMouseReleased(null);
        lowThumb.setOnMouseDragged(null);
        highThumb.setOnMousePressed(null);
        highThumb.setOnMouseReleased(null);
        highThumb.setOnMouseDragged(null);

        rangeSlider.widthProperty().removeListener(changeListener);
        rangeSlider.heightProperty().removeListener(changeListener);
        rangeSlider.lowValueProperty().removeListener(changeListener);
        rangeSlider.highValueProperty().removeListener(changeListener);
    }

    private void registerListeners() {
        lowThumb.setOnMousePressed(e -> {
            draggingLow = true;
            dragOffsetX = e.getX() - lowThumb.getWidth();
            lowThumb.requestFocus();
            rangeSlider.getLowThumbFocused().set(true);
            e.consume();
        });
        lowThumb.setOnMouseReleased(e -> {
            draggingLow = false;
            rangeSlider.getLowThumbFocused().set(false);
            e.consume();
        });
        lowThumb.setOnMouseDragged(this::handleLowThumbDrag);
        highThumb.setOnMousePressed(e -> {
            draggingHigh = true;
            dragOffsetX = e.getX();
            highThumb.requestFocus();
            rangeSlider.getHighThumbFocused().set(true);
            e.consume();
        });
        highThumb.setOnMouseReleased(e -> {
            draggingHigh = false;
            rangeSlider.getHighThumbFocused().set(false);
            e.consume();
        });
        highThumb.setOnMouseDragged(this::handleHighThumbDrag);

        rangeSlider.widthProperty().addListener(changeListener);
        rangeSlider.heightProperty().addListener(changeListener);
        rangeSlider.lowValueProperty().addListener(changeListener);
        rangeSlider.highValueProperty().addListener(changeListener);
    }

    private void updateUI() {
        RangeSlider slider = getSkinnable();
        double w = slider.getWidth() > 0 ? slider.getWidth() : 200;
        double min = slider.getMin();
        double max = slider.getMax();
        double range = max - min;
        double trackStart = TRACK_PADDING;
        double trackEnd = w - TRACK_PADDING;
        double trackWidth = trackEnd - trackStart;
        track.setLayoutX(trackStart);
        track.setPrefWidth(trackWidth);
        double lowX = trackStart + EDGE_OFFSET + ((slider.getLowValue() - min) / range) * trackWidth;
        double highX = trackStart - EDGE_OFFSET + ((slider.getHighValue() - min) / range) * trackWidth;
        lowThumb.setLayoutX(lowX - THUMB_SIZE);
        highThumb.setLayoutX(highX);
    }

    private void handleLowThumbDrag(MouseEvent e) {
        if (!draggingLow) {
            return;
        }

        RangeSlider slider = getSkinnable();
        double newLow = getValue(slider, e);
        double high = slider.getHighValue();
        // Clamp newLow to not exceed high
        if (newLow > high) {
            newLow = high;
        }
        if (newLow < slider.getMin()) {
            newLow = slider.getMin();
        }
        slider.setLowValue(newLow);
        e.consume();
    }

    private void handleHighThumbDrag(MouseEvent e) {
        if (!draggingHigh) {
            return;
        }

        RangeSlider slider = getSkinnable();
        double newHigh = getValue(slider, e);
        double low = slider.getLowValue();
        // Clamp newHigh to not go below low
        if (newHigh < low) {
            newHigh = low;
        }
        if (newHigh > slider.getMax()) {
            newHigh = slider.getMax();
        }
        slider.setHighValue(newHigh);
        e.consume();
    }

    private double getValue(RangeSlider slider, MouseEvent e) {
        double w = slider.getWidth() > 0 ? slider.getWidth() : 200;
        double min = slider.getMin();
        double max = slider.getMax();
        double range = max - min;
        double trackStart = TRACK_PADDING;
        double trackEnd = w - TRACK_PADDING;
        double trackWidth = trackEnd - trackStart;
        double mouseX = e.getSceneX() - container.localToScene(0, 0).getX();
        double anchorX = mouseX - dragOffsetX;
        double value = min + ((anchorX - trackStart) / trackWidth) * range;
        return Math.max(min, Math.min(value, max));
    }
}
