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

package bisq.desktop.main.content.chat.common.pub;

import bisq.desktop.main.content.chat.ChatModel;
import bisq.desktop.main.content.chat.ChatView;
import bisq.desktop.components.controls.BisqIconButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

public final class CommonPublicChatView extends ChatView<CommonPublicChatView, CommonPublicChatModel> {
    public CommonPublicChatView(ChatModel model, CommonPublicChatController controller,
                                Pane chatMessagesComponent, Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);
    }

    @Override
    protected void configTitleHBox() {
        titleHBox.setAlignment(Pos.CENTER);
        titleHBox.setPadding(new Insets(12.5, 25, 12.5, 25));
        titleHBox.getStyleClass().add("bisq-easy-container-header");
        titleHBox.setMinHeight(HEADER_HEIGHT);
        titleHBox.setMaxHeight(HEADER_HEIGHT);

        HBox headerTitle = new HBox(10, channelTitle, channelDescription);
        headerTitle.setAlignment(Pos.BASELINE_LEFT);
        headerTitle.setPadding(new Insets(7, 0, 0, 0));
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        channelTitle.getStyleClass().add("chat-header-title");
        channelDescription.getStyleClass().add("chat-header-description");

        searchBox.setMaxWidth(200);
        double searchBoxHeight = 29;
        searchBox.setMinHeight(searchBoxHeight);
        searchBox.setMaxHeight(searchBoxHeight);
        searchBox.setPrefHeight(searchBoxHeight);

        double scale = 1.15;
        helpButton = BisqIconButton.createIconButton("icon-help");
        helpButton.setScaleX(scale);
        helpButton.setScaleY(scale);
        infoButton = BisqIconButton.createIconButton("icon-info");
        infoButton.setScaleX(scale);
        infoButton.setScaleY(scale);

        HBox.setMargin(channelIcon, new Insets(0, 0, -2, 5));
        HBox.setMargin(helpButton, new Insets(-2, 0, 0, 0));
        HBox.setMargin(infoButton, new Insets(-2, 0, 0, 0));
        titleHBox.getChildren().addAll(channelIcon, headerTitle, searchBox, helpButton, infoButton);
    }
}
