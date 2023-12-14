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

import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.SearchBox;
import bisq.desktop.main.content.ContentTabView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatContainerView extends ContentTabView<ChatContainerModel, ChatContainerController> {
    private SearchBox searchBox;
    private Button infoButton;

    public ChatContainerView(ChatContainerModel model, ChatContainerController controller) {
        super(model, controller);

        model.getChannels().values().stream().sorted().forEach(channel ->
                addTab(channel.getChannelTitle(), channel.getNavigationTarget(), channel.getIconId())
        );
        addTab(Res.get("chat.private.title"), model.getPrivateChatsNavigationTarget(), "channels-private-chats");
    }

    @Override
    protected void onChildView(View<? extends Parent, ? extends Model, ? extends Controller> oldValue,
                               View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        super.onChildView(oldValue, newValue);
        controller.onSelected(model.getNavigationTarget());
    }

    @Override
    protected void setupTopBox() {
        searchBox = new SearchBox();
        Button helpButton = BisqIconButton.createIconButton("icon-help", Res.get("chat.topMenu.chatRules.tooltip"));
        helpButton.setOnAction(e -> controller.getChatSearchService().triggerHelpRequested());

        infoButton = BisqIconButton.createIconButton("icon-info", Res.get("chat.topMenu.channelInfoIcon.tooltip"));
        infoButton.setOnAction(e -> controller.getChatSearchService().triggerInfoRequested());

        HBox searchInfo = new HBox(searchBox, helpButton, infoButton);
        searchInfo.setSpacing(7);
        searchInfo.setAlignment(Pos.CENTER_RIGHT);
        HBox.setMargin(searchInfo, new Insets(0, 0, 10, 0));

        tabs.setFillHeight(true);
        tabs.setSpacing(46);
        VBox.setMargin(tabs, new Insets(0, 0, 11, 0));

        topBox = new VBox(12, searchInfo, tabs);
        topBox.setMinHeight(115);
        topBox.setPadding(DEFAULT_TOP_PANE_PADDING);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        searchBox.textProperty().bindBidirectional(model.getSearchText());
        infoButton.visibleProperty().bind(model.getHasSelectedChannel());
        infoButton.managedProperty().bind(model.getHasSelectedChannel());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        searchBox.textProperty().unbindBidirectional(model.getSearchText());
        infoButton.visibleProperty().unbind();
        infoButton.managedProperty().unbind();
    }
}
