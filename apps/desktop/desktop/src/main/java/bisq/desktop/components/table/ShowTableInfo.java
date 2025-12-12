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

package bisq.desktop.components.table;

import bisq.desktop.ServiceProvider;
import bisq.desktop.common.view.InitWithDataController;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.overlay.OverlayController;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

public class ShowTableInfo {
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class InitData {
        private final String title;
        private final String content;

        public InitData(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    @Getter
    private final Controller controller;

    public ShowTableInfo(ServiceProvider serviceProvider) {
        controller = new Controller(serviceProvider);
    }

    @Slf4j
    private static class Controller implements InitWithDataController<InitData> {
        @Getter
        private final View view;
        private final Model model;

        private Controller(ServiceProvider serviceProvider) {
            model = new Model();
            view = new View(model, this);
        }

        @Override
        public void initWithData(InitData initData) {
            model.setTitle(initData.getTitle());
            model.setContent(initData.getContent());
        }

        @Override
        public void onActivate() {
        }

        @Override
        public void onDeactivate() {
        }

        private void onClose() {
            OverlayController.hide();
        }
    }

    @Slf4j
    @Getter
    @Setter
    private static class Model implements bisq.desktop.common.view.Model {
        public String title;
        public String content;
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Button closeIconButton, closeButton;
        private final Label titleLabel, contentLabel;

        public View(Model model, Controller controller) {
            super(new VBox(30), model, controller);

            root.setPrefWidth(OverlayModel.WIDTH - 20);
            root.setPrefHeight(OverlayModel.HEIGHT - 20);
            root.setAlignment(Pos.TOP_LEFT);

            closeIconButton = BisqIconButton.createIconButton("close");
            HBox closeButtonRow = new HBox(Spacer.fillHBox(), closeIconButton);
            closeButtonRow.setPadding(new Insets(15, 15, 0, 0));

            titleLabel = new Label();
            titleLabel.getStyleClass().add("bisq-text-headline-2");
            HBox headlineBox = new HBox(Spacer.fillHBox(), titleLabel, Spacer.fillHBox());

            contentLabel = new Label();
            contentLabel.getStyleClass().addAll("popup-text", "wrap-text");
            contentLabel.setPadding(new Insets(0, 70, 0, 70));

            closeButton = new Button(Res.get("action.close"));
            HBox closeButtonBox = new HBox(20, closeButton);
            closeButtonBox.setAlignment(Pos.CENTER);

            root.getChildren().setAll(closeButtonRow, headlineBox, contentLabel, closeButtonBox);
        }

        @Override
        protected void onViewAttached() {
            titleLabel.setText(model.getTitle());
            contentLabel.setText(model.getContent());

            closeIconButton.setOnAction(e -> controller.onClose());
            closeButton.setOnAction(e -> controller.onClose());
        }

        @Override
        protected void onViewDetached() {
            titleLabel.setText("");
            contentLabel.setText("");

            closeIconButton.setOnAction(null);
            closeButton.setOnAction(null);
        }
    }
}
