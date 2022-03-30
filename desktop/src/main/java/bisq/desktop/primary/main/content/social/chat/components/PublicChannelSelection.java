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

package bisq.desktop.primary.main.content.social.chat.components;

import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import bisq.social.chat.PublicChannel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;


@Slf4j
public class PublicChannelSelection extends ChannelSelection {

    public PublicChannelSelection(ChatService chatService) {
        super();
        controller = new ChannelSelection.Controller(chatService, Res.get("social.publicChannels")) {

            @Override
            public void onViewAttached() {
                super.onViewAttached();
                channelsPin = FxBindings.<PublicChannel, Channel> bind(model.channels)
                        .to(this.chatService.getPersistableStore().getPublicChannels());

                if (!model.channels.isEmpty()) {
                    chatService.selectChannel(model.channels.get(0));
                }
            }
        };
    }
}