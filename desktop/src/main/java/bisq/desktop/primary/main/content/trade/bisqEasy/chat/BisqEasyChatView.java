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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat;

import bisq.desktop.components.controls.BisqToggleButton;
import bisq.desktop.components.table.FilterBox;
import bisq.desktop.primary.main.content.ChatView;
import javafx.scene.layout.Pane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatController bisqEasyChatController;
    private final BisqToggleButton toggleOffersButton;
    private final BisqEasyChatModel bisqEasyChatModel;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Pane marketChannelSelection,
                            Pane privateChannelSelection,
                            Pane chatMessagesComponent,
                            Pane notificationsSettings,
                            Pane channelInfo,
                            Pane helpPane,
                            FilterBox filterBox) {
        super(model,
                controller,
                marketChannelSelection,
                privateChannelSelection,
                chatMessagesComponent,
                notificationsSettings,
                channelInfo,
                helpPane,
                filterBox);

        bisqEasyChatController = controller;
        bisqEasyChatModel = model;

        toggleOffersButton = new BisqToggleButton();
        // toggleOffersButton.getStyleClass().add("bisq-text-4");
        toggleOffersButton.setText("Offers only");

        centerToolbar.getChildren().add(3, toggleOffersButton);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        toggleOffersButton.setSelected(bisqEasyChatModel.getOfferOnly().get());
        toggleOffersButton.setOnAction(e -> bisqEasyChatController.onToggleOffersOnly(toggleOffersButton.isSelected()));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        toggleOffersButton.selectedProperty().unbind();
        toggleOffersButton.setOnAction(null);
    }
}
