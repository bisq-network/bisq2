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

import bisq.chat.reactions.Reaction;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.DrawerMenu;
import bisq.desktop.main.content.chat.message_container.list.ReactionItem;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReactMenuBox extends DrawerMenu {
    private final Map<ReactionItem, ChangeListener<Boolean>> itemSelectedChangeListener = new HashMap<>();

    public ReactMenuBox(HashMap<Reaction, ReactionItem> reactionItems, List<Reaction> orderedReactions, ToggleReaction toggleReaction,
                        String defaultIconId, String hoverIconId, String activeIconId) {
        super(defaultIconId, hoverIconId, activeIconId);

        List<ReactionMenuItem> reactionMenuItems = new ArrayList<>();
        orderedReactions.forEach(reaction -> {
            if (reactionItems.containsKey(reaction)) {
                ReactionItem reactionItem = reactionItems.get(reaction);
                ReactionMenuItem reactionMenuItem = new ReactionMenuItem(reactionItem, toggleReaction);
                reactionMenuItems.add(reactionMenuItem);
                addItemListener(reactionItem, reactionMenuItem);
            }
        });
        addItems(reactionMenuItems);

        getStyleClass().add("react-menu-box");
    }

    public void dispose() {
        removeItemListeners();
    }

    public void reverseReactionsDisplayOrder() {
        ObservableList<Node> items = itemsHBox.getChildren();
        FXCollections.reverse(items);
    }

    private void addItemListener(ReactionItem reactionItem, ReactionMenuItem reactionMenuItem) {
        ChangeListener<Boolean> booleanChangeListener = (obs, oldValue, newValue) -> reactionMenuItem.setIsReactionSelected(newValue);;
        reactionItem.getSelected().addListener(booleanChangeListener);
        itemSelectedChangeListener.put(reactionItem, booleanChangeListener);
    }

    private void removeItemListeners() {
        itemSelectedChangeListener.forEach((item, changeListener) -> item.getSelected().removeListener(changeListener));
    }

    @Getter
    private static final class ReactionMenuItem extends BisqMenuItem {
        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private ReactionItem reactionItem;
        private ToggleReaction toggleReaction;

        private ReactionMenuItem(ReactionItem reactionItem, ToggleReaction toggleReaction) {
            this(reactionItem.getIconId());

            this.reactionItem = reactionItem;
            this.toggleReaction = toggleReaction;
            useIconOnly(24);
            getStyleClass().add("reaction-menu-item");
            setOnAction(e -> toggleReaction.execute(reactionItem));
            setIsReactionSelected(reactionItem.getSelected().get());

            ColorAdjust colorAdjust = new ColorAdjust();
            colorAdjust.setBrightness(-0.1);
            applyIconColorAdjustment(colorAdjust);
        }

        private ReactionMenuItem(String iconId) {
            super(iconId, iconId);
        }

        private void setIsReactionSelected(boolean isSelected) {
            pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, isSelected);
        }
    }
}
