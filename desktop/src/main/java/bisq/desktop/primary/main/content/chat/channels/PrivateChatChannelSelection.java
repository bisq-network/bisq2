/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Private License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Private
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Private License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.chat.channels;

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannel;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.priv.PrivateChatChannel;
import bisq.chat.channel.priv.PrivateChatChannelService;
import bisq.chat.message.ChatMessage;
import javafx.scene.control.ListCell;

public abstract class PrivateChatChannelSelection<
        C extends PrivateChatChannel<?>,
        S extends PrivateChatChannelService<?, C, ?>,
        E extends ChatChannelSelectionService
        > extends ChatChannelSelection<C, S, E> {

    public PrivateChatChannelSelection() {
        super();
    }

    protected static abstract class Controller<
            V extends ChatChannelSelection.View<M, ?>,
            M extends Model,
            C extends PrivateChatChannel<?>,
            S extends PrivateChatChannelService<?, C, ?>,
            E extends ChatChannelSelectionService
            >
            extends ChatChannelSelection.Controller<V, M, C, S, E> {

        public Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            super(applicationService, chatChannelDomain);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        protected void handleSelectedChannelChange(ChatChannel<? extends ChatMessage> chatChannel) {
            if (chatChannel instanceof PrivateChatChannel) {
                PrivateChatChannel<?> privateChatChannel = (PrivateChatChannel<?>) chatChannel;
                model.selectedChannelItem.set(findOrCreateChannelItem(privateChatChannel));
                userIdentityService.selectChatUserIdentity(privateChatChannel.getMyUserIdentity());
            } else {
                model.selectedChannelItem.set(null);
            }
        }
    }

    protected static abstract class View<
            M extends Model,
            C extends ChatChannelSelection.Controller<?, M, ?, ?, ?>
            > extends ChatChannelSelection.View<M, C> {
        protected View(M model, C controller) {
            super(model, controller);
        }

        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                @Override
                protected void updateItem(ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                }
            };
        }
    }
}