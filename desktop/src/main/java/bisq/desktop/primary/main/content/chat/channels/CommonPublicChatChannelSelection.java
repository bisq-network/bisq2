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
import bisq.chat.channel.ChatChannelService;
import bisq.chat.channel.pub.CommonPublicChatChannel;
import bisq.chat.channel.pub.CommonPublicChatChannelService;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonPublicChatChannelSelection extends PublicChatChannelSelection<CommonPublicChatChannel, CommonPublicChatChannelService, ChatChannelSelectionService> {
    private final Controller controller;

    public CommonPublicChatChannelSelection(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
        controller = new Controller(applicationService, chatChannelDomain);
    }

    @Override
    public Pane getRoot() {
        return controller.view.getRoot();
    }

    @Override
    public void deSelectChannel() {
        controller.deSelectChannel();
    }

    protected static class Controller extends ChatChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final CommonPublicChatChannelService channelService;
        private final ChatChannelSelectionService selectionService;

        protected Controller(DefaultApplicationService applicationService, ChatChannelDomain chatChannelDomain) {
            super(applicationService);

            channelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
            selectionService = chatService.getChatChannelSelectionServices().get(chatChannelDomain);

            model = new Model();
            view = new View(model, this);

            model.filteredList.setPredicate(item -> true);
        }

        @Override
        protected ChatChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        protected ChatChannelService<?, ?, ?> getChannelService() {
            return channelService;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelsPin = FxBindings.<CommonPublicChatChannel, ChatChannelSelection.View.ChannelItem>bind(model.channelItems)
                    .map(this::findOrCreateChannelItem)
                    .to(channelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(selectionService.getSelectedChannel(),
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

            selectionService.selectChannel(channelItem.getChatChannel());
        }

        public void deSelectChannel() {
            model.selectedChannelItem.set(null);
        }
    }

    protected static class Model extends ChatChannelSelection.Model {
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