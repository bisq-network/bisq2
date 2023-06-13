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

import bisq.desktop.common.utils.Layout;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.primary.main.content.chat.ChatView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyChatView extends ChatView {
    private final BisqEasyChatModel bisqEasyChatModel;
    private final Switch offersOnlySwitch;
    private final Region bisqEasyPrivateTradeChatChannelSelection;
    private final VBox tradeStateViewRoot;
    private final BisqEasyChatController bisqEasyChatController;
    private Subscription isBisqEasyPrivateTradeChatChannelPin;
    private final Button createOfferButton;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Region bisqEasyPublicChatChannelSelection,
                            Region bisqEasyPrivateTradeChatChannelSelection,
                            Region twoPartyPrivateChatChannelSelection,
                            VBox chatMessagesComponent,
                            Pane channelSidebar,
                            VBox tradeStateViewRoot) {
        super(model,
                controller,
                bisqEasyPublicChatChannelSelection,
                twoPartyPrivateChatChannelSelection,
                chatMessagesComponent,
                channelSidebar);

        bisqEasyChatController = controller;
        this.bisqEasyPrivateTradeChatChannelSelection = bisqEasyPrivateTradeChatChannelSelection;
        this.tradeStateViewRoot = tradeStateViewRoot;

        left.getChildren().add(1, Layout.separator());
        left.getChildren().add(2, bisqEasyPrivateTradeChatChannelSelection);

        bisqEasyChatModel = model;

        offersOnlySwitch = new Switch();
        offersOnlySwitch.setText(Res.get("bisqEasy.topPane.filter.offersOnly"));

        centerToolbar.getChildren().add(2, offersOnlySwitch);

        createOfferButton = new Button(Res.get("offer.createOffer"));
        createOfferButton.setMaxWidth(Double.MAX_VALUE);
        createOfferButton.setMinHeight(37);
        createOfferButton.setDefaultButton(true);
        VBox.setMargin(createOfferButton, new Insets(-2, 25, 17, 25));
        left.getChildren().add(createOfferButton);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        topSeparator.visibleProperty().bind(bisqEasyChatModel.getTopSeparatorVisible());
        topSeparator.managedProperty().bind(bisqEasyChatModel.getTopSeparatorVisible());
        createOfferButton.visibleProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        createOfferButton.managedProperty().bind(bisqEasyChatModel.getCreateOfferButtonVisible());
        offersOnlySwitch.visibleProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        offersOnlySwitch.managedProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        offersOnlySwitch.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());

        isBisqEasyPrivateTradeChatChannelPin = EasyBind.subscribe(bisqEasyChatModel.getIsBisqEasyPrivateTradeChatChannel(),
                isBisqEasyPrivateTradeChatChannel -> {
                    if (isBisqEasyPrivateTradeChatChannel) {
                        if (!chatMessagesComponent.getChildren().contains(tradeStateViewRoot)) {
                            chatMessagesComponent.getChildren().add(0, tradeStateViewRoot);
                            VBox.setMargin(tradeStateViewRoot, new Insets(2, 25, 25, 25));
                        }
                    } else {
                        chatMessagesComponent.getChildren().remove(tradeStateViewRoot);
                    }
                });

        createOfferButton.setOnAction(e -> bisqEasyChatController.onCreateOffer());
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        topSeparator.visibleProperty().unbind();
        topSeparator.managedProperty().unbind();

        createOfferButton.visibleProperty().unbind();
        createOfferButton.managedProperty().unbind();
        offersOnlySwitch.visibleProperty().unbind();
        offersOnlySwitch.managedProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().unbind();
        offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
        isBisqEasyPrivateTradeChatChannelPin.unsubscribe();

        createOfferButton.setOnAction(null);
    }
}
