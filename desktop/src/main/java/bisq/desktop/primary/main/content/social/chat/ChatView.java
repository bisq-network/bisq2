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

package bisq.desktop.primary.main.content.social.chat;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatView extends View<SplitPane, ChatModel, ChatController> {

    private final ListView<ChatMessageListItem> messagesListView;
    private final BisqInputTextField inputField;
    private final BisqLabel selectedChannelLabel;
    private final Button searchButton, notificationsButton, settingButton;
    private final VBox sideBar;
    private final ComboBox<UserProfileComboBox.ListItem> userProfileComboBox;
    private final VBox left;
    private final FilterBox filterBox;
    private final BisqInputTextField filterBoxRoot;

    public ChatView(ChatModel model, ChatController controller,
                    ComboBox<UserProfileComboBox.ListItem> userProfileComboBox,
                    Pane publicChannelSelection,
                    Pane privateChannelSelection) {
        super(new SplitPane(), model, controller);
        this.userProfileComboBox = userProfileComboBox;
        root.getStyleClass().add("hide-focus");

        userProfileComboBox.setPadding(new Insets(10, 10, 10, 10));

        left = Layout.vBoxWith(userProfileComboBox, publicChannelSelection, privateChannelSelection);
        left.setMinWidth(150);

        selectedChannelLabel = new BisqLabel();
        selectedChannelLabel.getStyleClass().add("headline-label");
        filterBox = new FilterBox(model.getFilteredChatMessages());
        filterBoxRoot = filterBox.getRoot();
        HBox.setHgrow(filterBoxRoot,Priority.ALWAYS);
        HBox.setMargin(filterBoxRoot,new Insets(0,0,0,10));
        searchButton = AwesomeDude.createIconButton(AwesomeIcon.SEARCH);
        notificationsButton = AwesomeDude.createIconButton(AwesomeIcon.BELL);
        settingButton = AwesomeDude.createIconButton(AwesomeIcon.GEAR);
        HBox centerToolbar = Layout.hBoxWith(selectedChannelLabel, filterBoxRoot, searchButton, notificationsButton, settingButton);

        messagesListView = new ListView<>();
        messagesListView.setFocusTraversable(false);
        VBox.setVgrow(messagesListView, Priority.ALWAYS);
        inputField = new BisqInputTextField();
        inputField.setPromptText(Res.get("social.chat.input.prompt"));
        sideBar = new VBox(); //todo for settings/info like in Element
        sideBar.setMinWidth(200);

        VBox messagesAndInput = Layout.vBoxWith(messagesListView, inputField);
        HBox messagesListAndSideBar = Layout.hBoxWith(messagesAndInput, sideBar);
        HBox.setHgrow(messagesAndInput, Priority.ALWAYS);
        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);
        VBox center = Layout.vBoxWith(centerToolbar, messagesListAndSideBar);
        center.setPadding(new Insets(10, 10, 10, 10));
        this.root.setDividerPosition(0, model.getDefaultLeftDividerPosition());
        this.root.getItems().addAll(left, center);

        messagesListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<ChatMessageListItem> call(ListView<ChatMessageListItem> list) {
                return new ListCell<>() {
                    BisqLabel user = new BisqLabel();
                    BisqLabel date = new BisqLabel();
                    BisqLabel text = new BisqLabel();

                    {
                        user.setStyle("-fx-text-fill: -bs-color-green-5; -fx-font-size: 1.0em");
                        user.setPadding(new Insets(5, 0, -8, 0));
                        date.getStyleClass().add("message-header");
                        date.setPadding(new Insets(1, 0, 0, 0));
                    }

                    @Override
                    public void updateItem(final ChatMessageListItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            text.setText(item.getMessage());
                            date.setText(item.getDate());
                            user.setText(item.getSenderUserName());
                            setGraphic(Layout.vBoxWith(user, Layout.hBoxWith(date, text)));
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        });
    }

    @Override
    public void onViewAttached() {
        userProfileComboBox.prefWidthProperty().bind(left.widthProperty());
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
        filterBoxRoot.visibleProperty().bind(model.getFilterBoxVisible());
        sideBar.visibleProperty().bind(model.getSideBarVisible());
        sideBar.managedProperty().bind(model.getSideBarVisible());

        searchButton.setOnAction(e -> controller.onToggleFilterBox());
        notificationsButton.setOnAction(e -> controller.onShowNotifications());
        settingButton.setOnAction(e -> controller.onToggleSettings());

        inputField.setOnAction(e -> {
            controller.onSendMessage(inputField.getText());
            inputField.clear();
        });
        messagesListView.setItems(model.getFilteredChatMessages());
    }

    @Override
    protected void onViewDetached() {
        userProfileComboBox.prefWidthProperty().unbind();
        selectedChannelLabel.textProperty().unbind();
        filterBoxRoot.visibleProperty().unbind();
        sideBar.visibleProperty().unbind();
        sideBar.managedProperty().unbind();

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        settingButton.setOnAction(null);
        inputField.setOnAction(null);
    }
}
