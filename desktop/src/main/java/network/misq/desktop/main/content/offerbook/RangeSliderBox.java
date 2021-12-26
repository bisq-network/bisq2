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

package network.misq.desktop.main.content.offerbook;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;
import network.misq.desktop.components.controls.AutoTooltipLabel;
import org.controlsfx.control.RangeSlider;

// Sub view which is using a custom sub model
@Slf4j
public class RangeSliderBox extends Pane {
    private final RangeSlider slider;
    private final Label titleLabel, minLabel, maxLabel, lowLabel, highLabel;

    public RangeSliderBox(String title, int width, OfferbookModel model, OfferbookController controller) {
        setPrefWidth(width);

        titleLabel = new AutoTooltipLabel(title);
        minLabel = new AutoTooltipLabel("Min");
        maxLabel = new AutoTooltipLabel("Max");
        lowLabel = new AutoTooltipLabel("lowLabel");
        highLabel = new AutoTooltipLabel("highLabel");
        slider = new RangeSlider(0, 100, 0, 100);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.prefWidthProperty().bind(this.prefWidthProperty());
        getChildren().addAll(titleLabel, slider, minLabel, maxLabel, lowLabel, highLabel);
        setMaxHeight(50);

        slider.lowValueProperty().bindBidirectional(model.getAmountFilterModel().getLowPercentage());
        slider.highValueProperty().bindBidirectional(model.getAmountFilterModel().getHighPercentage());
        lowLabel.textProperty().bind(model.getAmountFilterModel().getLowFormattedAmount());
        highLabel.textProperty().bind(model.getAmountFilterModel().getHighFormattedAmount());
    }

    public void onViewAdded() {
        slider.applyCss();
        Pane lowThumb = (Pane) slider.lookup(".range-slider .low-thumb");
        lowLabel.layoutXProperty().bind(lowThumb.layoutXProperty());
        lowLabel.layoutYProperty().bind(lowThumb.layoutYProperty().subtract(25));
        Pane highThumb = (Pane) slider.lookup(".range-slider .high-thumb");
        highLabel.layoutXProperty().bind(highThumb.layoutXProperty().add(highThumb.widthProperty()).subtract(highLabel.widthProperty()));
        highLabel.layoutYProperty().bind(highThumb.layoutYProperty().subtract(25));

        minLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));
        maxLabel.layoutXProperty().bind(slider.widthProperty().subtract(maxLabel.widthProperty()));
        maxLabel.layoutYProperty().bind(slider.layoutYProperty().add(20));

        titleLabel.layoutXProperty().bind(slider.widthProperty().subtract(titleLabel.widthProperty()).divide(2));
        titleLabel.layoutYProperty().bind(slider.layoutYProperty().subtract(45));
    }

    public void onViewRemoved() {
        //todo unbind
    }
}
