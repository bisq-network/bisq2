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

package bisq.desktop.main.content.createoffer;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.AutoTooltipButton;
import bisq.desktop.components.controls.BisqLabel;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateOfferView extends View<VBox, CreateOfferModel, CreateOfferController> {
    private final AutoTooltipButton back, next;

    public CreateOfferView(CreateOfferModel model, CreateOfferController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(40);
        root.setPadding(new Insets(20, 20, 20, 20));
        HBox topPane = new HBox();
        topPane.setSpacing(20);
        topPane.getChildren().addAll(getStepNode("Select assets"),
                getStepNode("Set amounts"),
                getStepNode("Protocol"),
                getStepNode("Transfer"),
                getStepNode("Options"),
                getStepNode("Review offer"));
        HBox footer = new HBox();
        back = new AutoTooltipButton("Back");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        next = new AutoTooltipButton("Next");
        footer.getChildren().addAll(back, spacer, next);

        root.getChildren().addAll(topPane, footer);
    }

    private Node getStepNode(String title) {
        return new BisqLabel(title);
    }

    protected void activate() {
        Scene scene = root.getScene();
        Stage window = (Stage) scene.getWindow();
        window.titleProperty().bind(model.titleProperty);

        root.minWidthProperty().bind(model.minWidthProperty);
        root.minHeightProperty().bind(model.minHeightProperty);
        back.visibleProperty().bind(model.backButtonVisible);
        next.visibleProperty().bind(model.nextButtonVisible);
        model.view.addListener((observable, oldValue, newValue) -> root.getChildren().add(1, newValue.getRoot()));

        back.setOnAction(e -> controller.onNavigateBack());
        next.setOnAction(e -> controller.onNavigateNext());
    }

    protected void deactivate() {
        //todo remove bindings, listeners
    }
}
