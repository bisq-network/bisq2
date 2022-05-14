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

package bisq.desktop.primary.main.content.newProfilePopup;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.desktop.overlay.BasicOverlay;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewProfilePopupView extends View<Pane, NewProfilePopupModel, NewProfilePopupController> {
    private final NewProfilePopup popup;
    private HBox stepsBox;

    public NewProfilePopupView(NewProfilePopupModel model, 
                               NewProfilePopupController controller, 
                               NewProfilePopup popup, 
                               DefaultApplicationService applicationService) {
        super(new Pane(), model, controller);
        this.popup = popup;
    }

    @Override
    protected void onViewAttached() {}

    @Override
    protected void onViewDetached() {}

    protected void addContent() {
        VBox content = popup.getMainContent();
        content.setPrefWidth(BasicOverlay.owner.getWidth() - 180);
        content.setPrefHeight(BasicOverlay.owner.getHeight() - 100);

        StackPane stackPane = new StackPane();
        VBox.setMargin(stackPane, new Insets(0, 0, 12, 0));
        
        stepsBox = new HBox(
                new Label(Res.get("initNymProfile.step1").toUpperCase()),
                new Label(Res.get("initNymProfile.step2").toUpperCase()),
                new Label(Res.get("initNymProfile.step3").toUpperCase())
        );
        stepsBox.setSpacing(60);
        stepsBox.setAlignment(Pos.BASELINE_CENTER);
        stepsBox.getStyleClass().add("border-bottom-2");
        stepsBox.setPadding(new Insets(32, 0, 32, 0));

        Button skipButton = new Button(Res.get("skip").toUpperCase());
        skipButton.getStyleClass().setAll("bisq-transparent-button", "bisq-small-light-label");
        StackPane.setAlignment(skipButton, Pos.TOP_RIGHT);
        StackPane.setMargin(skipButton, new Insets(24, 24, 0, 0));
        skipButton.setOnAction(e -> controller.skipStep());
        
        stackPane.getChildren().addAll(stepsBox, skipButton);
        content.getChildren().add(stackPane);
    }


    protected void setupSelectedStep() {
        markSelectedStepLabel();
        loadSelectedStepView();
    }

    private void markSelectedStepLabel() {
        int selectedStep = model.currentStepProperty().get();

        for (int i = 0; i < stepsBox.getChildren().size(); i++) {
            Label step = (Label) stepsBox.getChildren().get(i);
            Layout.chooseStyleClass(
                    step,
                    "bisq-small-light-label",
                    "bisq-small-light-label-dimmed-2",
                    selectedStep == i
            );
        }
    }

    private void loadSelectedStepView() {
        int selectedStep = model.currentStepProperty().get();
        VBox container = popup.getMainContent();

        Node view = controller.stepsControllers.get(selectedStep).getView().getRoot();

        if (container.getChildren().size() == 1) {
            container.getChildren().add(view);
        } else {
            container.getChildren().set(1, view);
        }
    }
}
