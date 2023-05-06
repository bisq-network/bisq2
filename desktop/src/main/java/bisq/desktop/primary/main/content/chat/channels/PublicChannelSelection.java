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

import bisq.chat.channel.PublicChatChannel;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import javafx.collections.MapChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.fxmisc.easybind.Subscription;

import javax.annotation.Nullable;

public abstract class PublicChannelSelection extends ChannelSelection {

    abstract public void deSelectChannel();

    abstract public Pane getRoot();

    protected static abstract class View<M extends ChannelSelection.Model, C extends ChannelSelection.Controller> extends ChannelSelection.View<M, C> {
        protected View(M model, C controller) {
            super(model, controller);
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
                    if (item != null && !empty && item.getChatChannel() instanceof PublicChatChannel) {
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