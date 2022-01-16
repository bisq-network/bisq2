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

package bisq.desktop.primary.main.content.social.user;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.desktop.components.controls.BisqTextField;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.social.chat.ChatPeer;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

@Slf4j
public class ChatUserView extends View<VBox, ChatUserModel, ChatUserController> {

    private final BisqComboBox<ChatPeer> comboBox;
    private final ListChangeListener<ChatPeer> listener;
    private final ChangeListener<ChatPeer> selectionListener;
    private final ChangeListener<ChatPeer> selectedChatUserListener;

    public ChatUserView(ChatUserModel model, ChatUserController controller) {
        super(new VBox(), model, controller);

        root.setPadding(Layout.PADDING);
        root.setSpacing(Layout.SPACING);

        comboBox = new BisqComboBox<>(Res.common.get("social.selectChatUser"));
        comboBox.setMinWidth(200);

        root.maxWidthProperty().bind(comboBox.widthProperty());
        root.maxHeightProperty().bind(comboBox.heightProperty());

        BisqButton createUserButton = new BisqButton(Res.common.get("social.createChatUser"));
        BisqTextField textField = new BisqTextField();
        textField.setPromptText(Res.common.get("social.createChatUser.prompt"));
        createUserButton.disableProperty().bind(textField.textProperty().isEmpty());
        
        createUserButton.setOnAction(e -> controller.onCreateNewChatUser(textField.getText()));
        root.getChildren().addAll(comboBox, createUserButton, textField);

        listener = c -> updateItems();
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(@Nullable ChatPeer chatPeer) {
                return chatPeer != null ? chatPeer.userName() : "";
            }

            @Override
            public ChatPeer fromString(String string) {
                return null;
            }
        });
        selectionListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                controller.onSelectChatUser(newValue);
            }
        };
        selectedChatUserListener = (observable, oldValue, newValue) -> {
            comboBox.getSelectionModel().select(newValue);
        };
    }

    private void updateItems() {
        comboBox.setItems(model.getChatPeers());
    }

    @Override
    public void onViewAttached() {
        model.getChatPeers().addListener(listener);
        comboBox.getSelectionModel().selectedItemProperty().addListener(selectionListener);
        model.getSelectedChatUser().addListener(selectedChatUserListener);
        updateItems();
        //comboBox.getSelectionModel().select(model.getSelectedChatUser().get());
    }

    @Override
    protected void onViewDetached() {
        model.getChatPeers().removeListener(listener);
        comboBox.getSelectionModel().selectedItemProperty().removeListener(selectionListener);
        model.getSelectedChatUser().removeListener(selectedChatUserListener);
    }
}
