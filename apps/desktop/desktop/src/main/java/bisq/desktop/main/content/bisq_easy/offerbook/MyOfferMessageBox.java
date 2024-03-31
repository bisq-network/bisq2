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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.DropdownMenuItem;
import bisq.desktop.main.content.components.chat_messages.list_view.ChatMessageListItem;
import bisq.desktop.main.content.components.chat_messages.list_view.ChatMessagesListController;
import bisq.desktop.main.content.components.chat_messages.list_view.ChatMessagesListModel;
import bisq.desktop.main.content.components.chat_messages.list_view.message_box.BubbleMessageBox;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class MyOfferMessageBox extends BubbleMessageBox {
    private DropdownMenuItem removeOffer;
    private Label copyIcon;

    public MyOfferMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                             ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                             ChatMessagesListController controller, ChatMessagesListModel model) {
        super(item, list, controller, model);

        // Message
        message.getStyleClass().add("chat-my-offer-message");

        // User profile icon
        userProfileIcon.setSize(OFFER_MESSAGE_USER_ICON_SIZE);

        // Dropdown menu
        DropdownMenu dropdownMenu = createAndGetDropdownMenu();

        // Wrappers
        HBox hBox = new HBox(15, message, userProfileIconVbox);
        hBox.setAlignment(Pos.CENTER);
        VBox vBox = new VBox(5, hBox, dropdownMenu);
        vBox.setAlignment(Pos.CENTER_RIGHT);

        // Message background
        messageBgHBox.getStyleClass().add("chat-my-offer-message-bg");
        messageBgHBox.getChildren().setAll(vBox);
        messageBgHBox.setMaxWidth(Control.USE_PREF_SIZE);

        // Reactions
        reactionsHBox.getChildren().setAll(Spacer.fillHBox(), supportedLanguages, copyIcon);

        contentVBox.setAlignment(Pos.CENTER_RIGHT);
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, reactionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, dateTime, userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(userNameAndDateHBox, new Insets(0, 10, 5, 0));
    }

    @Override
    protected void setUpReactions() {
        copyIcon = getIconWithToolTip(AwesomeIcon.COPY, Res.get("action.copyToClipboard"));
        reactionsHBox.setVisible(false);
    }

    @Override
    protected void addReactionsHandlers() {
        copyIcon.setOnMouseClicked(e -> onCopyMessage(item.getChatMessage()));
    }

    private DropdownMenu createAndGetDropdownMenu() {
        removeOffer = new DropdownMenuItem("delete-bin-red-lit-10", "delete-bin-red",
                Res.get("offer.deleteOffer"));
        removeOffer.setOnAction(e -> controller.onDeleteMessage(item.getChatMessage()));
        removeOffer.getStyleClass().add("red-menu-item");

        DropdownMenu dropdownMenu = new DropdownMenu("ellipsis-h-grey", "ellipsis-h-white", true);
        dropdownMenu.setVisible(item.isPublicChannel());
        dropdownMenu.setManaged(item.isPublicChannel());
        dropdownMenu.setTooltip(Res.get("chat.dropdownMenu.tooltip"));
        dropdownMenu.addMenuItems(removeOffer);
        return dropdownMenu;
    }

    @Override
    public void cleanup() {
        removeOffer.setOnAction(null);
    }
}
