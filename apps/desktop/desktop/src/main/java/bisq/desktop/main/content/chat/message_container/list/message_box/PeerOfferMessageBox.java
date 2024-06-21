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

package bisq.desktop.main.content.chat.message_container.list.message_box;

import bisq.chat.ChatChannel;
import bisq.chat.ChatMessage;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.data.Pair;
import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import static com.google.common.base.Preconditions.checkArgument;

public final class PeerOfferMessageBox extends PeerTextMessageBox {
    private Button takeOfferButton;
    private Label peerNickName;

    public PeerOfferMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                               ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                               ChatMessagesListController controller) {
        super(item, list, controller);

        HBox.setMargin(copyIcon, new Insets(4, 0, -4, 0));
        HBox.setMargin(supportedLanguages, new Insets(5, 0, -5, 0));
        actionsHBox.getChildren().setAll(replyIcon, pmIcon, copyIcon, supportedLanguages, moreOptionsMenu, Spacer.fillHBox());

        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, actionsHBox);
    }

    @Override
    protected void setUpPeerMessage() {
        // User profile icon
        userProfileIcon.setSize(OFFER_MESSAGE_USER_ICON_SIZE);

        // Reputation
        Label reputationLabel = new Label(Res.get("chat.message.reputation").toUpperCase());
        VBox reputationVBox = new VBox(4, reputationLabel, item.getReputationScoreDisplay());
        reputationVBox.setAlignment(Pos.CENTER);
        reputationVBox.getStyleClass().add("reputation");

        // Take offer title and button
        Pair<HBox, Button> takeOfferLabelAndButton = createAndGetTakeOfferTitleBoxAndButton();
        HBox takeOfferTitle = takeOfferLabelAndButton.getFirst();
        takeOfferButton = takeOfferLabelAndButton.getSecond();

        // Message
        message.getStyleClass().add("chat-peer-offer-message");

        // Offer content
        VBox offerMessage = new VBox(10, takeOfferTitle, message, takeOfferButton);
        Region separator = new Region();
        separator.getStyleClass().add("take-offer-vLine");
        HBox offerContent = new HBox(15, userProfileIconVbox, reputationVBox, separator, offerMessage);
        userProfileIconVbox.setAlignment(Pos.CENTER);
        reputationVBox.setAlignment(Pos.CENTER);
        offerContent.setAlignment(Pos.CENTER);

        // Message background
        messageBgHBox.getStyleClass().add("chat-peer-offer-message-bg");
        messageBgHBox.getChildren().setAll(offerContent);
        messageBgHBox.setAlignment(Pos.CENTER_LEFT);
        messageBgHBox.setMaxWidth(Control.USE_PREF_SIZE);
    }

    private Pair<HBox, Button> createAndGetTakeOfferTitleBoxAndButton() {
        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
        checkArgument(bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent(),
                "Bisq Easy Offerbook message must contain an offer");

        boolean isBuy = bisqEasyOfferbookMessage.getBisqEasyOffer().get().getDirection() == Direction.BUY;

        // Label
        String title = isBuy
                ? Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessageTitle.sell")
                : Res.get("bisqEasy.tradeWizard.review.chatMessage.peerMessageTitle.buy");
        Label messageTitle = new Label(title);
        messageTitle.getStyleClass().addAll("bisq-easy-offer-title", "normal-text", "font-default");
        messageTitle.setPadding(new Insets(0, 0, 0, 7));
        peerNickName = new Label(StringUtils.truncate(item.getNickName(), 28));
        peerNickName.getStyleClass().addAll("code-block", "hand-cursor");
        peerNickName.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        HBox messageTitleBox = new HBox(5, messageTitle, peerNickName);
        messageTitleBox.getStyleClass().add(isBuy ? "bisq-easy-offer-sell-btc-title" : "bisq-easy-offer-buy-btc-title");
        messageTitleBox.setAlignment(Pos.BASELINE_LEFT);

        // Button
        Button button = new Button(isBuy ? Res.get("offer.takeOffer.sell.button") : Res.get("offer.takeOffer.buy.button"));
        button.getStyleClass().addAll("take-offer-button", "medium-text", "font-default");
        button.getStyleClass().add(isBuy ? "sell-btc-button" : "buy-btc-button");
        button.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage));
        button.setDefaultButton(!item.isOfferAlreadyTaken());
        VBox.setMargin(button, new Insets(10, 0, 0, 7));

        return new Pair<>(messageTitleBox, button);
    }

    @Override
    public void cleanup() {
        super.cleanup();

        takeOfferButton.setOnAction(null);
        peerNickName.setOnMouseClicked(null);
    }
}
