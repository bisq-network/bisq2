package bisq.desktop.main.content.chat.message_container.components;

import bisq.common.util.StringUtils;
import bisq.desktop.components.controls.BisqPopup;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.user.profile.UserProfile;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
        listView.setPrefWidth(600);
        listView.setCellFactory(getCellFactory());

        setAlignment(Alignment.LEFT);
        setContentNode(listView);

        filterChangeListener = (observableValue, oldValue, newValue) -> {
            if (newValue != null) {
                filteredList.setPredicate(item -> item.matchUserName(newValue));
                listView.setPrefHeight(Math.min(600, 20 + filteredList.size() * ListItem.CELL_HEIGHT));
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
