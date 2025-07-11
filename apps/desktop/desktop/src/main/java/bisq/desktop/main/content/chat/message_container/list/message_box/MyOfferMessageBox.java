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
import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import static com.google.common.base.Preconditions.checkArgument;

public final class MyOfferMessageBox extends BubbleMessageBox {
    private final Label myOfferTitle;
    private BisqMenuItem removeOffer;

    public MyOfferMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                             ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                             ChatMessagesListController controller) {
        super(item, list, controller);

        // User profile icon
        userProfileIcon.setSize(OFFER_MESSAGE_USER_ICON_SIZE);
        userProfileIconVbox.getChildren().addAll(item.getReputationScoreDisplay(), Spacer.fillVBox(), supportedLanguagesHBox);
        supportedLanguagesHBox.setAlignment(Pos.CENTER);
        userProfileIconVbox.setSpacing(10);
        userProfileIconVbox.setAlignment(Pos.CENTER);
        item.getReputationScoreDisplay().setScale(0.8);
        item.getReputationScoreDisplay().setAlignment(Pos.CENTER);

        myOfferTitle = createAndGetMyOfferTitle();

        // Offer content
        VBox offerMessage = new VBox(10, myOfferTitle, amountAndPriceBox, Spacer.fillVBox(), paymentAndSettlementMethodsBox);
        HBox messageContent = new HBox(30, offerMessage, userProfileIconVbox);
        VBox.setMargin(paymentAndSettlementMethodsBox, new Insets(3, 0, 0, 0));

        // Message background
        messageBgHBox.getStyleClass().add("chat-my-offer-message-bg");
        messageBgHBox.getChildren().setAll(messageContent);
        messageBgHBox.setMaxWidth(Control.USE_PREF_SIZE);

        // Actions
        actionsHBox.getChildren().setAll(Spacer.fillHBox(), copyAction, removeOffer);

        contentVBox.setAlignment(Pos.CENTER_RIGHT);
        contentVBox.getChildren().setAll(userNameAndDateHBox, messageBgHBox, actionsHBox);
    }

    @Override
    protected void setUpUserNameAndDateTime() {
        super.setUpUserNameAndDateTime();

        userNameAndDateHBox = new HBox(10, dateTime, item.getBondedRoleBadge(), userName);
        userNameAndDateHBox.setAlignment(Pos.CENTER_RIGHT);
        VBox.setMargin(userNameAndDateHBox, new Insets(0, 10, 5, 0));
    }

    @Override
    protected void setUpActions() {
        super.setUpActions();

        removeOffer = new BisqMenuItem("delete-t-grey", "delete-t-red");
        removeOffer.useIconOnly();
        removeOffer.setTooltip(Res.get("offer.deleteOffer"));
        HBox.setMargin(removeOffer, ACTION_ITEMS_MARGIN);
    }

    @Override
    protected void addActionsHandlers() {
        copyAction.setOnAction(e -> onCopyMessage(String.format("%s\n%s", myOfferTitle.getText(), message.getText())));
        removeOffer.setOnAction(e -> controller.onDeleteMessage(item.getChatMessage()));
    }

    private Label createAndGetMyOfferTitle() {
        BisqEasyOfferbookMessage bisqEasyOfferbookMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
        checkArgument(bisqEasyOfferbookMessage.getBisqEasyOffer().isPresent(),
                "Bisq Easy Offerbook message must contain an offer");

        Direction direction = bisqEasyOfferbookMessage.getBisqEasyOffer().get().getDirection();
        String directionString = StringUtils.capitalize(Res.get("offer." + direction.name().toLowerCase()));
        String title = Res.get("bisqEasy.tradeWizard.review.chatMessage.myMessageTitle", directionString);
        Label label = new Label(title);
        label.getStyleClass().addAll("bisq-easy-offer-title", "normal-text", "font-default");
        boolean isBuy = direction == Direction.BUY;
        label.getStyleClass().add(isBuy ? "bisq-easy-offer-buy-btc-title" : "bisq-easy-offer-sell-btc-title");
        return label;
    }

    @Override
    public void dispose() {
        super.dispose();

        copyAction.setOnAction(null);
        removeOffer.setOnAction(null);
    }
}
