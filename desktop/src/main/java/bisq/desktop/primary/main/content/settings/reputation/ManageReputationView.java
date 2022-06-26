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

package bisq.desktop.primary.main.content.settings.reputation;

import bisq.desktop.common.view.View;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManageReputationView extends View<VBox, ManageReputationModel, ManageReputationController> {

    private final Button addNewUserButton;

    public ManageReputationView(ManageReputationModel model,
                                ManageReputationController controller,
                                UserProfileSelection userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(30);
        root.setPadding(new Insets(20));

        Label selectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        selectLabel.getStyleClass().add("bisq-text-4");
        VBox selectionVBox = new VBox(0, selectLabel, userProfileSelection.getRoot());

        addNewUserButton = new Button(Res.get("settings.userProfile.addNewUser"));
        addNewUserButton.setDefaultButton(true);

        root.getChildren().addAll(selectionVBox, addNewUserButton);
    }

    @Override
    protected void onViewAttached() {

    }

    @Override
    protected void onViewDetached() {
    }
}
