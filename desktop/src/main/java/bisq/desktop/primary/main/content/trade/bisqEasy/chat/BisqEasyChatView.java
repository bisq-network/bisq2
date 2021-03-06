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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatController bisqEasyChatController;
    private final Switch toggleOffersButton;
    private final BisqEasyChatModel bisqEasyChatModel;

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
        toggleOffersButton.setText(Res.get("satoshisquareapp.chat.filter.offersOnly"));

        centerToolbar.getChildren().add(2, toggleOffersButton);

        model.getView().addListener((observable, oldValue, newValue) -> {
            log.error("newValue {}", newValue);
            if (newValue != null) {
                Region childRoot = newValue.getRoot();
                // chatMessagesComponent is VBox
                VBox.setMargin(childRoot, new Insets(25, 25, 25, 25));
                chatMessagesComponent.getChildren().add(0, childRoot);
               UIThread.runOnNextRenderFrame(()-> Transitions.transitContentViews(oldValue, newValue));
            } else if (oldValue instanceof TradeGuideView) {
                chatMessagesComponent.getChildren().remove(0);
            }
        });
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();
        toggleOffersButton.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        toggleOffersButton.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
    }
}
