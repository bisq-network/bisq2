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
import bisq.desktop.components.containers.BisqHSpacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqInputTextField;
import bisq.desktop.components.controls.BisqLabel;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.layout.Layout;
import bisq.desktop.primary.main.content.social.chat.components.UserProfileComboBox;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeDude;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.ListChangeListener;
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
    private final Button searchButton, notificationsButton, infoButton;
    private final ComboBox<UserProfileComboBox.ListItem> userProfileComboBox;
    private final VBox left;
    private final FilterBox filterBox;
    private final BisqInputTextField filterBoxRoot;
    private final Pane notificationsSettings;
    private final Pane channelInfo;
    private final ListChangeListener<ChatMessageListItem> messagesListener;

    public ChatView(ChatModel model, ChatController controller,
                    ComboBox<UserProfileComboBox.ListItem> userProfileComboBox,
                    Pane publicChannelSelection,
                    Pane privateChannelSelection,
                    Pane notificationsSettings,
                    Pane channelInfo) {
        super(new SplitPane(), model, controller);

        this.notificationsSettings = notificationsSettings;
        this.channelInfo = channelInfo;
        this.userProfileComboBox = userProfileComboBox;

        root.getStyleClass().add("hide-focus");

        userProfileComboBox.setPadding(new Insets(10, 10, 10, 10));

        left = Layout.vBoxWith(userProfileComboBox, publicChannelSelection, privateChannelSelection);
        left.setMinWidth(150);

        selectedChannelLabel = new BisqLabel();
        selectedChannelLabel.getStyleClass().add("headline-label");
        filterBox = new FilterBox(model.getFilteredChatMessages());
        filterBoxRoot = filterBox.getRoot();
        HBox.setHgrow(filterBoxRoot, Priority.ALWAYS);
        HBox.setMargin(filterBoxRoot, new Insets(0, 0, 0, 10));
        searchButton = AwesomeDude.createIconButton(AwesomeIcon.SEARCH);
        notificationsButton = AwesomeDude.createIconButton(AwesomeIcon.BELL);
        infoButton = AwesomeDude.createIconButton(AwesomeIcon.INFO_SIGN);
        HBox centerToolbar = Layout.hBoxWith(selectedChannelLabel, filterBoxRoot, searchButton, notificationsButton, infoButton);

        messagesListView = new ListView<>();
        messagesListView.setCellFactory(getCellFactory());
        messagesListView.setFocusTraversable(false);
        VBox.setVgrow(messagesListView, Priority.ALWAYS);

        inputField = new BisqInputTextField();
        inputField.setPromptText(Res.get("social.chat.input.prompt"));

        VBox messagesAndInput = Layout.vBoxWith(messagesListView, inputField);
        channelInfo.setMinWidth(200);
        channelInfo.setMaxWidth(600);
        HBox messagesListAndSideBar = Layout.hBoxWith(messagesAndInput, notificationsSettings, channelInfo);
        HBox.setHgrow(messagesAndInput, Priority.ALWAYS);
        VBox.setVgrow(messagesListAndSideBar, Priority.ALWAYS);
        VBox center = Layout.vBoxWith(centerToolbar, messagesListAndSideBar);
        center.setPadding(new Insets(10, 10, 10, 10));
        root.setDividerPosition(0, model.getDefaultLeftDividerPosition());
        root.getItems().addAll(left, center);

        messagesListener = c -> messagesListView.scrollTo(model.getFilteredChatMessages().size() - 1);
    }

    @Override
    public void onViewAttached() {
        userProfileComboBox.prefWidthProperty().bind(left.widthProperty());
        selectedChannelLabel.textProperty().bind(model.getSelectedChannelAsString());
        filterBoxRoot.visibleProperty().bind(model.getFilterBoxVisible());
        notificationsSettings.visibleProperty().bind(model.getNotificationsVisible());
        notificationsSettings.managedProperty().bind(model.getNotificationsVisible());
        channelInfo.visibleProperty().bind(model.getInfoVisible());
        channelInfo.managedProperty().bind(model.getInfoVisible());

        searchButton.setOnAction(e -> controller.onToggleFilterBox());
        notificationsButton.setOnAction(e -> controller.onToggleNotifications());
        infoButton.setOnAction(e -> controller.onToggleInfo());

        inputField.setOnAction(e -> {
            controller.onSendMessage(inputField.getText());
            inputField.clear();
        });

        model.getFilteredChatMessages().addListener(messagesListener);

        messagesListView.setItems(model.getFilteredChatMessages());
    }

    @Override
    protected void onViewDetached() {
        userProfileComboBox.prefWidthProperty().unbind();
        selectedChannelLabel.textProperty().unbind();
        filterBoxRoot.visibleProperty().unbind();
        notificationsSettings.visibleProperty().unbind();
        notificationsSettings.managedProperty().unbind();
        channelInfo.visibleProperty().unbind();
        channelInfo.managedProperty().unbind();

        searchButton.setOnAction(null);
        notificationsButton.setOnAction(null);
        infoButton.setOnAction(null);
        inputField.setOnAction(null);
        model.getFilteredChatMessages().removeListener(messagesListener);
    }
    private Callback<ListView<ChatMessageListItem>, ListCell<ChatMessageListItem>> getCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<ChatMessageListItem> call(ListView<ChatMessageListItem> list) {
                return new ListCell<>() {
                    BisqLabel user = new BisqLabel();
                    BisqButton openPrivateChannel = new BisqButton();
                    BisqLabel date = new BisqLabel();
                    BisqLabel text = new BisqLabel();

                    {
                        user.setStyle("-fx-text-fill: -bs-color-green-5; -fx-font-size: 1.0em");
                        user.setPadding(new Insets(5, 0, -8, 0));
//                        openPrivateChannel.setStyle("-fx-text-fill: -bs-color-green-5; -fx-font-size: 0.8em");
                        openPrivateChannel.setPadding(new Insets(5, 0, -8, 10));
                        openPrivateChannel.setText("create private Channel"); // TODO use Res.get()
                        date.getStyleClass().add("message-header");
                        date.setPadding(new Insets(1, 0, 0, 0));
                    }

                    @Override
                    public void updateItem(final ChatMessageListItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            text.setText(item.getMessage());
                            openPrivateChannel.setVisible(true); // TODO visibility if user is not myself
                            openPrivateChannel.setOnAction(e -> controller.openPrivateChannel(item.getSenderUserName(), item.getChatMessage().getSenderNetworkId()));
                            date.setText(item.getDate());
                            user.setText(item.getSenderUserName());
                            setGraphic(Layout.vBoxWith(Layout.hBoxWith(user, openPrivateChannel), Layout.hBoxWith(date, text)));
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }
}
