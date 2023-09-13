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

package bisq.desktop.main.content.bisq_easy.trade_wizard.take_offer;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.data.Pair;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.robohash.RoboHash;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.OfferPriceFormatter;
import bisq.offer.price.PriceUtil;
import bisq.presentation.formatters.AmountFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
class TradeWizardTakeOfferView extends View<VBox, TradeWizardTakeOfferModel, TradeWizardTakeOfferController> {
    private final static int TABLE_WIDTH = 800;
    private final HBox noMatchingOffersBox;

    private final BisqTableView<ListItem> tableView;
    private final Label headLineLabel, subtitleLabel;
    private Button goBackButton, browseOfferbookButton;

    TradeWizardTakeOfferView(TradeWizardTakeOfferModel model, TradeWizardTakeOfferController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.CENTER);

        headLineLabel = new Label();
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-3");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("bisq-easy-trade-wizard-take-offer-table-view");
        tableView.setMinWidth(TABLE_WIDTH);
        tableView.setMaxWidth(tableView.getMinWidth());
        tableView.setMaxHeight(206);

        noMatchingOffersBox = new HBox(25);

        VBox.setMargin(noMatchingOffersBox, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, tableView, noMatchingOffersBox, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        boolean showOffers = model.getShowOffers().get();

        tableView.setVisible(showOffers);
        tableView.setManaged(showOffers);
        noMatchingOffersBox.setVisible(!showOffers);
        noMatchingOffersBox.setManaged(!showOffers);
        if (showOffers) {
            VBox.setMargin(headLineLabel, new Insets(-30, 0, 0, 0));
            maybeConfigTableView();
            tableView.getSelectionModel().select(model.getSelectedItem());
        } else {
            VBox.setMargin(headLineLabel, new Insets(0, 0, 0, 0));
            if (noMatchingOffersBox.getChildren().isEmpty()) {
                Pair<VBox, Button> goBackPair = getBoxPair(Res.get("bisqEasy.tradeWizard.takeOffer.noMatchingOffers.goBack"),
                        Res.get("bisqEasy.tradeWizard.takeOffer.noMatchingOffers.goBack.info"),
                        "outlined-button");
                VBox goBackBox = goBackPair.getFirst();
                goBackButton = goBackPair.getSecond();

                Pair<VBox, Button> browseOfferbookPair = getBoxPair(Res.get("bisqEasy.tradeWizard.takeOffer.noMatchingOffers.browseOfferbook"),
                        Res.get("bisqEasy.tradeWizard.takeOffer.noMatchingOffers.browseOfferbook.info"),
                        "grey-transparent-outlined-button");
                VBox browseOfferbookBox = browseOfferbookPair.getFirst();
                browseOfferbookButton = browseOfferbookPair.getSecond();

                noMatchingOffersBox.getChildren().addAll(goBackBox, browseOfferbookBox);
                noMatchingOffersBox.setAlignment(Pos.CENTER);
            }

            goBackButton.setOnAction(e -> controller.onGoBack());
            browseOfferbookButton.setOnAction(e -> controller.onOpenOfferbook());
        }

        headLineLabel.setText(model.getHeadLine());
        subtitleLabel.setText(model.getSubHeadLine());
    }

    @Override
    protected void onViewDetached() {
        if (goBackButton != null) {
            goBackButton.setOnAction(null);
        }
        if (browseOfferbookButton != null) {
            browseOfferbookButton.setOnAction(null);
        }
    }

    private Pair<VBox, Button> getBoxPair(String title, String info, String style) {
        Button button = new Button(title);
        button.getStyleClass().addAll(style, "bisq-easy-trade-wizard-direction-button");
        button.setAlignment(Pos.CENTER);
        int width = 235;
        button.setMinWidth(width);
        button.setMinHeight(112);

        Label infoLabel = new Label(info);
        infoLabel.getStyleClass().add("bisq-text-3");
        infoLabel.setMaxWidth(width);
        infoLabel.setWrapText(true);
        infoLabel.setTextAlignment(TextAlignment.CENTER);
        infoLabel.setAlignment(Pos.CENTER);

        VBox vBox = new VBox(8, button, infoLabel);
        vBox.setAlignment(Pos.CENTER);

        return new Pair<>(vBox, button);
    }

    private void maybeConfigTableView() {
        if (!tableView.getColumns().isEmpty()) {
            return;
        }

        // Maker 
        String peer = model.getDirection() == Direction.BUY ?
                Res.get("offer.seller") :
                Res.get("offer.buyer");
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(peer)
                .left()
                .minWidth(100)
                .setCellFactory(getMakerCellFactory())
                .comparator(Comparator.comparing(ListItem::getMakerUserName))
                .build());

        // Reputation
        BisqTableColumn<ListItem> reputationColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.createOffer.review.table.reputation"))
                .minWidth(120)
                .setCellFactory(getReputationCellFactory())
                .comparator(Comparator.comparing(ListItem::getReputationScore).reversed())
                .build();
        tableView.getColumns().add(reputationColumn);
        tableView.getSortOrder().add(reputationColumn);

        // Price
        if (model.getDirection().isBuy()) {
            tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("bisqEasy.createOffer.review.table.price", model.getMarket().getMarketCodes()))
                    .minWidth(160)
                    .valueSupplier(ListItem::getPriceDisplayString)
                    .comparator(Comparator.comparing(ListItem::getPriceAsLong))
                    .build());
        }

        // BTC amount
        String baseAmountTitle = model.getDirection().isBuy() ?
                Res.get("bisqEasy.createOffer.review.table.baseAmount.buyer") :
                Res.get("bisqEasy.createOffer.review.table.baseAmount.seller");
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(baseAmountTitle)
                .minWidth(160)
                .valueSupplier(ListItem::getBaseAmountDisplayString)
                .comparator(Comparator.comparing(ListItem::getBaseAmountAsLong))
                .build());

        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .minWidth(170)
                .isSortable(false)
                .setCellFactory(getSelectButtonCellFactory())
                .right()
                .build());
    }


    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getMakerCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<ListItem, ListItem> call(TableColumn<ListItem, ListItem> column) {
                return new TableCell<>() {
                    private final Label userName = new Label();
                    private final ImageView roboIcon = new ImageView();
                    private final HBox hBox;

                    {
                        userName.setId("chat-user-name");
                        int size = 20;
                        roboIcon.setFitWidth(size);
                        roboIcon.setFitHeight(size);
                        StackPane roboIconWithRing = ImageUtil.addRingToNode(roboIcon, size, 1.5, "-bisq-grey-5");
                        hBox = new HBox(10, roboIconWithRing, userName);
                        hBox.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    public void updateItem(final ListItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            userName.setText(item.getMakerUserName());
                            item.getAuthorUserProfile().ifPresent(userProfile ->
                                    roboIcon.setImage(RoboHash.getImage(userProfile.getPubKeyHash())));
                            setGraphic(hBox);
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getReputationCellFactory() {
        return new Callback<>() {
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
        };
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getSelectButtonCellFactory() {
        return column -> new TableCell<>() {

            private final Button button = new Button(Res.get("bisqEasy.tradeWizard.takeOffer.table.select"));
            private TableRow<ListItem> tableRow;
            private Subscription selectedItemPin;

            {
                button.setMinWidth(160);
                button.setMaxWidth(160);
            }

            @Override
            public void updateItem(final ListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    button.setOnAction(e -> {
                        tableView.getSelectionModel().select(item);
                        controller.onSelect(item);
                    });

                    tableRow = getTableRow();
                    tableRow.setOnMouseEntered(e -> {
                        if (!tableRow.isSelected()) {
                            button.setVisible(true);
                            button.getStyleClass().remove("white-button");
                            button.getStyleClass().add("outlined-button");
                        }
                    });
                    tableRow.setOnMouseExited(e -> {
                        button.getStyleClass().remove("outlined-button");
                        if (!tableRow.isSelected()) {
                            button.setVisible(tableView.getSelectionModel().getSelectedItem() == null);
                            button.getStyleClass().remove("white-button");
                        }
                    });
                    tableRow.setOnMouseClicked(e -> controller.onSelectRow(item));

                    selectedItemPin = EasyBind.subscribe(tableView.getSelectionModel().selectedItemProperty(),
                            selectedItem -> {
                                if (item.equals(selectedItem)) {
                                    button.setVisible(true);
                                    button.getStyleClass().remove("outlined-button");
                                    button.getStyleClass().add("white-button");
                                    button.setText(Res.get("bisqEasy.tradeWizard.takeOffer.table.takeOffer"));
                                } else {
                                    button.setVisible(selectedItem == null);
                                    button.getStyleClass().remove("white-button");
                                    button.setText(Res.get("bisqEasy.tradeWizard.takeOffer.table.select"));
                                }
                            });

                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    if (tableRow != null) {
                        tableRow.setOnMouseEntered(null);
                        tableRow.setOnMouseExited(null);
                        tableRow.setOnMouseClicked(null);
                        tableRow = null;
                    }
                    if (selectedItemPin != null) {
                        selectedItemPin.unsubscribe();
                        selectedItemPin = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    static class ListItem implements TableItem {
        private final Optional<UserProfile> authorUserProfile;
        private final String makerUserName, baseAmountDisplayString, priceDisplayString;
        private final long priceAsLong, baseAmountAsLong;
        @EqualsAndHashCode.Exclude
        private final ReputationScore reputationScore;
        private final BisqEasyOffer bisqEasyOffer;

        public ListItem(BisqEasyOffer bisqEasyOffer,
                        TradeWizardTakeOfferModel model,
                        UserProfileService userProfileService,
                        ReputationService reputationService,
                        MarketPriceService marketPriceService) {
            this.bisqEasyOffer = bisqEasyOffer;
            authorUserProfile = userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId());
            makerUserName = authorUserProfile.map(UserProfile::getUserName).orElse("");
            priceAsLong = PriceUtil.findQuote(marketPriceService, bisqEasyOffer).map(PriceQuote::getValue).orElse(0L);
            priceDisplayString = OfferPriceFormatter.formatQuote(marketPriceService, bisqEasyOffer, false);
            Monetary baseAmountAsMonetary = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService,
                            model.getAmountSpec(),
                            bisqEasyOffer.getPriceSpec(),
                            bisqEasyOffer.getMarket())
                    .orElse(Monetary.from(0, model.getMarket().getBaseCurrencyCode()));
            baseAmountAsLong = baseAmountAsMonetary.getValue();
            baseAmountDisplayString = AmountFormatter.formatAmountWithCode(baseAmountAsMonetary, false);
            reputationScore = authorUserProfile.flatMap(reputationService::findReputationScore)
                    .orElse(ReputationScore.NONE);
        }
    }
}
