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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.desktop.ServiceProvider;
import bisq.desktop.main.content.authorized_role.info.RoleInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Setter;

public class SecurityManager {
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;

        public Controller(ServiceProvider serviceProvider) {
            SecurityManagerTabController releaseManagerTabController = new SecurityManagerTabController(serviceProvider);
            Model model = new Model();
            DifficultyAdjustmentController difficultyAdjustmentController = new DifficultyAdjustmentController(serviceProvider);
            RoleInfo roleInfo = new RoleInfo(serviceProvider);
            view = new View(model,
                    this,
                    releaseManagerTabController.getView().getRoot(),
                    difficultyAdjustmentController.getView().getRoot(),
                    roleInfo.getRoot());
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    @Getter
    @Setter
    public static class Model implements bisq.desktop.common.view.Model {
    }

    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        public View(Model model,
                    Controller controller,
                    Pane tabViewRoot,
                    Pane difficultyAdjustment,
                    Pane roleInfo) {
            super(new VBox(30), model, controller);

            root.setPadding(new Insets(0, 40, 40, 40));
            root.setAlignment(Pos.TOP_LEFT);

            VBox.setMargin(roleInfo, new Insets(0, -40, 0, -40));
            root.getChildren().addAll(tabViewRoot, difficultyAdjustment, roleInfo);
        }

        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }
}
