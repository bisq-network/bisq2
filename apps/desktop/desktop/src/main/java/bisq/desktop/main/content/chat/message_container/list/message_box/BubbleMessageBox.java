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
import bisq.chat.Citation;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.reactions.Reaction;
import bisq.common.data.Pair;
import bisq.common.locale.LanguageRepository;
import bisq.desktop.common.utils.ClipboardUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.DropdownMenu;
import bisq.desktop.components.controls.SelectableLabel;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.chat.message_container.list.ChatMessageListItem;
import bisq.desktop.main.content.chat.message_container.list.ChatMessagesListController;
import bisq.desktop.main.content.chat.message_container.list.reactions_box.ActiveReactionsDisplayBox;
import bisq.desktop.main.content.chat.message_container.list.reactions_box.ReactMenuBox;
import bisq.desktop.main.content.chat.message_container.list.reactions_box.ToggleReaction;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class BubbleMessageBox extends MessageBox {
    protected static final double CHAT_MESSAGE_BOX_MAX_WIDTH = 630; // TODO: it should be 510 because of reactions on min size
    protected static final double OFFER_MESSAGE_USER_ICON_SIZE = 50;
    protected static final Insets ACTION_ITEMS_MARGIN = new Insets(2, 0, -2, 0);
    private static final List<Reaction> REACTIONS_ORDER = Arrays.asList(Reaction.THUMBS_UP, Reaction.THUMBS_DOWN, Reaction.HAPPY,
            Reaction.LAUGH, Reaction.HEART, Reaction.PARTY);
    private static final int MAX_NUM_SUPPORTED_LANGUAGES = 5;

    protected final ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item;
    protected final ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list;
    protected final ChatMessagesListController controller;
    protected final UserProfileIcon userProfileIcon = new UserProfileIcon(60);
    protected final HBox actionsHBox = new HBox(5);
    protected final VBox quotedMessageVBox, contentVBox;
    protected ActiveReactionsDisplayBox activeReactionsDisplayHBox;
    protected ReactMenuBox reactMenuBox;
    protected Label userName;
    protected Label dateTime;
    protected final SelectableLabel message;
    protected HBox userNameAndDateHBox;
    protected final HBox messageBgHBox;
    protected final HBox messageHBox;
    protected final HBox supportedLanguagesHBox;
    protected final HBox amountAndPriceBox;
    protected final HBox paymentAndSettlementMethodsBox;
    protected VBox userProfileIconVbox;
    protected BisqMenuItem copyAction;
    protected DropdownMenu moreActionsMenu;
    private Subscription showHighlightedPin, reactMenuPin;
    private Timeline timeline;

    public BubbleMessageBox(ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>> item,
                            ListView<ChatMessageListItem<? extends ChatMessage, ? extends ChatChannel<? extends ChatMessage>>> list,
                            ChatMessagesListController controller) {
        this.item = item;
        this.list = list;
        this.controller = controller;

        setUpUserNameAndDateTime();
        setUpUserProfileIcon();
        setUpReactions();
        setUpActions();
        addActionsHandlers();
        addOnMouseEventHandlers();

        supportedLanguagesHBox = createAndGetSupportedLanguagesBox();
        amountAndPriceBox = createAndGetAmountAndPriceBox();
        paymentAndSettlementMethodsBox = createAndGetPaymentAndSettlementMethodsBox();
        quotedMessageVBox = createAndGetQuotedMessageBox();
        handleQuoteMessageBox();
        message = createAndGetMessage();
        messageBgHBox = createAndGetMessageBackground();
        messageHBox = createAndGetMessageBox();

        setFillWidth(true);
        HBox.setHgrow(this, Priority.ALWAYS);

        contentVBox = new VBox();
        contentVBox.setMaxWidth(CHAT_BOX_MAX_WIDTH);
        contentVBox.getStyleClass().add("chat-message-content-box");
        getChildren().setAll(contentVBox);
        setAlignment(Pos.CENTER);

        setUpMessageHighlight();
    }

    protected void setUpUserNameAndDateTime() {
        userName = new Label();
        userName.getStyleClass().addAll("text-fill-white", "font-size-09", "font-default");

        dateTime = new Label();
        dateTime.getStyleClass().addAll("text-fill-grey-dimmed", "font-size-09", "font-light");
        dateTime.setText(item.getDate());
        dateTime.setVisible(false);
    }

    private void setUpUserProfileIcon() {
        userProfileIcon.setSize(60);
        userProfileIconVbox = new VBox(userProfileIcon);

        item.getSenderUserProfile().ifPresent(author -> {
            userName.setText(author.getUserName());
            userName.setOnMouseClicked(e -> controller.onMention(author));

            userProfileIcon.setUserProfile(author);
            userProfileIcon.setCursor(Cursor.HAND);
            userProfileIcon.setOnMouseClicked(e -> controller.onShowChatUserDetails(item.getChatMessage()));
        });
    }

    private void setUpReactions() {
        // Active Reactions Display
        ToggleReaction toggleReactionDisplayMenuFunction = reactionItem ->
                controller.onReactMessage(item.getChatMessage(), reactionItem.getReaction(), item.getChatChannel());
        activeReactionsDisplayHBox = new ActiveReactionsDisplayBox(item.getUserReactions().values(), toggleReactionDisplayMenuFunction);

        // React Menu
        ToggleReaction toggleReactionReactMenuFunction = reactionItem -> {
            controller.onReactMessage(item.getChatMessage(), reactionItem.getReaction(), item.getChatChannel());
            reactMenuBox.hideMenu();
        };
        reactMenuBox = new ReactMenuBox(item.getUserReactions(), REACTIONS_ORDER, toggleReactionReactMenuFunction,
                "react-grey", "react-white", "react-green");
        reactMenuBox.setTooltip(Res.get("action.react"));
        reactMenuBox.setVisible(item.getChatMessage().canShowReactions());
        reactMenuBox.setManaged(item.getChatMessage().canShowReactions());

        reactMenuPin = EasyBind.subscribe(reactMenuBox.getIsMenuShowing(), isShowing -> {
            if (!isShowing && !isHover()) {
                showDateTimeAndActionsMenu(false);
            }
        });
    }

    protected void setUpActions() {
        copyAction = new BisqMenuItem("copy-grey", "copy-white");
        copyAction.useIconOnly();
        copyAction.setTooltip(Res.get("action.copyToClipboard"));
        actionsHBox.setVisible(false);
        HBox.setMargin(copyAction, ACTION_ITEMS_MARGIN);
        HBox.setMargin(reactMenuBox, ACTION_ITEMS_MARGIN);
    }

    protected void addActionsHandlers() {
    }

    private void addOnMouseEventHandlers() {
        setOnMouseEntered(e -> showDateTimeAndActionsMenu(true));
        setOnMouseExited(e -> showDateTimeAndActionsMenu(false));
    }

    @Override
    public void dispose() {
        setOnMouseEntered(null);
        setOnMouseExited(null);

        showHighlightedPin.unsubscribe();
        reactMenuPin.unsubscribe();

        activeReactionsDisplayHBox.dispose();
        reactMenuBox.dispose();
        userProfileIcon.dispose();

        if (timeline != null) {
            timeline.stop();
        }
        if (messageBgHBox != null) {
            messageBgHBox.setEffect(null);
        }

        quotedMessageVBox.setOnMouseClicked(null);
    }

    private void showDateTimeAndActionsMenu(boolean shouldShow) {
        if (shouldShow) {
            if ((moreActionsMenu != null && moreActionsMenu.getIsMenuShowing().get()) || reactMenuBox.getIsMenuShowing().get()) {
                return;
            }
            dateTime.setVisible(true);
            showActionsHBox();
        } else {
            if ((moreActionsMenu == null || !moreActionsMenu.getIsMenuShowing().get()) && !reactMenuBox.getIsMenuShowing().get()) {
                dateTime.setVisible(false);
                actionsHBox.setVisible(false);
            }
        }
    }

    protected void showActionsHBox() {
        actionsHBox.setVisible(true);
    }

    private HBox createAndGetSupportedLanguagesBox() {
        HBox hBox = new HBox(3);
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            Label iconLabel = new Label(":", ImageUtil.getImageViewById("language-grey"));
            iconLabel.setGraphicTextGap(3);
            iconLabel.setTooltip(new BisqTooltip(Res.get("chat.message.supportedLanguages.Tooltip")));
            hBox.getChildren().add(iconLabel);
            BisqEasyOfferbookMessage chatMessage = (BisqEasyOfferbookMessage) item.getChatMessage();
            if (chatMessage.getBisqEasyOffer().isPresent()) {
                BisqEasyOffer offer = chatMessage.getBisqEasyOffer().get();
                int codesCount = Math.min(offer.getSupportedLanguageCodes().size(), MAX_NUM_SUPPORTED_LANGUAGES);
                for (int i = 0; i < codesCount; i++) {
                    String languageCode = offer.getSupportedLanguageCodes().get(i).toUpperCase();
                    Label codeLabel = (i == codesCount - 1) ? new Label(languageCode) : new Label(languageCode + ",");
                    codeLabel.setTooltip(new BisqTooltip(LanguageRepository.getDisplayString(languageCode)));
                    codeLabel.getStyleClass().add("text-fill-white");
                    hBox.getChildren().add(codeLabel);
                }
            }
        }
        return hBox;
    }

    private HBox createAndGetAmountAndPriceBox() {
        HBox hBox = new HBox(5);
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            Optional<Pair<String, String>> amountAndPriceSpec = item.getBisqEasyOfferAmountAndPriceSpec();
            if (amountAndPriceSpec.isPresent()) {
                Label amount = new Label(amountAndPriceSpec.get().getFirst());
                amount.getStyleClass().add("text-fill-white");
                Label price = new Label(amountAndPriceSpec.get().getSecond());
                price.getStyleClass().add("text-fill-white");
                Label connector = new Label("@");
                hBox.getChildren().addAll(amount, connector, price);
            }
        }
        return hBox;
    }

    private HBox createAndGetPaymentAndSettlementMethodsBox() {
        if (item.isBisqEasyPublicChatMessageWithOffer()) {
            HBox hBox = BisqEasyViewUtils.getPaymentAndSettlementMethodsBox(item.getBisqEasyOfferPaymentMethods(), item.getBisqEasyOfferSettlementMethods());
            hBox.setAlignment(Pos.BOTTOM_LEFT);
            return hBox;
        }
        return new HBox();
    }

    private VBox createAndGetQuotedMessageBox() {
        VBox vBox = new VBox(5);
        vBox.setVisible(false);
        vBox.setManaged(false);
        VBox.setMargin(vBox, new Insets(15, 0, 10, 5));
        return vBox;
    }

    private void handleQuoteMessageBox() {
        Optional<Citation> optionalCitation = item.getCitation();
        if (optionalCitation.isPresent()) {
            Citation citation = optionalCitation.get();
            if (citation.isValid()) {
                quotedMessageVBox.setVisible(true);
                quotedMessageVBox.setManaged(true);
                Label quotedMessageField = new Label();
                quotedMessageField.setWrapText(true);
                quotedMessageField.setText(citation.getText());
                quotedMessageField.setStyle("-fx-fill: -fx-mid-text-color");
                Label userName = new Label(controller.getUserName(citation.getAuthorUserProfileId()));
                userName.getStyleClass().add("font-medium");
                userName.setStyle("-fx-text-fill: -bisq-mid-grey-30");
                quotedMessageVBox.getChildren().setAll(userName, quotedMessageField);
                quotedMessageVBox.setOnMouseClicked(e -> controller.onClickQuoteMessage(citation.getChatMessageId()));
                quotedMessageVBox.getStyleClass().add("hand-cursor");
            }
        } else {
            quotedMessageVBox.getChildren().clear();
            quotedMessageVBox.setVisible(false);
            quotedMessageVBox.setManaged(false);
            quotedMessageVBox.setOnMouseClicked(null);
        }
    }

    private SelectableLabel createAndGetMessage() {
        SelectableLabel label = new SelectableLabel(item.getMessage());
        label.maxWidthProperty().unbind();
        label.setWrapText(true);
        label.setPadding(new Insets(10));
        label.getStyleClass().addAll("text-fill-white", "medium-text", "font-default");
        label.setEditable(false);
        return label;
    }

    private HBox createAndGetMessageBackground() {
        HBox hBox = new HBox(15);
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setMaxWidth(CHAT_MESSAGE_BOX_MAX_WIDTH);
        HBox.setHgrow(hBox, Priority.SOMETIMES);
        if (item.hasTradeChatOffer()) {
            hBox.setPadding(new Insets(15));
        } else {
            hBox.setPadding(new Insets(5, 15, 5, 15));
        }
        return hBox;
    }

    private HBox createAndGetMessageBox() {
        HBox hBox = new HBox(5);
        VBox.setMargin(hBox, new Insets(10, 0, 0, 0));
        return hBox;
    }

    protected static void onCopyMessage(ChatMessage chatMessage) {
        ClipboardUtil.copyToClipboard(chatMessage.getTextOrNA());
    }

    protected static void onCopyMessage(String chatMessageText) {
        ClipboardUtil.copyToClipboard(chatMessageText);
    }

    private void setUpMessageHighlight() {
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(86, 174, 72)); // Bisq2 green
        dropShadow.setBlurType(BlurType.GAUSSIAN);

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(10),
                        new KeyValue(dropShadow.radiusProperty(), dropShadow.getRadius()),
                        new KeyValue(dropShadow.spreadProperty(), dropShadow.getSpread())),
                new KeyFrame(Duration.seconds(11),
                        new KeyValue(dropShadow.radiusProperty(), 0),
                        new KeyValue(dropShadow.spreadProperty(), 0)));
        timeline.setOnFinished(e -> messageBgHBox.setEffect(null));

        showHighlightedPin = EasyBind.subscribe(item.getShowHighlighted(), showHighlighted -> {
            if (showHighlighted) {
                dropShadow.setRadius(15);
                dropShadow.setSpread(0.3);
                messageBgHBox.setEffect(dropShadow);
                timeline.play();
            } else {
                timeline.stop();
                messageBgHBox.setEffect(null);
            }
        });
    }
}
