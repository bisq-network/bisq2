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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.primary.main.content.chat.ChatView;
import bisq.desktop.primary.main.content.trade.bisqEasy.chat.guide.TradeGuideView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatController bisqEasyChatController;
    private final Switch toggleOffersButton;
    private final BisqEasyChatModel bisqEasyChatModel;
    private final Button actionButton;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Pane marketChannelSelection,
                            Pane privateChannelSelection,
                            Pane chatMessagesComponent,
                            Pane channelInfo) {
        super(model,
                controller,
                marketChannelSelection,
                privateChannelSelection,
                chatMessagesComponent,
                channelInfo);

        bisqEasyChatController = controller;
        bisqEasyChatModel = model;

        toggleOffersButton = new Switch();
        toggleOffersButton.setText(Res.get("bisqEasy.filter.offersOnly"));

        centerToolbar.getChildren().add(2, toggleOffersButton);

        actionButton = new Button();
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setMinHeight(37);
        actionButton.setDefaultButton(true);
        VBox.setMargin(actionButton, new Insets(-2, 25, 17, 25));
        left.getChildren().add(actionButton);

        model.getView().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                // chatMessagesComponent is VBox
                VBox.setMargin(childRoot, new Insets(25, 25, 25, 25));
                chatMessagesComponent.getChildren().add(0, childRoot);
                UIThread.runOnNextRenderFrame(() -> Transitions.transitContentViews(oldValue, newValue));
            } else if (oldValue instanceof TradeGuideView) {
                chatMessagesComponent.getChildren().remove(0);
            }
        });
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        actionButton.visibleProperty().bind(bisqEasyChatModel.getActionButtonVisible());
        actionButton.managedProperty().bind(bisqEasyChatModel.getActionButtonVisible());
        actionButton.textProperty().bind(bisqEasyChatModel.getActionButtonText());
        actionButton.setOnAction(e -> bisqEasyChatController.onActionButtonClicked());

        toggleOffersButton.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        actionButton.visibleProperty().unbind();
        actionButton.managedProperty().unbind();
        actionButton.textProperty().unbind();
        actionButton.setOnAction(null);

        toggleOffersButton.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
    }
}
