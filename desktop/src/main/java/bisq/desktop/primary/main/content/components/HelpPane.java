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

package bisq.desktop.primary.main.content.components;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HelpPane {

    private final Controller controller;

    public HelpPane() {
        controller = new Controller();
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;

        private Controller() {
            Model model = new Model();
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(5);

          /*  Label headline = new Label(Res.get("social.chat.help"));
            headline.setId("chat-sidebar-headline");
            headline.setPadding(new Insets(0, 40, 0, 0));
            VBox.setMargin(headline, new Insets(0, 0, 20, 0));*/

            Label label = new Label("WIP");
            label.setStyle("-fx-text-fill: -bisq-grey-8; -fx-font-size: 8em");
            Label small = new Label("Help content...");
            small.setStyle("-fx-text-fill: -bisq-grey-8; -fx-font-size: 1em");
            root.getChildren().addAll(label, small);
        }


        @Override
        protected void onViewAttached() {
        }

        @Override
        protected void onViewDetached() {
        }
    }
}