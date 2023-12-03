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

package bisq.desktop.main.content.common_chat;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.ContentTabView;
import javafx.scene.Parent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatContainerView extends ContentTabView<ChatContainerModel, ChatContainerController> {
    public ChatContainerView(ChatContainerModel model, ChatContainerController controller) {
        super(model, controller);

        model.getChannels().forEach(channel -> addTab(channel.getChannelTitle(), channel.getNavigationTarget()));
        addTab("Private chats", NavigationTarget.DISCUSSION_PRIVATECHATS); // TODO: needs to pass the correct one depending on domain
    }

    @Override
    protected void onChildView(View<? extends Parent, ? extends Model, ? extends Controller> oldValue,
                               View<? extends Parent, ? extends Model, ? extends Controller> newValue) {
        super.onChildView(oldValue, newValue);
        controller.onSelected(this.model.getNavigationTarget());
    }
}
