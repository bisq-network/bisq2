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

package bisq.desktop.primary.main.content.social.components;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.observable.FxBindings;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import bisq.social.chat.ChatService;
import bisq.social.chat.PrivateChannel;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrivateChannelSelection extends ChannelSelection {
    private final Controller controller;

    public PrivateChannelSelection(DefaultApplicationService applicationService) {
        controller = new Controller(applicationService.getChatService());
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    protected static class Controller extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Controller {
        private final Model model;
        @Getter
        private final View view;

        protected Controller(ChatService chatService) {
            super(chatService);

            model = new Model();
            view = new View(model, this);
        }

        @Override
        protected ChannelSelection.Model getChannelSelectionModel() {
            return model;
        }

        @Override
        public void onActivate() {
            super.onActivate();
            chatService.removeExpiredPrivateMessages();
            channelsPin = FxBindings.<PrivateChannel, Channel<?>>bind(model.channels)
                    .to(chatService.getPrivateChannels());

            selectedChannelPin = FxBindings.subscribe(chatService.getSelectedChannel(),
                    channel -> {
                        if (channel instanceof PrivateChannel) {
                            model.selectedChannel.set(channel);
                        }
                    });
        }
    }

    protected static class Model extends bisq.desktop.primary.main.content.social.components.ChannelSelection.Model {
    }

    protected static class View extends bisq.desktop.primary.main.content.social.components.ChannelSelection.View<Model, Controller> {
        protected View(Model model, Controller controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("social.privateChannels");
        }
    }
}