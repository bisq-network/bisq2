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

package bisq.desktop.main.content.chat.common;

import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
import bisq.i18n.Res;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

public class ChatToolbox {
    private final Controller controller;

    public ChatToolbox() {
        controller = new Controller();
    }

    public HBox getRoot() {
        return controller.getView().getRoot();
    }

    public ReadOnlyStringProperty searchTextProperty() {
        return controller.model.getSearchText();
    }

    public void resetSearchText() {
        controller.resetSearchText();
    }

    public void setOnOpenHelpHandler(Runnable onOpenHelpHandler) {
        controller.setOnOpenHelpHandler(onOpenHelpHandler);
    }

    public void setOnToggleChannelInfoHandler(Runnable onToggleChannelInfoHandler) {
        controller.setOnToggleChannelInfoHandler(onToggleChannelInfoHandler);
    }

    @Slf4j
    public static class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private Optional<Runnable> onOpenHelpHandler = Optional.empty();
        private Optional<Runnable> onToggleChannelInfoHandler = Optional.empty();

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        private void resetSearchText() {
            model.getSearchText().set("");
        }

        private void setOnOpenHelpHandler(Runnable onOpenHelpHandler) {
            this.onOpenHelpHandler = Optional.of(onOpenHelpHandler);
        }

        private void setOnToggleChannelInfoHandler(Runnable onToggleChannelInfoHandler) {
            this.onToggleChannelInfoHandler = Optional.of(onToggleChannelInfoHandler);
        }

        @Override
        public void onActivate() {
            model.getSearchText().set("");
        }

        @Override
        public void onDeactivate() {
        }


        private void onOpenHelp() {
            onOpenHelpHandler.ifPresent(Runnable::run);
        }

        private void onToggleChannelInfo() {
            onToggleChannelInfoHandler.ifPresent(Runnable::run);
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final StringProperty searchText = new SimpleStringProperty();
    }

    @Slf4j
    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final SearchBox searchBox;
        private final Button helpButton, infoButton;

        private View(Model model, Controller controller) {
            super(new HBox(7), model, controller);

            root.setAlignment(Pos.CENTER_RIGHT);

            searchBox = new SearchBox();
            helpButton = BisqIconButton.createIconButton("icon-help", Res.get("chat.topMenu.chatRules.tooltip"));
            infoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.topMenu.channelInfoIcon.tooltip"));

            root.getChildren().addAll(searchBox, helpButton, infoButton);
        }

        @Override
        protected void onViewAttached() {
            searchBox.textProperty().bindBidirectional(model.getSearchText());
            helpButton.setOnAction(e -> controller.onOpenHelp());
            infoButton.setOnAction(e -> controller.onToggleChannelInfo());
        }

        @Override
        protected void onViewDetached() {
            searchBox.textProperty().unbindBidirectional(model.getSearchText());
            helpButton.setOnAction(null);
            infoButton.setOnAction(null);
        }
    }
}