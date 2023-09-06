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

package bisq.desktop.main.content.chat.channels;

import bisq.chat.*;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.pub.PublicChatChannel;
import bisq.chat.pub.PublicChatChannelService;
import bisq.desktop.ServiceProvider;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.i18n.Res;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

public abstract class PublicChannelSelectionMenu<C extends PublicChatChannel<?>,
        S extends PublicChatChannelService<?, C, ?>,
        E extends ChatChannelSelectionService> extends ChannelSelectionMenu<C, S, E> {

    public PublicChannelSelectionMenu() {
        super();
    }

    protected abstract static class Controller<V extends ChannelSelectionMenu.View<M, ?>,
            M extends Model,
            C extends ChatChannel<?>,
            S extends ChatChannelService<?, C, ?>,
            E extends ChatChannelSelectionService>
            extends ChannelSelectionMenu.Controller<V, M, C, S, E> {

        public Controller(ServiceProvider serviceProvider, ChatChannelDomain chatChannelDomain) {
            super(serviceProvider, chatChannelDomain);
        }

        @Override
        public void onActivate() {
            super.onActivate();
        }

        @Override
        protected boolean isChannelExpectedInstance(ChatChannel<? extends ChatMessage> chatChannel) {
            return chatChannel instanceof PublicChatChannel;
        }
    }

    protected abstract static class View<M extends ChannelSelectionMenu.Model,
            C extends ChannelSelectionMenu.Controller<?, M, ?, ?, ?>> extends ChannelSelectionMenu.View<M, C> {
        protected View(M model, C controller) {
            super(model, controller);
        }

        @Override
        protected String getHeadlineText() {
            return Res.get("chat.channelSelection.public.headline");
        }

        protected ListCell<ChannelItem> getListCell() {
            return new ListCell<>() {
                private Subscription widthSubscription;
                final Label label = new Label();
                final ImageView iconImageView = new ImageView();
                final HBox hBox = new HBox();
                final Badge numMessagesBadge = new Badge();
                @Nullable
                MapChangeListener<String, Integer> channelIdWithNumUnseenMessagesMapListener;

                {
                    setCursor(Cursor.HAND);
                    setPrefHeight(40);
                    setPadding(new Insets(0, 0, -20, 0));
                    setPadding(new Insets(0, 0, -20, 0));
                    label.setGraphic(iconImageView);
                    label.setGraphicTextGap(8);
                    hBox.setSpacing(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    numMessagesBadge.setPosition(Pos.CENTER);
                    HBox.setMargin(numMessagesBadge, new Insets(0, 12, 0, 0));
                    hBox.getChildren().addAll(label, Spacer.fillHBox(), numMessagesBadge);
                }

                @Override
                protected void updateItem(View.ChannelItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && !empty && item.getChatChannel() instanceof CommonPublicChatChannel) {
                        widthSubscription = setupCellBinding(this, item, label, iconImageView);
                        updateCell(this, item, label, iconImageView);

                        channelIdWithNumUnseenMessagesMapListener = change -> onUnseenMessagesChanged(item, change.getKey(), numMessagesBadge);
                        model.channelIdWithNumUnseenMessagesMap.addListener(channelIdWithNumUnseenMessagesMapListener);
                        model.channelIdWithNumUnseenMessagesMap.keySet().forEach(key -> onUnseenMessagesChanged(item, key, numMessagesBadge));

                        setGraphic(hBox);
                    } else {
                        setGraphic(null);
                        if (widthSubscription != null) {
                            widthSubscription.unsubscribe();
                            widthSubscription = null;
                        }

                        if (channelIdWithNumUnseenMessagesMapListener != null) {
                            model.channelIdWithNumUnseenMessagesMap.removeListener(channelIdWithNumUnseenMessagesMapListener);
                            channelIdWithNumUnseenMessagesMapListener = null;
                        }
                    }
                }
            };
        }
    }
}