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
            e.consume();
        });
        lowThumb.setOnMouseReleased(e -> {
            draggingLow = false;
            e.consume();
        });
        lowThumb.setOnMouseDragged(this::handleLowThumbDrag);
        highThumb.setOnMousePressed(e -> {
            draggingHigh = true;
            dragOffsetX = e.getX();
            highThumb.requestFocus();
            e.consume();
        });
        highThumb.setOnMouseReleased(e -> {
            draggingHigh = false;
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
        double h = slider.getHeight() > 0 ? slider.getHeight() : 40;
        double min = slider.getMin();
        double max = slider.getMax();
        double range = max - min;
        double trackStart = TRACK_PADDING;
        double trackEnd = w - TRACK_PADDING;
        double trackWidth = trackEnd - trackStart;
        track.setLayoutX(trackStart);
        track.setPrefWidth(trackWidth);
        double lowX = trackStart + ((slider.getLowValue() - min) / range) * trackWidth;
        double highX = trackStart + ((slider.getHighValue() - min) / range) * trackWidth;
        double thumbSize = 16; // Use a default size for layout
        lowThumb.setLayoutX(lowX - thumbSize);
        highThumb.setLayoutX(highX);
    }

    private void handleLowThumbDrag(MouseEvent e) {
        if (!draggingLow) {
            return;
        }
        RangeSlider slider = getSkinnable();
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
        value = Math.max(min, Math.min(value, slider.getHighValue()));
        slider.setLowValue(value);
        e.consume();
    }

    private void handleHighThumbDrag(MouseEvent e) {
        if (!draggingHigh) {
            return;
        }
        RangeSlider slider = getSkinnable();
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
        value = Math.max(slider.getLowValue(), Math.min(value, max));
        slider.setHighValue(value);
        e.consume();
    }
}
