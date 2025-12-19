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

package bisq.desktop.main.content.chat.message_container.components;

import bisq.common.util.StringUtils;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.i18n.Res;
import bisq.user.profile.UserProfile;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.function.Consumer;

@Slf4j
public class ChatMentionPopupMenu extends BisqPopup {
    private final BisqTextArea inputField;
    private final Consumer<UserProfile> userProfileSelectedHandler;
    private final StringProperty filter = new SimpleStringProperty();
    @Getter
    private final ObservableList<ListItem> observableList = FXCollections.observableArrayList();
    private final FilteredList<ListItem> filteredList = new FilteredList<>(observableList);
    private final SortedList<ListItem> sortedList = new SortedList<>(filteredList);
    private final ListView<ListItem> listView = new ListView<>(sortedList);
    private final ChangeListener<String> filterChangeListener;

    public ChatMentionPopupMenu(BisqTextArea inputField, Consumer<UserProfile> userProfileSelectedHandler) {
        super();
        this.inputField = inputField;
        this.userProfileSelectedHandler = userProfileSelectedHandler;

        sortedList.setComparator(ListItem::compareTo);
        listView.getStyleClass().add("chat-mention-list-view");
        listView.setPrefWidth(450);
        listView.setCellFactory(getCellFactory());
        Label placeholderLabel = new Label(Res.get("chat.atMentionPopup.placeholder"));
        placeholderLabel.setGraphic(ImageUtil.getImageViewById("search-white"));
        placeholderLabel.setGraphicTextGap(8);
        placeholderLabel.getStyleClass().add("chat-mention-placeholder-label");
        listView.setPlaceholder(placeholderLabel);

        setAlignment(Alignment.LEFT);
        setAnchorLocation(AnchorLocation.WINDOW_BOTTOM_LEFT);
        setContentNode(listView);
        getStyleClass().add("chat-mention-popup");

        filterChangeListener = (observableValue, oldValue, newValue) -> {
            if (newValue != null) {
                filteredList.setPredicate(item -> item.matchUserName(newValue));
                sortedList.setComparator(sortByPrefixMatchingQuery(newValue));
                listView.setPrefHeight(Math.min(ListItem.CELL_HEIGHT * 10, filteredList.size() * ListItem.CELL_HEIGHT));
                if (oldValue == null) {
                    show(inputField);
                }
            } else {
                hide();
            }
        };
    }

    public void init() {
        filter.addListener(filterChangeListener);
        filter.bind(Bindings.createStringBinding(
                () -> StringUtils.deriveWordStartingWith(inputField.getText(), '@'),
                inputField.textProperty()));
    }

    public void cleanup() {
        filter.removeListener(filterChangeListener);
        filter.unbind();
    }

    @Override
    public void show(Node owner) {
        Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
        super.show(owner, bounds.getMinX(), bounds.getMinY() - 5);
    }

    private static Comparator<ListItem> sortByPrefixMatchingQuery(String query) {
        return (o1, o2) -> {
            String q = query.toLowerCase();
            String name1 = o1.getUserName().toLowerCase();
            String name2 = o2.getUserName().toLowerCase();

            boolean starts1 = name1.startsWith(q);
            boolean starts2 = name2.startsWith(q);

            if (starts1 && !starts2) return -1;
            if (!starts1 && starts2) return 1;

            return name1.compareTo(name2);
        };
    }

    private Callback<ListView<ListItem>, ListCell<ListItem>> getCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<ListItem> call(ListView<ListItem> listItemListView) {
                return new ListCell<>() {
                    private final Button button = new Button();

                    {
                        button.getStyleClass().add("chat-mention-list-button");
                        button.setMaxWidth(Double.MAX_VALUE);
                    }

                    @Override
                    protected void updateItem(ListItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            button.setText(item.getUserName());
                            button.setOnAction(e -> {
                                userProfileSelectedHandler.accept(item.getUserProfile());
                                hide();
                            });

                            setGraphic(button);
                        } else {
                            button.setOnAction(null);
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem implements Comparable<ListItem> {
        public static final double CELL_HEIGHT = 30;

        @EqualsAndHashCode.Include
        private final UserProfile userProfile;

        private final String userName;

        public ListItem(UserProfile userProfile) {
            this.userProfile = userProfile;

            userName = userProfile.getUserName();
        }

        public boolean matchUserName(String searchString) {
            return StringUtils.containsIgnoreCase(userName, searchString);
        }

        @Override
        public int compareTo(ListItem o) {
            return userName.toLowerCase().compareTo(o.getUserName().toLowerCase());
        }
    }
}
