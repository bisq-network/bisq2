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

import bisq.application.DefaultApplicationService;
import bisq.chat.channel.ChatChannelDomain;
import bisq.chat.channel.ChatChannelSelectionService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonPublicChatChannelSelection extends PublicChatChannelSelection<
        CommonPublicChatChannel,
        CommonPublicChatChannelService,
        ChatChannelSelectionService
        > {
    @Getter
    private final Controller controller;

    public CommonPublicChatChannelSelection(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        super();

        controller = new Controller(applicationService, chatChannelDomain);
    }

    protected static class Controller extends PublicChatChannelSelection.Controller<
            View,
            Model,
            CommonPublicChatChannel,
            CommonPublicChatChannelService,
            ChatChannelSelectionService
            > {

        protected Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            super(applicationService, chatChannelDomain);
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

        @Override
        public void onActivate() {
            super.onActivate();

            selectedChannelPin = FxBindings.subscribe(chatChannelSelectionService.getSelectedChannel(),
                    chatChannel -> UIThread.runOnNextRenderFrame(() -> {
                                if (chatChannel instanceof CommonPublicChatChannel) {
                                    model.selectedChannelItem.set(findOrCreateChannelItem(chatChannel));
                                } else if (chatChannel == null && !model.channelItems.isEmpty()) {
                                    model.selectedChannelItem.set(model.channelItems.get(0));
                                } else {
                                    model.selectedChannelItem.set(null);
                                }
                            }
                    ));
        }

        @Override
        protected void onSelected(ChatChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }

            chatChannelSelectionService.selectChannel(channelItem.getChatChannel());
        }
    }

    protected static class Model extends ChatChannelSelection.Model {
        public Model(ChatChannelDomain chatChannelDomain) {
            super(chatChannelDomain);
        }
    }

    protected static class View extends PublicChatChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.publicChannels");
        }
    }
}