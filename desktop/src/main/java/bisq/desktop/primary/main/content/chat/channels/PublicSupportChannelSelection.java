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
import bisq.chat.channel.ChannelSelectionService;
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
public class PublicSupportChannelSelection extends PublicChannelSelection {
    private final Controller controller;

    public PublicSupportChannelSelection(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService);
    }

    @Override
    public Pane getRoot() {
        return controller.view.getRoot();
    }

    @Override
    public void deSelectChannel() {
        controller.deSelectChannel();
    }

    protected static class Controller extends ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;
        private final CommonPublicChatChannelService publicSupportChannelService;
        private final ChannelSelectionService supportChannelSelectionService;

        protected Controller(DefaultApplicationService applicationService) {
            super(applicationService);

            publicSupportChannelService = chatService.getPublicSupportChannelService();
            supportChannelSelectionService = chatService.getSupportChannelSelectionService();

            model = new Model();
            view = new View(model, this);

            model.filteredList.setPredicate(item -> true);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        protected ChatChannelService<?, ?, ?> getChannelService() {
            return publicSupportChannelService;
        }

        @Override
        public void onActivate() {
            super.onActivate();

            channelsPin = FxBindings.<CommonPublicChatChannel, ChannelSelection.View.ChannelItem>bind(model.channelItems)
                    .map(ChannelSelection.View.ChannelItem::new)
                    .to(publicSupportChannelService.getChannels());

            selectedChannelPin = FxBindings.subscribe(supportChannelSelectionService.getSelectedChannel(),
                    channel -> UIThread.runOnNextRenderFrame(() -> {
                        if (channel instanceof CommonPublicChatChannel) {
                            model.selectedChannelItem.set(new ChannelSelection.View.ChannelItem(channel));
                        } else if (channel == null && !model.channelItems.isEmpty()) {
                            model.selectedChannelItem.set(model.channelItems.get(0));
                        } else {
                            model.selectedChannelItem.set(null);
                        }
                            }
                    ));
        }

        @Override
        protected void onSelected(ChannelSelection.View.ChannelItem channelItem) {
            if (channelItem == null) {
                return;
            }

            supportChannelSelectionService.selectChannel(channelItem.getChatChannel());
        }

        public void deSelectChannel() {
            model.selectedChannelItem.set(null);
        }
    }

    protected static class Model extends ChannelSelection.Model {
    }

    protected static class View extends PublicChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.publicChannels");
        }
    }
}