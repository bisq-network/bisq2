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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TacView extends NavigationView<VBox, TacModel, TacController> {
    private static final double PADDING = 30;

    public TacView(TacModel model, TacController controller) {
        super(new VBox(), model, controller);

        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(OverlayModel.HEIGHT);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                childRoot.setPrefWidth(OverlayModel.WIDTH);
                childRoot.setPrefHeight(OverlayModel.HEIGHT);
                childRoot.setPadding(new Insets(PADDING));
                root.getChildren().add(childRoot);
                if (oldValue != null) {
                    Transitions.transitLeftOut(childRoot, oldValue.getRoot());
                } else {
                    Transitions.fadeIn(childRoot);
                }
            } else {
                root.getChildren().clear();
            }
        });
    }

    Node confirmationToggle() {
        return confirmCheckBox;
    }

    Node acceptAction() {
        return acceptButton;
    }

    Node rejectAction() {
        return rejectButton;
    }

    @Override
    protected void onViewAttached() {
        root.requestFocus();
    }

    @Override
    protected void onViewDetached() {
    }
}
