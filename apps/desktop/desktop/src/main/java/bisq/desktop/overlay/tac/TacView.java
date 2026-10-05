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

package bisq.desktop.overlay.tac;

import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.NavigationView;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacView extends NavigationView<VBox, TacModel, TacController> {
    private static final double PADDING = 30;
    private static final double STEP_TOP_PADDING = 18;

    public TacView(TacModel model, TacController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        Label headline = new Label(Res.get("tac.headline"));
        headline.getStyleClass().add("tac-headline");
        headline.setWrapText(true);
        headline.setAlignment(Pos.CENTER);
        headline.setMaxWidth(Double.MAX_VALUE);

        VBox header = new VBox(headline);
        VBox.setMargin(headline, new Insets(-10, 0, -10, 0));
        header.setPadding(new Insets(PADDING, PADDING, 0, PADDING));

        // The header stays in place while the steps slide in and out. A StackPane lets the
        // outgoing and the incoming step use the same area during the transition.
        StackPane stepContainer = new StackPane();
        VBox.setVgrow(stepContainer, Priority.ALWAYS);

        root.getChildren().addAll(header, stepContainer);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                childRoot.setPadding(new Insets(STEP_TOP_PADDING, PADDING, PADDING, PADDING));
                stepContainer.getChildren().add(childRoot);
                if (oldValue != null) {
                    Transitions.transitLeftOut(childRoot, oldValue.getRoot());
                } else {
                    Transitions.fadeIn(childRoot);
                }
            } else {
                stepContainer.getChildren().clear();
            }
        });
    }

    @Override
    protected void onViewAttached() {
        root.requestFocus();
    }

    @Override
    protected void onViewDetached() {
    }
}
