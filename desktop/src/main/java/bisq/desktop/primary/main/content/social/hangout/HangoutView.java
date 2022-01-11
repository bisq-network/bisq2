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

package bisq.desktop.primary.main.content.social.hangout;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTextField;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class HangoutView extends View<HBox, HangoutModel, HangoutController> {
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final BisqButton sendButton;
    private final BisqTextField inputTextField;
    private final VBox userList;
    private final BisqTextArea textArea;
    private final ListChangeListener<String> peerListChangeListener;

    public HangoutView(HangoutModel model, HangoutController controller) {
        super(new HBox(), model, controller);

        root.setSpacing(Layout.SPACING);
        root.setPadding(new Insets(20, 20, 20, 0));
        userList = new VBox();
        userList.setMinWidth(150);
        userList.setSpacing(10);
        VBox chatSpace = new VBox();
        chatSpace.setSpacing(Layout.SPACING);
        textArea = new BisqTextArea();
        HBox sendBox = new HBox();
        sendBox.setSpacing(Layout.SPACING);
        inputTextField = new BisqTextField();
        sendButton = new BisqButton(Res.common.get("send"));
        sendBox.getChildren().addAll(inputTextField, sendButton);
        chatSpace.getChildren().addAll(textArea, sendBox);
        root.getChildren().addAll(userList, chatSpace);

        peerListChangeListener = c -> updatePeerList();
    }

    private void updatePeerList() {
        List<PeerButton> collect = model.getChatPeers().stream()
                .map(chatPeer -> new PeerButton(chatPeer, toggleGroup, () -> controller.selectChatPeer(chatPeer)))
                .collect(Collectors.toList());
        userList.getChildren().setAll(collect);
    }

    @Override
    public void onViewAttached() {
        model.chatPeers.addListener(peerListChangeListener);
        textArea.textProperty().bind(model.chatText);
        inputTextField.setPromptText(Res.common.get("inputFieldPrompt", model.getSelectedChatPeer().orElse("")));
        sendButton.setOnAction(e -> controller.send(inputTextField.getText()));
        updatePeerList();
    }

    @Override
    protected void onViewDetached() {
        model.chatPeers.removeListener(peerListChangeListener);
        sendButton.setOnAction(null);
    }

    private static class PeerButton extends JFXButton implements Toggle {
        private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

        private PeerButton(String title, ToggleGroup toggleGroup, Runnable handler) {
            super(title.toUpperCase());

            setPrefHeight(40);
            setPrefWidth(150);
            setAlignment(Pos.CENTER_LEFT);

            this.setToggleGroup(toggleGroup);
            toggleGroup.getToggles().add(this);

            this.selectedProperty().addListener((ov, oldValue, newValue) -> setMouseTransparent(newValue));
            this.setOnAction(e -> handler.run());
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        // Toggle implementation
        ///////////////////////////////////////////////////////////////////////////////////////////

        @Override
        public ToggleGroup getToggleGroup() {
            return toggleGroupProperty.get();
        }

        @Override
        public void setToggleGroup(ToggleGroup toggleGroup) {
            toggleGroupProperty.set(toggleGroup);
        }

        @Override
        public ObjectProperty<ToggleGroup> toggleGroupProperty() {
            return toggleGroupProperty;
        }

        @Override
        public boolean isSelected() {
            return selectedProperty.get();
        }

        @Override
        public BooleanProperty selectedProperty() {
            return selectedProperty;
        }

        @Override
        public void setSelected(boolean selected) {
            selectedProperty.set(selected);
            if (selected) {
                getStyleClass().add("action-button");
            } else {
                getStyleClass().remove("action-button");
            }
        }
    }
}
