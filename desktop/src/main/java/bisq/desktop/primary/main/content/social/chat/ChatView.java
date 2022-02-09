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

package bisq.desktop.primary.main.content.social.chat;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.layout.Layout;
import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatView extends View<HBox, ChatModel, ChatController> {

    private final ListView<Object> messageListView;
    private final BisqInputTextField inputField;
    private final BisqLabel selectedChannelLabel;

    public ChatView(ChatModel model, ChatController controller, Pane userProfile) {
        super(new HBox(), model, controller);

        root.setSpacing(Layout.SPACING);
        root.setPadding(new Insets(20, 20, 20, 0));

        VBox left = new VBox();

        HBox leftToolbar = new HBox();
        leftToolbar.getChildren().addAll(userProfile); //todo make small design for profile
        VBox publicChannels = new VBox();
        VBox privateChannels = new VBox();
        left.getChildren().addAll(leftToolbar, publicChannels, privateChannels);

        VBox center = new VBox();
        HBox centerToolbar = new HBox();
        selectedChannelLabel = new BisqLabel();
        centerToolbar.getChildren().addAll(selectedChannelLabel);
        inputField = new BisqInputTextField();
        messageListView = new ListView<>();
        center.getChildren().addAll(centerToolbar, messageListView, inputField);


        VBox right = new VBox();
        root.getChildren().addAll(left, center, right);
    }

    @Override
    public void onViewAttached() {
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
    }

    @Override
    protected void onViewDetached() {
    }

    private class ChatMessageListItem {
    }
}
