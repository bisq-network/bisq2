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

package bisq.desktop.primary.overlay.createOffer.review;

import bisq.chat.bisqeasy.message.PublicTradeChatMessage;
import bisq.chat.bisqeasy.message.TradeChatOffer;
import bisq.common.currency.Market;
import bisq.common.monetary.Fiat;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.primary.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import bisq.presentation.formatters.AmountFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
class ReviewOfferView extends View<StackPane, ReviewOfferModel, ReviewOfferController> {
    private final static int BUTTON_WIDTH = 140;

    private final Label topHeadLine, createOfferLabel;
    private final BisqTableView<ListItem> tableView;
    private final Button createOfferButton;
    private final Label createOfferText;
    private final HBox createOfferHBox;
    private final Label noMatchingOffersLabel;
    private Subscription matchingOffersFoundPin;
    private final VBox content, createOfferSuccessFeedback, takeOfferSuccessFeedback;
    private final Button viewOfferButton;
    private final Button openPrivateChannelButton;
    private Subscription showCreateOfferSuccessPin, showTakeOfferSuccessPin;

    ReviewOfferView(ReviewOfferModel model, ReviewOfferController controller) {
        super(new StackPane(), model, controller);

        content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.getStyleClass().add("bisq-content-bg");

        topHeadLine = new Label();
        topHeadLine.getStyleClass().add("bisq-text-headline-2");

        noMatchingOffersLabel = new Label();
        noMatchingOffersLabel.setTextAlignment(TextAlignment.CENTER);
        noMatchingOffersLabel.setAlignment(Pos.CENTER);
        noMatchingOffersLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("offer-review");
        tableView.setMinWidth(700);
        tableView.setMaxWidth(700);

        createOfferLabel = new Label();
        createOfferLabel.getStyleClass().add("bisq-text-headline-2");

        createOfferText = new Label();
        createOfferText.setWrapText(true);
        createOfferText.setId("chat-messages-message");

        createOfferButton = new Button(Res.get("createOffer"));
        createOfferButton.setDefaultButton(true);
        createOfferButton.setMinWidth(BUTTON_WIDTH);
        createOfferButton.setMaxWidth(BUTTON_WIDTH);

        HBox.setHgrow(createOfferText, Priority.ALWAYS);
        createOfferHBox = new HBox(15, createOfferText, Spacer.fillHBox(), createOfferButton);
        createOfferHBox.getStyleClass().add("create-offer-message-my-offer");
        createOfferHBox.setAlignment(Pos.CENTER_LEFT);

        // margin is set in onViewAttached
        content.getChildren().addAll(topHeadLine, noMatchingOffersLabel, tableView, createOfferLabel, createOfferHBox);

        createOfferSuccessFeedback = new VBox();
        createOfferSuccessFeedback.setVisible(false);
        viewOfferButton = new Button(Res.get("onboarding.completed.createOfferSuccess.viewOffer"));
        configCreateOfferSuccess();

        takeOfferSuccessFeedback = new VBox();
        takeOfferSuccessFeedback.setVisible(false);
        openPrivateChannelButton = new Button(Res.get("onboarding.completed.takeOfferSuccess.openPrivateChannel"));
        configTakeOfferSuccess();

        StackPane.setMargin(createOfferSuccessFeedback, new Insets(-55, 0, 380, 0));
        StackPane.setMargin(takeOfferSuccessFeedback, new Insets(-55, 0, 380, 0));
        root.getChildren().addAll(content, createOfferSuccessFeedback, takeOfferSuccessFeedback);
    }

    @Override
    protected void onViewAttached() {
        configTableView();
        Transitions.removeEffect(content);

        viewOfferButton.setOnAction(e -> controller.onOpenBisqEasy());
        openPrivateChannelButton.setOnAction(e -> controller.onOpenPrivateChat());
        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        createOfferText.setText(model.getMyOfferText());
        noMatchingOffersLabel.setText(model.isShowMatchingOffers() ?
                Res.get("onboarding.completed.noMatchingOffers") :
                Res.get("onboarding.completed.createOfferMode")
        );

        matchingOffersFoundPin = EasyBind.subscribe(model.getMatchingOffersFound(), matchingOffersFound -> {
            tableView.setVisible(matchingOffersFound);
            tableView.setManaged(matchingOffersFound);
            createOfferLabel.setVisible(matchingOffersFound);
            createOfferLabel.setManaged(matchingOffersFound);
            noMatchingOffersLabel.setVisible(!matchingOffersFound);
            noMatchingOffersLabel.setManaged(!matchingOffersFound);

            if (matchingOffersFound) {
                createOfferHBox.setMinWidth(tableView.getMaxWidth());
                createOfferHBox.setMaxWidth(tableView.getMaxWidth());
                topHeadLine.setText(Res.get("onboarding.completed.headline.takeOffer"));
                createOfferLabel.setText(Res.get("onboarding.completed.headLine2.createOffer"));
            } else {
                createOfferHBox.setMinWidth(550);
                createOfferHBox.setMaxWidth(550);
                topHeadLine.setText(Res.get("createOffer"));
            }

            int numEntries = model.getMatchingOffers().size();

            tableView.setMaxHeight(42 + numEntries * 55);
            if (numEntries == 3) {
                VBox.setMargin(topHeadLine, new Insets(25, 0, 0, 0));
                VBox.setMargin(tableView, new Insets(-5, 0, 40, 0));
                VBox.setMargin(createOfferHBox, new Insets(10, 0, 0, 0));
            } else if (numEntries == 2) {
                VBox.setMargin(topHeadLine, new Insets(40, 0, 0, 0));
                VBox.setMargin(tableView, new Insets(-5, 0, 60, 0));
                VBox.setMargin(createOfferHBox, new Insets(10, 0, 0, 0));
            } else if (numEntries == 1) {
                VBox.setMargin(topHeadLine, new Insets(50, 0, 0, 0));
                VBox.setMargin(tableView, new Insets(5, 0, 70, 0));
                VBox.setMargin(createOfferHBox, new Insets(20, 0, 0, 0));
            } else {
                VBox.setMargin(topHeadLine, new Insets(80, 0, 5, 0));
                VBox.setMargin(createOfferHBox, new Insets(40, 0, 0, 0));
            }

            if (numEntries == 0) {
                createOfferHBox.setPadding(new Insets(15, 26, 15, 15));
            } else {
                // Aligned with take offer button
                createOfferHBox.setPadding(new Insets(12));
            }

        });

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(),
                show -> {
                    createOfferSuccessFeedback.setVisible(show);
                    if (show) {
                        Transitions.blurLight(content, -0.5);
                        Transitions.slideInTop(createOfferSuccessFeedback, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
        showTakeOfferSuccessPin = EasyBind.subscribe(model.getShowTakeOfferSuccess(),
                show -> {
                    takeOfferSuccessFeedback.setVisible(show);
                    if (show) {
                        Transitions.blurLight(content, -0.5);
                        Transitions.slideInTop(takeOfferSuccessFeedback, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        viewOfferButton.setOnAction(null);
        openPrivateChannelButton.setOnAction(null);
        matchingOffersFoundPin.unsubscribe();
        showCreateOfferSuccessPin.unsubscribe();
        showTakeOfferSuccessPin.unsubscribe();
    }

    private void configTableView() {
        if (!tableView.getColumns().isEmpty()) {
            return;
        }
        String peer = model.getDirection() == Direction.BUY ?
                Res.get("seller") :
                Res.get("buyer");
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(peer)
                .isFirst()
                .setCellFactory(new Callback<>() {
                    @Override
                    public TableCell<ListItem, ListItem> call(TableColumn<ListItem, ListItem> column) {
                        return new TableCell<>() {
                            private final Label userName = new Label();
                            private final ImageView roboIcon = new ImageView();
                            private final HBox hBox = new HBox(10, roboIcon, userName);

                            @Override
                            public void updateItem(final ListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                {
                                    roboIcon.setFitWidth(20);
                                    roboIcon.setFitHeight(20);
                                    userName.setId("chat-user-name");
                                    hBox.setAlignment(Pos.CENTER_LEFT);
                                }
                                if (item != null && !empty) {
                                    userName.setText(item.getUserName());
                                    item.getSenderUserProfile().ifPresent(userProfile ->
                                            roboIcon.setImage(RoboHash.getImage(userProfile.getPubKeyHash())));
                                    setGraphic(hBox);
                                } else {
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                })
                .comparator(Comparator.comparing(ListItem::getUserName))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("amount"))
                .valueSupplier(ListItem::getAmount)
                .comparator(Comparator.comparing(ListItem::getAmountAsLong))
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("reputation"))
                .setCellFactory(new Callback<>() {
                    @Override
                    public TableCell<ListItem, ListItem> call(TableColumn<ListItem, ListItem> column) {
                        return new TableCell<>() {
                            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();

                            @Override
                            public void updateItem(final ListItem item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null && !empty) {
                                    reputationScoreDisplay.applyReputationScore(item.getReputationScore());
                                    setGraphic(reputationScoreDisplay);
                                } else {
                                    setGraphic(null);
                                }
                            }
                        };
                    }
                })
                .comparator(Comparator.comparing(ListItem::getReputationScore))
                .build());
        BisqTableColumn<ListItem> takeOffer = new BisqTableColumn.Builder<ListItem>()
                .defaultCellFactory(BisqTableColumn.DefaultCellFactory.BUTTON)
                .value(Res.get("takeOffer"))
                .actionHandler(controller::onTakeOffer)
                .updateItemWithButtonHandler((item, button) -> {
                    Button takeOfferButton = (Button) button;
                    takeOfferButton.setDefaultButton(true);
                    takeOfferButton.setMinWidth(BUTTON_WIDTH);
                    takeOfferButton.setMaxWidth(BUTTON_WIDTH);
                })
                .isLast()
                .build();
        //takeOffer.setStyle("-fx-padding: 0 5 0 10;");
        tableView.getColumns().add(takeOffer);
    }

    private void configCreateOfferSuccess() {
        double width = 700;
        createOfferSuccessFeedback.setAlignment(Pos.TOP_CENTER);
        createOfferSuccessFeedback.setMaxWidth(width);
        createOfferSuccessFeedback.setId("sellBtcWarning");
        createOfferSuccessFeedback.setPadding(new Insets(30, 0, 30, 0));
        createOfferSuccessFeedback.setSpacing(20);

        Label headLineLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(width - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");

        viewOfferButton.setDefaultButton(true);
        VBox.setMargin(viewOfferButton, new Insets(10, 0, 0, 0));
        createOfferSuccessFeedback.getChildren().addAll(headLineLabel, subtitleLabel, viewOfferButton);
    }

    private void configTakeOfferSuccess() {
        double width = 700;
        takeOfferSuccessFeedback.setAlignment(Pos.TOP_CENTER);
        takeOfferSuccessFeedback.setMaxWidth(width);
        takeOfferSuccessFeedback.setId("sellBtcWarning");
        takeOfferSuccessFeedback.setPadding(new Insets(30, 0, 30, 0));
        takeOfferSuccessFeedback.setSpacing(20);

        Label headLineLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(width - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.getStyleClass().addAll("bisq-text-13", "wrap-text");

        openPrivateChannelButton.setDefaultButton(true);
        VBox.setMargin(openPrivateChannelButton, new Insets(10, 0, 0, 0));
        takeOfferSuccessFeedback.getChildren().addAll(headLineLabel, subtitleLabel, openPrivateChannelButton);
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    static class ListItem implements TableItem {
        private final PublicTradeChatMessage chatMessage;
        private final Optional<UserProfile> senderUserProfile;
        private final String userName;
        private final String amount;
        private final long amountAsLong;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;

        public ListItem(PublicTradeChatMessage chatMessage, UserProfileService userProfileService, ReputationService reputationService) {
            this.chatMessage = chatMessage;
            senderUserProfile = userProfileService.findUserProfile(chatMessage.getAuthorId());
            userName = senderUserProfile.map(UserProfile::getUserName).orElse("");
            Optional<TradeChatOffer> tradeChatOffer = chatMessage.getTradeChatOffer();
            amountAsLong = tradeChatOffer.map(TradeChatOffer::getQuoteSideAmount).orElse(0L);
            String code = tradeChatOffer.map(TradeChatOffer::getMarket)
                    .map(Market::getQuoteCurrencyCode)
                    .orElse("");
            amount = AmountFormatter.formatAmountWithCode(Fiat.of(amountAsLong, code), true);

            reputationScore = senderUserProfile.flatMap(reputationService::findReputationScore)
                    .orElse(ReputationScore.NONE);
        }
    }
}
