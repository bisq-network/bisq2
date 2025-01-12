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
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatMessage;
import bisq.common.currency.Market;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.table.BisqTableView;
import bisq.user.profile.UserProfile;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import lombok.Getter;
import java.util.Optional;

public class ChannelMessagesDisplayList<M extends PublicChatMessage> {
    private final Controller controller;

    public ChannelMessagesDisplayList(ServiceProvider serviceProvider,
                                      PublicChatChannel<M> publicChatChannel,
                                      UserProfile userProfile) {
        controller = new Controller(serviceProvider, publicChatChannel, userProfile);
    }

    public VBox getRoot() {
        return controller.view.getRoot();
    }

    private class Controller implements bisq.desktop.common.view.Controller {
        @Getter
        private final View view;
        private final Model model;
        private final PublicChatChannel<M> publicChatChannel;
        private final UserProfile userProfile;
        private Pin publicMessagesPin;

        private Controller(ServiceProvider serviceProvider,
                           PublicChatChannel<M> publicChatChannel,
                           UserProfile userProfile) {
            model = new Model();
            view = new View(model, this);
            this.publicChatChannel = publicChatChannel;
            this.userProfile = userProfile;
        }

        @Override
        public void onActivate() {
            model.getChannelName().set(publicChatChannel.getDisplayString());
            if (publicChatChannel instanceof BisqEasyOfferbookChannel bisqEasyOfferbookChannel) {
                model.getMarket().set(bisqEasyOfferbookChannel.getMarket());
            }

            publicMessagesPin = publicChatChannel.getChatMessages().addObserver(new CollectionObserver<>() {
                @Override
                public void add(M element) {
                    if (element.getChatMessageType() == ChatMessageType.TEXT) {
                        UIThread.runOnNextRenderFrame(() -> {
                            boolean shouldAddMessage = model.getChannelMessageItems().stream()
                                    .noneMatch(item -> item.getPublicChatMessage().equals(element))
                                    && element.getAuthorUserProfileId().equals(userProfile.getId());
                            if (shouldAddMessage) {
                                ChannelMessageItem chatMessageItem = new ChannelMessageItem(element, userProfile);
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
        private final StringProperty channelName = new SimpleStringProperty();
        private final ObjectProperty<Market> market = new SimpleObjectProperty<>();
        private final ObservableList<ChannelMessageItem> channelMessageItems = FXCollections.observableArrayList();
    }

    private class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final Label headline;
        private final BisqTableView<ChannelMessageItem> messagesList;

        private View(Model model, Controller controller) {
            super(new VBox(20), model, controller);

            headline = new Label();
            messagesList = new BisqTableView<>(model.getChannelMessageItems());
            configTableView();

            root.getChildren().addAll(headline, messagesList);
        }

        @Override
        protected void onViewAttached() {
            headline.setText(model.getChannelName().get());
            messagesList.initialize();
        }

        @Override
        protected void onViewDetached() {

        }

        private void configTableView() {

        }
    }
}
