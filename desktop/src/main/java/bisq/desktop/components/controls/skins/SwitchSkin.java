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

import bisq.desktop.components.controls.Switch;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.skin.ToggleButtonSkin;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SwitchSkin extends ToggleButtonSkin {

    public SwitchSkin(Switch toggleButton) {
        super(toggleButton);

        double circleRadius = toggleButton.getSize();

        Line line = new Line();
        line.setStroke(computeLineColor(toggleButton));
        line.setStrokeWidth(circleRadius * 2);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        line.setEndX(circleRadius * 2 + 2);
        line.setSmooth(true);

        Circle circle = new Circle();
        circle.setFill(computeCircleColor(toggleButton));
        circle.setRadius(circleRadius);
        circle.setCenterX(-circleRadius);
        circle.setTranslateX(computeCirclePosition(circleRadius, line));
        circle.setSmooth(true);

        StackPane stackPane = new StackPane(line, circle);
        stackPane.setCursor(Cursor.HAND);
        stackPane.setPadding(new Insets(0, 5, 0, 0));

        toggleButton.selectedProperty().addListener(observable -> circle.setTranslateX(computeCirclePosition(circleRadius, line)));

        if (toggleButton.getGraphic() != null) {
            Node graphic = toggleButton.getGraphic();
            double distance = line.getStrokeWidth() + 70;
            stackPane.setPadding(new Insets(0, 5, 0, -30));
            StackPane.setMargin(graphic, new Insets(0, 0, 0, distance));
            stackPane.getChildren().add(graphic);
            toggleButton.setGraphicTextGap(5);
            toggleButton.setStyle("-fx-padding: 0 0 0 10;");
        }

        toggleButton.setGraphic(stackPane);
    }

    private Paint computeLineColor(Switch toggleButton) {
        return toggleButton.isSelected() ? toggleButton.getToggleLineColor() : toggleButton.getUnToggleLineColor();
    }

    private Paint computeCircleColor(Switch toggleButton) {
        return toggleButton.isSelected() ? toggleButton.getToggleColor() : toggleButton.getUnToggleColor();
    }

    private double computeCirclePosition(double circleRadius, Line line) {
        return (getSkinnable().isSelected() ? 1 : -1) * ((line.getLayoutBounds().getWidth() / 2) - circleRadius + 2);
    }
}
