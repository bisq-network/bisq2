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

import bisq.desktop.common.view.View;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NewProfilePopupView extends View<Pane, NewProfilePopupModel, NewProfilePopupController> {
    private Label stepLabel;
    private final Button skipButton;
    private Subscription stepSubscription;

    public NewProfilePopupView(NewProfilePopupModel model,
                               NewProfilePopupController controller) {
        super(new VBox(), model, controller);

        StackPane stackPane = new StackPane();
        VBox.setMargin(stackPane, new Insets(0, 0, 12, 0));

        stepLabel = new Label();
        stepLabel.getStyleClass().addAll("bisq-text-9");
        stepLabel.setAlignment(Pos.CENTER);
        stepLabel.setPadding(new Insets(32, 0, 32, 0));

        skipButton = new Button(Res.get("later"));
        skipButton.getStyleClass().setAll("bisq-transparent-button", "bisq-text-grey-10");
        StackPane.setAlignment(skipButton, Pos.TOP_RIGHT);
        StackPane.setMargin(skipButton, new Insets(24, 24, 0, 0));
        stackPane.getChildren().addAll(stepLabel, skipButton);
        root.getChildren().add(stackPane);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (root.getChildren().size() == 1) {
                root.getChildren().add(newValue.getRoot());
            } else {
                root.getChildren().set(1, newValue.getRoot());
            }
        });
    }

    @Override
    protected void onViewAttached() {
        stepSubscription = EasyBind.subscribe(model.currentStepProperty(), step -> {
            stepLabel.setText(((int) step + 1) + " / 3");
        });

        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
        stepSubscription.unsubscribe();

        skipButton.setOnAction(null);
    }
}
