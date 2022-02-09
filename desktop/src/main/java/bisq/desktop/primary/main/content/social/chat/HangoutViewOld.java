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
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqTextArea;
import bisq.desktop.components.controls.BisqTextField;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import bisq.social.chat.Channel;
import com.jfoenix.controls.JFXButton;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HangoutViewOld extends View<HBox, ChatModel, ChatController> {
    private final ToggleGroup toggleGroup = new ToggleGroup();
    private final BisqButton sendButton;
    private final BisqTextField inputTextField;
    private final VBox channelList;
    private final BisqTextArea textArea;
    private final ListChangeListener<Channel> channelsChangeListener;
    private final ChangeListener<Channel> selectedChannelListener;
    private final Map<String, ChannelButton> channelButtonByChannelId = new HashMap<>();
    private final ChangeListener<String> textAreaListener;

    public HangoutViewOld(ChatModel model, ChatController controller, Pane userProfile) {
        super(new HBox(), model, controller);

        root.setSpacing(Layout.SPACING);
        root.setPadding(new Insets(20, 20, 20, 0));
        channelList = new VBox();
        channelList.setMinWidth(200);
        channelList.setSpacing(10);
        VBox chatSpace = new VBox();
        chatSpace.setSpacing(Layout.SPACING);
        textArea = new BisqTextArea();
        textArea.setMinWidth(650);

        HBox sendBox = new HBox();
        sendBox.setSpacing(Layout.SPACING);
        inputTextField = new BisqTextField();
        sendButton = new BisqButton(Res.get("send"));
        sendBox.getChildren().addAll(inputTextField, sendButton);
        chatSpace.getChildren().addAll(textArea, sendBox);

        //todo user UI needs more work...
        root.getChildren().addAll(userProfile,channelList, chatSpace);

        channelsChangeListener = c -> updateChannels();
        selectedChannelListener = (observable, oldValue, newValue) -> {
            sendButton.setDisable(newValue == null);
            if (newValue != null) {
                toggleGroup.selectToggle(channelButtonByChannelId.get(newValue.getId()));
            }
        };
        textAreaListener = (observable, oldValue, newValue) -> {
            // textArea.setText() triggers setScrollTop(0), but clear/appendText not.
            // https://stackoverflow.com/questions/17799160/javafx-textarea-and-autoscroll
            textArea.clear();
            textArea.appendText(newValue);
            textArea.setScrollTop(Double.MAX_VALUE);
        };
    }

    private void updateChannels() {
        channelList.getChildren().setAll(model.getChannels().stream()
                .map(channel -> {
                    ChannelButton channelButton = new ChannelButton(channel.getChannelName(), toggleGroup, () -> controller.onSelectChannel(channel));
                    channelButtonByChannelId.put(channel.getId(), channelButton);
                    return channelButton;
                })
                .collect(Collectors.toList()));
    }

    @Override
    public void onViewAttached() {
        model.getChannels().addListener(channelsChangeListener);
        model.getSelectedChannel().addListener(selectedChannelListener);
        model.getSelectedChatMessages().addListener(textAreaListener);
        sendButton.setOnAction(e -> {
            controller.onSendMessage(inputTextField.getText());
            inputTextField.clear();
        });

        inputTextField.setPromptText(Res.get("sendMessagePrompt"));
        updateChannels();
        sendButton.setDisable(model.getSelectedChannel().get() == null);
    }

    @Override
    protected void onViewDetached() {
        model.getChannels().removeListener(channelsChangeListener);
        model.getSelectedChannel().removeListener(selectedChannelListener);
        textArea.textProperty().removeListener(textAreaListener);
        sendButton.setOnAction(null);
    }

    private static class ChannelButton extends JFXButton implements Toggle {
        private final ObjectProperty<ToggleGroup> toggleGroupProperty = new SimpleObjectProperty<>();
        private final BooleanProperty selectedProperty = new SimpleBooleanProperty();

        private ChannelButton(String channelId, ToggleGroup toggleGroup, Runnable handler) {
            super(channelId);

            setPrefHeight(40);
            setPrefWidth(200);
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
