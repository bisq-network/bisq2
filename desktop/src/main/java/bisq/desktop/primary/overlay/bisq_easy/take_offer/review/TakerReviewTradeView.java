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

package bisq.desktop.primary.overlay.bisq_easy.take_offer.review;

import bisq.chat.bisqeasy.message.BisqEasyPublicChatMessage;
import bisq.desktop.common.utils.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.primary.main.content.components.ReputationScoreDisplay;
import bisq.desktop.primary.overlay.bisq_easy.take_offer.TakeOfferView;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.bisq_easy.BisqEasyOffer;
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
class TakerReviewTradeView extends View<StackPane, TakerReviewTradeModel, TakerReviewTradeController> {
    private final static int BUTTON_WIDTH = 140;
    private final static int FEEDBACK_WIDTH = 700;

    private final Label headLineLabel, createOfferLabel;
    private final BisqTableView<ListItem> tableView;
    private final Button createOfferButton;
    private final Label createOfferText;
    private final HBox createOfferHBox;
    private final Label subtitleLabel;
    private Subscription matchingOffersFoundPin;
    private final VBox content, createOfferSuccess, takeOfferSuccess;
    private final Button viewOfferButton;
    private final Button openPrivateChannelButton;
    private Subscription showCreateOfferSuccessPin, showTakeOfferSuccessPin;

    TakerReviewTradeView(TakerReviewTradeModel model, TakerReviewTradeController controller) {
        super(new StackPane(), model, controller);


        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headLineLabel = new Label();
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

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
        content.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, tableView, createOfferLabel, createOfferHBox, Spacer.fillVBox());

        viewOfferButton = new Button(Res.get("onboarding.completed.createOfferSuccess.viewOffer"));
        createOfferSuccess = new VBox(20);
        configCreateOfferSuccess();

        openPrivateChannelButton = new Button(Res.get("onboarding.completed.takeOfferSuccess.openPrivateChannel"));
        takeOfferSuccess = new VBox(20);
        configTakeOfferSuccess();

        StackPane.setMargin(createOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        StackPane.setMargin(takeOfferSuccess, new Insets(-TakeOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, createOfferSuccess, takeOfferSuccess);
    }

    @Override
    protected void onViewAttached() {
        if (true) return;
        Transitions.removeEffect(content);

        viewOfferButton.setOnAction(e -> controller.onOpenBisqEasy());
        openPrivateChannelButton.setOnAction(e -> controller.onOpenPrivateChat());
        createOfferButton.setOnAction(e -> controller.onCreateOffer());

        createOfferText.setText(model.getMyOfferText());
        subtitleLabel.setText(model.isShowMatchingOffers() ?
                Res.get("onboarding.completed.noMatchingOffers") :
                Res.get("onboarding.completed.createOfferMode")
        );

        matchingOffersFoundPin = EasyBind.subscribe(model.getMatchingOffersVisible(), matchingOffersVisible -> {
            tableView.setVisible(matchingOffersVisible);
            tableView.setManaged(matchingOffersVisible);
            createOfferLabel.setVisible(matchingOffersVisible);
            createOfferLabel.setManaged(matchingOffersVisible);
            subtitleLabel.setVisible(!matchingOffersVisible);
            subtitleLabel.setManaged(!matchingOffersVisible);

            createOfferHBox.setPadding(new Insets(15, 26, 15, 15));

            if (matchingOffersVisible) {
                maybeConfigTableView();
                createOfferHBox.setMinWidth(tableView.getMaxWidth());
                createOfferHBox.setMaxWidth(tableView.getMaxWidth());
                headLineLabel.setText(Res.get("onboarding.completed.headline.takeOffer"));
                createOfferLabel.setText(Res.get("onboarding.completed.headLine2.createOffer"));

                int numMatchingOffers = model.getMatchingOffers().size();
                if (numMatchingOffers > 0) {
                    tableView.setMaxHeight(42 + numMatchingOffers * 55);
                    VBox.setMargin(createOfferHBox, new Insets(10, 0, 0, 0));
                    // Aligned with take offer button
                    createOfferHBox.setPadding(new Insets(12));
                    if (numMatchingOffers == 3) {
                        VBox.setMargin(tableView, new Insets(-5, 0, 10, 0));
                        VBox.setMargin(headLineLabel, new Insets(-10, 0, 0, 0));
                    } else if (numMatchingOffers == 2) {
                        VBox.setMargin(tableView, new Insets(-5, 0, 30, 0));
                        VBox.setMargin(headLineLabel, new Insets(-10, 0, 0, 0));
                    } else if (numMatchingOffers == 1) {
                        VBox.setMargin(tableView, new Insets(-5, 0, 50, 0));
                        VBox.setMargin(headLineLabel, new Insets(-10, 0, 0, 0));
                    }
                } else {
                    VBox.setMargin(headLineLabel, new Insets(70, 0, 0, 0));
                    VBox.setMargin(subtitleLabel, new Insets(20, 0, 10, 0));
                    VBox.setMargin(createOfferHBox, new Insets(40, 0, 0, 0));
                }
            } else {
                createOfferHBox.setMinWidth(550);
                createOfferHBox.setMaxWidth(550);
                headLineLabel.setText(Res.get("createOffer"));

                VBox.setMargin(headLineLabel, new Insets(-100, 0, 0, 0));
                VBox.setMargin(createOfferHBox, new Insets(10, 0, 0, 0));
            }
        });

        showCreateOfferSuccessPin = EasyBind.subscribe(model.getShowCreateOfferSuccess(),
                show -> {
                    createOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(createOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
        showTakeOfferSuccessPin = EasyBind.subscribe(model.getShowTakeOfferSuccess(),
                show -> {
                    takeOfferSuccess.setVisible(show);
                    if (show) {
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(takeOfferSuccess, 450);
                    } else {
                        Transitions.removeEffect(content);
                    }
                });
    }

    @Override
    protected void onViewDetached() {
        if (true) return;
        viewOfferButton.setOnAction(null);
        openPrivateChannelButton.setOnAction(null);
        matchingOffersFoundPin.unsubscribe();
        showCreateOfferSuccessPin.unsubscribe();
        showTakeOfferSuccessPin.unsubscribe();
    }

    private void maybeConfigTableView() {
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
                                    item.getAuthorUserProfileId().ifPresent(userProfile ->
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
                .minWidth(200)
                .valueSupplier(ListItem::getAmountDisplayString)
                .comparator(Comparator.comparing(ListItem::getMaxAmountAsLong))
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
        VBox contentBox = getFeedbackContentBox();

        createOfferSuccess.setVisible(false);
        createOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.createOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        viewOfferButton.setDefaultButton(true);
        VBox.setMargin(viewOfferButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, viewOfferButton);
        createOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }


    private void configTakeOfferSuccess() {
        VBox contentBox = getFeedbackContentBox();

        takeOfferSuccess.setVisible(false);
        takeOfferSuccess.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.completed.takeOfferSuccess.subTitle"));
        configFeedbackSubtitleLabel(subtitleLabel);

        openPrivateChannelButton.setDefaultButton(true);
        VBox.setMargin(openPrivateChannelButton, new Insets(10, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, openPrivateChannelButton);
        takeOfferSuccess.getChildren().addAll(contentBox, Spacer.fillVBox());
    }

    private VBox getFeedbackContentBox() {
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("create-offer-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(FEEDBACK_WIDTH);
        return contentBox;
    }

    private void configFeedbackSubtitleLabel(Label subtitleLabel) {
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setMinHeight(100);
        subtitleLabel.getStyleClass().addAll("bisq-text-21", "wrap-text");
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    static class ListItem implements TableItem {
        private final BisqEasyPublicChatMessage chatMessage;
        private final Optional<UserProfile> authorUserProfileId;
        private final String userName;
        private final String amountDisplayString;
        private final long maxAmountAsLong;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;

        public ListItem(BisqEasyPublicChatMessage chatMessage, UserProfileService userProfileService, ReputationService reputationService) {
            this.chatMessage = chatMessage;
            authorUserProfileId = userProfileService.findUserProfile(chatMessage.getAuthorUserProfileId());
            userName = authorUserProfileId.map(UserProfile::getUserName).orElse("");
            BisqEasyOffer bisqEasyOffer = chatMessage.getBisqEasyOffer().orElseThrow();
            maxAmountAsLong = bisqEasyOffer.getQuoteSideMaxAmount().getValue();
            amountDisplayString = bisqEasyOffer.getQuoteSideAmountAsDisplayString();
            reputationScore = authorUserProfileId.flatMap(reputationService::findReputationScore)
                    .orElse(ReputationScore.NONE);
        }
    }
}
