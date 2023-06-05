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
    private final Pane tradeAssistant;
    private Subscription isBisqEasyPrivateTradeChatChannelPin;

    public BisqEasyChatView(BisqEasyChatModel model,
                            BisqEasyChatController controller,
                            Region bisqEasyPublicChatChannelSelection,
                            Region bisqEasyPrivateTradeChatChannelSelection,
                            Region twoPartyPrivateChatChannelSelection,
                            VBox chatMessagesComponent,
                            Pane channelSidebar,
                            Pane tradeAssistant) {
        super(model,
                controller,
                bisqEasyPublicChatChannelSelection,
                twoPartyPrivateChatChannelSelection,
                chatMessagesComponent,
                channelSidebar);

        this.bisqEasyPrivateTradeChatChannelSelection = bisqEasyPrivateTradeChatChannelSelection;
        this.tradeAssistant = tradeAssistant;

        left.getChildren().add(1, Layout.separator());
        left.getChildren().add(2, bisqEasyPrivateTradeChatChannelSelection);

        bisqEasyChatModel = model;

        offersOnlySwitch = new Switch();
        offersOnlySwitch.setText(Res.get("bisqEasy.filter.offersOnly"));

        centerToolbar.getChildren().add(2, offersOnlySwitch);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        offersOnlySwitch.visibleProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        offersOnlySwitch.managedProperty().bind(bisqEasyChatModel.getOfferOnlyVisible());
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().bind(bisqEasyChatModel.getIsTradeChannelVisible());
        offersOnlySwitch.selectedProperty().bindBidirectional(bisqEasyChatModel.getOfferOnly());

        isBisqEasyPrivateTradeChatChannelPin = EasyBind.subscribe(bisqEasyChatModel.getIsBisqEasyPrivateTradeChatChannel(), value -> {
            if (value) {
                if (!chatMessagesComponent.getChildren().contains(tradeAssistant)) {
                    chatMessagesComponent.getChildren().add(0, tradeAssistant);
                    VBox.setMargin(tradeAssistant, new Insets(5, 25, 25, 25));
                }
            } else {
                chatMessagesComponent.getChildren().remove(tradeAssistant);
            }
        });
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        offersOnlySwitch.visibleProperty().unbind();
        offersOnlySwitch.managedProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.visibleProperty().unbind();
        bisqEasyPrivateTradeChatChannelSelection.managedProperty().unbind();
        offersOnlySwitch.selectedProperty().unbindBidirectional(bisqEasyChatModel.getOfferOnly());
        isBisqEasyPrivateTradeChatChannelPin.unsubscribe();
    }
}
