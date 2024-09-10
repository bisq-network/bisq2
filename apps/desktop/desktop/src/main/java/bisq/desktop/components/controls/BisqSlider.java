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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.Slider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqSlider extends Slider {
    private final ChangeListener<Number> changeListener = (observableValue, oldValue, newValue) -> {
        double percentage = 100.0 * newValue.doubleValue() / getMax();
        String style = String.format(
                "-track-color: linear-gradient(to right, " +
                        "-bisq2-green 0%%, " +
                        "-bisq2-green %1$.1f%%, " +
                        "-default-track-color %1$.1f%%, " +
                        "-default-track-color 100%%);",
                percentage);
        log.error("{} {}", percentage, style);
        //49.36406995230524 -track-color: linear-gradient(to right, -fx-accent 0%, -fx-accent 49.4%, -default-track-color 49.4%, -default-track-color 100%);
        setStyle(style);
    };

    public BisqSlider() {
        setupListener();
    }

    public BisqSlider(double min, double max, double value) {
        super(min, max, value);
        setupListener();
    }

    private void setupListener() {
        valueProperty().addListener(new WeakChangeListener<>(changeListener));
    }
}
