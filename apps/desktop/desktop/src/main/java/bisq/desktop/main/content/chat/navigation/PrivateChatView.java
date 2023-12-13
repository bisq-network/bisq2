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

package bisq.desktop.main.content.chat.navigation;

import bisq.desktop.common.Layout;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.common_chat.CommonChatView;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChatView extends CommonChatView {
    private VBox left;

    public PrivateChatView(PrivateChatModel model,
                           PrivateChatController controller,
                           Region twoPartyPrivateChatChannelSelection,
                           Pane chatMessagesComponent,
                           Pane channelInfo) {
        super(model, controller, chatMessagesComponent, channelInfo);

        left.getChildren().addAll(twoPartyPrivateChatChannelSelection, Spacer.fillVBox());
        left.setPrefWidth(210);
        left.setMinWidth(210);
        left.setFillWidth(true);
        left.getStyleClass().add("bisq-grey-2-bg");
    }

    protected void configContainerHBox() {
        containerHBox.setFillHeight(true);
        Layout.pinToAnchorPane(containerHBox, 0, 0, 0, 0);

        AnchorPane wrapper = new AnchorPane();
        wrapper.setPadding(new Insets(0, SIDE_PADDING, 0, SIDE_PADDING));
        wrapper.getChildren().add(containerHBox);

        root.setContent(wrapper);

        left = new VBox();
        HBox.setHgrow(left, Priority.NEVER);
        HBox.setHgrow(centerVBox, Priority.ALWAYS);
        HBox.setHgrow(sideBar, Priority.NEVER);
        containerHBox.getChildren().addAll(left, centerVBox, sideBar);
    }
}
