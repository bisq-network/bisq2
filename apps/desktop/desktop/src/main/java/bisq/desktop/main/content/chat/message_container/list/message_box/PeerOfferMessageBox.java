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
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
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
    private static final double OFFER_MESSAGE_BOX_MIN_WIDTH = 310;

    private Button takeOfferButton;
    private Label peerNickName;

    public PeerOfferMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                               ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                               ChatMessagesListController controller) {
        super(item, list, controller);

        actionsHBox.getChildren().setAll(replyAction, openPrivateChatAction, copyAction, moreActionsMenu, Spacer.fillHBox());

        VBox.setMargin(userNameAndDateHBox, new Insets(-5, 0, 5, 10));
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, actionsHBox);
    }

    @Override
    protected void setUpPeerMessage() {
        // User profile icon
        userProfileIcon.setSize(OFFER_MESSAGE_USER_ICON_SIZE);
        userProfileIconVbox.getChildren().addAll(item.getReputationScoreDisplay(), Spacer.fillVBox(), supportedLanguagesHBox);
        supportedLanguagesHBox.setAlignment(Pos.CENTER);
        userProfileIconVbox.setSpacing(10);
        userProfileIconVbox.setAlignment(Pos.CENTER);
        item.getReputationScoreDisplay().setScale(0.8);
        item.getReputationScoreDisplay().setAlignment(Pos.CENTER);

        Pair<HBox, Button> takeOfferLabelAndButton = createAndGetTakeOfferTitleBoxAndButton();
        HBox takeOfferTitle = takeOfferLabelAndButton.getFirst();
        takeOfferButton = takeOfferLabelAndButton.getSecond();

        // Offer content
        paymentAndSettlementMethodsBox.setPadding(new Insets(0, 0, 2, 0));
        HBox methodsAndTakeOfferButtonBox = new HBox(10, paymentAndSettlementMethodsBox, Spacer.fillHBox(), takeOfferButton);
        VBox offerMessage = new VBox(10, takeOfferTitle, amountAndPriceBox, Spacer.fillVBox(), methodsAndTakeOfferButtonBox);
        offerMessage.setMinWidth(OFFER_MESSAGE_BOX_MIN_WIDTH);
        VBox.setMargin(offerMessage, new Insets(0, 0, 0, 7));

        Region separator = new Region();
        separator.getStyleClass().add("take-offer-vLine");
        HBox offerContent = new HBox(15, userProfileIconVbox, separator, offerMessage);
        userProfileIconVbox.setAlignment(Pos.TOP_CENTER);
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
        peerNickName = new Label(StringUtils.truncate(item.getNickName(), 28));
        peerNickName.getStyleClass().addAll("code-block", "offerbook-peer-name", "hand-cursor");
        peerNickName.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        HBox messageTitleBox = new HBox(5, messageTitle, peerNickName);
        messageTitleBox.getStyleClass().add(isBuy ? "bisq-easy-offer-sell-btc-title" : "bisq-easy-offer-buy-btc-title");
        messageTitleBox.setAlignment(Pos.BASELINE_LEFT);

        // Button
        Button button = new Button(isBuy ? Res.get("offer.takeOffer.sell.button") : Res.get("offer.takeOffer.buy.button"));
        button.getStyleClass().addAll("take-offer-button", "medium-text", "font-default");
        button.getStyleClass().add(isBuy ? "sell-btc-button" : "buy-btc-button");
        button.setOnAction(e -> controller.onTakeOffer(bisqEasyOfferbookMessage));
        button.setDefaultButton(!item.isWasOfferAlreadyTaken());

        return new Pair<>(messageTitleBox, button);
    }

    @Override
    public void dispose() {
        super.dispose();

        takeOfferButton.setOnAction(null);
        peerNickName.setOnMouseClicked(null);
    }
}
