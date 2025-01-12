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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.chat.ChatMessageType;
import bisq.chat.Citation;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.presentation.formatters.DateFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;

import static bisq.desktop.main.content.chat.message_container.ChatMessageContainerView.EDITED_POST_FIX;

public class ChannelMessagesDisplayList<M extends PublicChatMessage> {
    private final Controller controller;

    public ChannelMessagesDisplayList(ServiceProvider serviceProvider,
                                      PublicChatChannel<M> publicChatChannel) {
        controller = new Controller(serviceProvider, publicChatChannel);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    private class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final PublicChatChannel<M> publicChatChannel;
        private final UserProfileService userProfileService;
        private Pin publicMessagesPin;

        private Controller(ServiceProvider serviceProvider,
                           PublicChatChannel<M> publicChatChannel) {
            model = new Model();
            view = new View(model, this);
            this.publicChatChannel = publicChatChannel;
            userProfileService = serviceProvider.getUserService().getUserProfileService();
        }

        @Override
        public void onActivate() {
            publicMessagesPin = publicChatChannel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M element) {
                    if (element.getChatMessageType() == ChatMessageType.TEXT) {
                        UIThread.runOnNextRenderFrame(() -> {
                            boolean shouldAddMessage = model.getChannelMessageItems().stream()
                                    .noneMatch(item -> item.getPublicChatMessage().equals(element));
                            if (shouldAddMessage) {
                                ChannelMessageItem chatMessageItem = new ChannelMessageItem(element,
                                        userProfileService.findUserProfile(element.getAuthorUserProfileId()).orElseThrow());
                                model.getChannelMessageItems().add(chatMessageItem);
                            }
                        });
                    }
                }

                @Override
                public void remove(Object element) {
                    if (element instanceof PublicChatMessage && ((PublicChatMessage) element).getChatMessageType() == ChatMessageType.TEXT) {
                        UIThread.runOnNextRenderFrame(() -> {
                            PublicChatMessage publicChatMessage = (PublicChatMessage) element;
                            Optional<ChannelMessageItem> toRemove = model.getChannelMessageItems().stream()
                                    .filter(item -> item.getPublicChatMessage().getId().equals(publicChatMessage.getId()))
                                    .findAny();
                            toRemove.ifPresent(item -> {
                                item.dispose();
                                model.getChannelMessageItems().remove(item);
                            });
                        });
                    }
                }

                @Override
                public void clear() {
                    UIThread.runOnNextRenderFrame(() -> {
                        model.getChannelMessageItems().forEach(ChannelMessageItem::dispose);
                        model.getChannelMessageItems().clear();
                    });
                }
            });
        }

        @Override
        public void onDeactivate() {
            publicMessagesPin.unbind();
        }
    }

    @Getter
    private static class Model implements bisq.desktop.common.view.Model {
        private final ObservableList<ChannelMessageItem> channelMessageItems = FXCollections.observableArrayList();
    }

    private class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);


        }

        @Override
        protected void onViewAttached() {

        }

        @Override
        protected void onViewDetached() {

        }
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ChannelMessageItem {
        @EqualsAndHashCode.Include
        private final PublicChatMessage publicChatMessage;

        private final UserProfile senderUserProfile;
        private final String message;
        private final String date;
        private final Optional<Citation> citation;

        public ChannelMessageItem(PublicChatMessage publicChatMessage, UserProfile senderUserProfile) {
            this.publicChatMessage = publicChatMessage;
            this.senderUserProfile = senderUserProfile;

            String editPostFix = publicChatMessage.isWasEdited() ? EDITED_POST_FIX : "";
            message = publicChatMessage.getText() + editPostFix;
            date = DateFormatter.formatDateTime(new Date(publicChatMessage.getDate()),
                    DateFormat.MEDIUM, DateFormat.SHORT, true, " " + Res.get("temporal.at") + " ");
            citation = publicChatMessage.getCitation();
        }

        private void initialize() {

        }

        public void dispose() {

        }
    }
}
