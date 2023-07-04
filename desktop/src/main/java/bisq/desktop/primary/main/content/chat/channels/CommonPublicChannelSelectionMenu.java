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

package bisq.desktop.primary.main.content.chat.channels;

import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.desktop.ServiceProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonPublicChannelSelectionMenu extends PublicChannelSelectionMenu<
        CommonPublicChatChannel,
        CommonPublicChatChannelService,
        ChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public CommonPublicChannelSelectionMenu(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
        super();

        controller = new Controller(serviceProvider, chatChannelDomain);
    }

    protected static class Controller extends PublicChannelSelectionMenu.Controller<
            View,
            Model,
            CommonPublicChatChannel,
            CommonPublicChatChannelService,
            ChatChannelSelectionService
            > {

        protected Controller(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
            super(serviceProvider, chatChannelDomain);
        }

        @Override
        protected CommonPublicChatChannelService createAndGetChatChannelService(ChatChannelDomain chatChannelDomain) {
            return chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        }

        @Override
        protected ChatChannelSelectionService createAndGetChatChannelSelectionService(ChatChannelDomain chatChannelDomain) {
            return chatService.getChatChannelSelectionServices().get(chatChannelDomain);
        }

        @Override
        protected View createAndGetView() {
            return new View(model, this);
        }

        @Override
        protected Model createAndGetModel(ChatChannelDomain chatChannelDomain) {
            return new Model(chatChannelDomain);
        }
    }

    protected static class Model extends PublicChannelSelectionMenu.Model {
        public Model(ChatChannelDomain chatChannelDomain) {
            super(chatChannelDomain);
        }
    }

    protected static class View extends PublicChannelSelectionMenu.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }
    }
}