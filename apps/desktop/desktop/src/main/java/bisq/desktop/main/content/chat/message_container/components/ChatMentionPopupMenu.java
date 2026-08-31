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
import bisq.desktop.main.content.chat.message_container.components.ChatMentionParser.MentionMatch;
import bisq.user.profile.UserProfile;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.IndexedCell;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.function.BiConsumer;

@Slf4j
public class ChatMentionPopupMenu extends BisqPopup {
    private final BisqTextArea inputField;
    private final BiConsumer<UserProfile, MentionMatch> userProfileSelectedHandler;
    private final ObjectProperty<MentionMatch> mentionMatch = new SimpleObjectProperty<>();
    // Escape dismisses the popup for the current mention token; leaving the token re-arms it.
    private boolean dismissed;
    // Last non-torn parse result; JavaFX updates the text before clamping the caret, and the
    // torn intermediate state must not produce a transient null that wipes the dismissal.
    private MentionMatch lastStableMatch;
    @Getter
    private final ObservableList<ListItem> observableList = FXCollections.observableArrayList();
    private final FilteredList<ListItem> filteredList = new FilteredList<>(observableList);
    private final SortedList<ListItem> sortedList = new SortedList<>(filteredList);
    private final ListView<ListItem> listView = new ListView<>(sortedList);
    private final ChangeListener<MentionMatch> mentionMatchChangeListener;

    public ChatMentionPopupMenu(BisqTextArea inputField, BiConsumer<UserProfile, MentionMatch> userProfileSelectedHandler) {
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

        // While a popup is showing, JavaFX redirects the owner window's key events to the popup
        // window first, so the navigation keys must be handled here - a filter on the input
        // field never sees them. Unhandled keys (typed characters, Enter without a candidate)
        // continue to the input field through the same redirection.
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);

        mentionMatchChangeListener = (observableValue, oldValue, newValue) -> {
            if (newValue != null) {
                // The caret can jump from one mention token straight into another; the token
                // identity, not a null transition, decides when the dismissal is re-armed.
                boolean isNewToken = oldValue == null || oldValue.indicatorIndex() != newValue.indicatorIndex();
                if (isNewToken) {
                    dismissed = false;
                }
                String query = newValue.query();
                filteredList.setPredicate(item -> item.matchUserName(query));
                sortedList.setComparator(sortByPrefixMatchingQuery(query));
                listView.setPrefHeight(Math.min(ListItem.CELL_HEIGHT * 10, filteredList.size() * ListItem.CELL_HEIGHT));
                listView.getSelectionModel().clearSelection();
                if (!dismissed && !isShowing()) {
                    show(inputField);
                }
            } else {
                dismissed = false;
                hide();
            }
        };
    }

    public void init() {
        mentionMatch.addListener(mentionMatchChangeListener);
        mentionMatch.bind(Bindings.createObjectBinding(
                () -> {
                    String text = inputField.getText();
                    int caretPosition = inputField.getCaretPosition();
                    if (text != null && caretPosition > text.length()) {
                        // Torn text/caret intermediate state: keep the previous result until
                        // the caret is clamped.
                        return lastStableMatch;
                    }
                    lastStableMatch = ChatMentionParser.findMentionAtCaret(text, caretPosition).orElse(null);
                    return lastStableMatch;
                },
                inputField.textProperty(),
                inputField.caretPositionProperty()));
    }

    public void cleanup() {
        hide();
        mentionMatch.removeListener(mentionMatchChangeListener);
        mentionMatch.unbind();
        mentionMatch.set(null);
        listView.getSelectionModel().clearSelection();
        dismissed = false;
        lastStableMatch = null;
    }

    public boolean moveSelectionDown() {
        if (sortedList.isEmpty()) {
            return false;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        int next = Math.min(index + 1, sortedList.size() - 1);
        listView.getSelectionModel().select(next);
        scrollToIfNotVisible(next);
        return true;
    }

    public boolean moveSelectionUp() {
        if (sortedList.isEmpty()) {
            return false;
        }
        int index = listView.getSelectionModel().getSelectedIndex();
        int previous = Math.max(index - 1, 0);
        listView.getSelectionModel().select(previous);
        scrollToIfNotVisible(previous);
        return true;
    }

    private void scrollToIfNotVisible(int index) {
        // scrollTo anchors the row to the top, so calling it unconditionally makes the whole
        // list jump on every arrow press; only scroll when the selection leaves the viewport.
        if (listView.lookup(".virtual-flow") instanceof VirtualFlow<?> virtualFlow) {
            IndexedCell<?> firstVisibleCell = virtualFlow.getFirstVisibleCell();
            IndexedCell<?> lastVisibleCell = virtualFlow.getLastVisibleCell();
            if (firstVisibleCell != null && lastVisibleCell != null
                    && index >= firstVisibleCell.getIndex() && index <= lastVisibleCell.getIndex()) {
                return;
            }
        }
        listView.scrollTo(index);
    }

    public boolean completeSelection() {
        MentionMatch match = mentionMatch.get();
        if (match == null || sortedList.isEmpty()) {
            return false;
        }
        ListItem item = listView.getSelectionModel().getSelectedItem();
        if (item == null) {
            item = sortedList.get(0);
        }
        userProfileSelectedHandler.accept(item.getUserProfile(), match);
        // Completion dismisses like Escape: when the caret lands inside the completed token
        // (e.g. before punctuation) the popup must not immediately re-open.
        dismiss();
        return true;
    }

    public void dismiss() {
        dismissed = true;
        hide();
    }

    private void handleKeyPressed(KeyEvent keyEvent) {
        boolean noModifiers = !keyEvent.isShiftDown() && !keyEvent.isControlDown()
                && !keyEvent.isAltDown() && !keyEvent.isMetaDown();
        switch (keyEvent.getCode()) {
            case DOWN -> {
                // With an empty result list the arrows keep their text-area meaning.
                if (noModifiers && moveSelectionDown()) {
                    keyEvent.consume();
                }
            }
            case UP -> {
                if (noModifiers && moveSelectionUp()) {
                    keyEvent.consume();
                }
            }
            case ENTER, TAB -> {
                // Without a completable candidate the event continues to the input field, so
                // Enter still sends the message.
                if (noModifiers && completeSelection()) {
                    keyEvent.consume();
                }
            }
            case ESCAPE -> {
                dismiss();
                keyEvent.consume();
            }
            default -> {
            }
        }
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
                                MentionMatch match = mentionMatch.get();
                                if (match != null) {
                                    userProfileSelectedHandler.accept(item.getUserProfile(), match);
                                }
                                dismiss();
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
