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

package bisq.desktop.main.content.chat.message_container.list.reactions_box;

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ReactionItem;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.layout.HBox;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ActiveReactionsDisplayBox extends HBox {
    private final ObservableList<ReactionItem> reactionItems = FXCollections.observableArrayList();
    private final FilteredList<ReactionItem> filteredReactionItems = new FilteredList<>(reactionItems, ReactionItem::hasActiveReactions);
    private final SortedList<ReactionItem> sortedReactionItems = new SortedList<>(filteredReactionItems);
    private final Map<ReactionItem, ChangeListener<Number>> itemChangeListeners = new HashMap<>();
    private final ListChangeListener<ReactionItem> listChangeListener;
    private final ToggleReaction toggleReaction;

    public ActiveReactionsDisplayBox(Collection<ReactionItem> reactionItems, ToggleReaction toggleReaction) {
        this.reactionItems.addAll(reactionItems);
        this.toggleReaction = toggleReaction;
        listChangeListener = change -> updateItems();
        sortedReactionItems.setComparator(ReactionItem.firstAddedComparator());
        initialize();
        updateItems();
        getStyleClass().add("active-reactions-display-box");
    }

    private void initialize() {
        reactionItems.forEach(this::addItemListeners);
        sortedReactionItems.addListener(listChangeListener);
    }

    public void dispose() {
        removeItemListeners();
        sortedReactionItems.removeListener(listChangeListener);
    }

    private void addItemListeners(ReactionItem item) {
        ChangeListener<Number> listener = (obs, oldValue, newValue) -> {
            int idx = reactionItems.indexOf(item);
            if (idx >= 0) {
                reactionItems.set(idx, item);
            }
        };
        item.getCount().addListener(listener);
        itemChangeListeners.put(item, listener);
    }

    private void removeItemListeners() {
        itemChangeListeners.forEach((item, listener) -> {
            item.getCount().removeListener(listener);
        });
    }

    private void updateItems() {
        UIThread.run(() -> {
            getChildren().clear();
            getChildren().addAll(sortedReactionItems.stream()
                    .map(item -> new ActiveReactionMenuItem(item, toggleReaction))
                    .collect(Collectors.toList()));
        });
    }

    @Getter
    private static final class ActiveReactionMenuItem extends BisqMenuItem {
        private ReactionItem reactionItem;
        private ToggleReaction toggleReaction;

        public ActiveReactionMenuItem(ReactionItem reactionItem, ToggleReaction toggleReaction) {
            this(reactionItem.getIconId());

            this.reactionItem = reactionItem;
            this.toggleReaction = toggleReaction;
            setText(reactionItem.getCountAsString());
            setGraphicTextGap(4);
            addStyleClasses();
            setOnAction(e -> toggleReaction.execute(reactionItem));
        }

        private ActiveReactionMenuItem(String iconId) {
            super(iconId, iconId);
        }

        private void addStyleClasses() {
            getStyleClass().add("active-reaction-menu-item");
            if (reactionItem.getSelected().get()) {
                getStyleClass().add("active-reaction-selected");
            } else {
                getStyleClass().remove("active-reaction-selected");
            }
        }
    }
}
