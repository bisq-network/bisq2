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

package bisq.desktop.main.content.bisq_easy.trade_wizard.select_offer;

import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.data.Pair;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.desktop.common.view.View;
import bisq.desktop.components.cathash.CatHash;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
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
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.Optional;

@Slf4j
class TradeWizardSelectOfferView extends View<VBox, TradeWizardSelectOfferModel, TradeWizardSelectOfferController> {
    private static final int TABLE_WIDTH = 800;

    private final HBox noMatchingOffersBox;
    private final BisqTableView<ListItem> tableView;
    private final Label headlineLabel, subtitleLabel;
    private final VBox tableContainer;
    private Button goBackButton, browseOfferbookButton;
    private boolean isTableViewConfigured;

    TradeWizardSelectOfferView(TradeWizardSelectOfferModel model, TradeWizardSelectOfferController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-3");

        tableView = new BisqTableView<>(model.getSortedList());
        tableView.getStyleClass().add("bisq-easy-trade-wizard-select-offer");
        tableView.setMinWidth(TABLE_WIDTH);
        // fits 4 rows
        tableView.setMaxHeight(260); // 4 * 55 (row height) + 40 (header height)

        tableContainer = new VBox(tableView);
        tableContainer.getStyleClass().add("matching-offers-table-container");

        noMatchingOffersBox = new HBox(25);

        VBox.setMargin(noMatchingOffersBox, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, subtitleLabel, tableContainer, noMatchingOffersBox, Spacer.fillVBox());
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
        boolean showOffers = model.getShowOffers().get();

        tableContainer.setVisible(showOffers);
        tableContainer.setManaged(showOffers);
        noMatchingOffersBox.setVisible(!showOffers);
        noMatchingOffersBox.setManaged(!showOffers);
        if (showOffers) {
            VBox.setMargin(headlineLabel, new Insets(-30, 0, 0, 0));
            maybeConfigTableView();
            tableView.getSelectionModel().select(model.getSelectedItem());
        } else {
            VBox.setMargin(headlineLabel, new Insets(0, 0, 0, 0));
            if (noMatchingOffersBox.getChildren().isEmpty()) {
                Pair<VBox, Button> goBackPair = getBoxPair(Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.goBack"),
                        Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.goBack.info"));
                VBox goBackBox = goBackPair.getFirst();
                goBackButton = goBackPair.getSecond();
                goBackButton.setDefaultButton(true);

                Pair<VBox, Button> browseOfferbookPair = getBoxPair(Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.browseOfferbook"),
                        Res.get("bisqEasy.tradeWizard.selectOffer.noMatchingOffers.browseOfferbook.info"));
                VBox browseOfferbookBox = browseOfferbookPair.getFirst();
                browseOfferbookButton = browseOfferbookPair.getSecond();

                noMatchingOffersBox.getChildren().addAll(goBackBox, browseOfferbookBox);
                noMatchingOffersBox.setAlignment(Pos.CENTER);
            }

            goBackButton.setOnAction(e -> controller.onGoBack());
            browseOfferbookButton.setOnAction(e -> controller.onOpenOfferbook());
        }

        headlineLabel.setText(model.getHeadline());
        subtitleLabel.setText(model.getSubHeadLine());
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
        if (goBackButton != null) {
            goBackButton.setOnAction(null);
        }
        if (browseOfferbookButton != null) {
            browseOfferbookButton.setOnAction(null);
        }
    }

    private Pair<VBox, Button> getBoxPair(String title, String info) {
        Button button = new Button(title);
        button.setAlignment(Pos.CENTER);
        button.getStyleClass().addAll("card-button", "card-button-border");
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
        if (isTableViewConfigured) {
            return;
        }

        // Selection marker
        tableView.getColumns().add(tableView.getSelectionMarkerColumn());

        // Maker 
        String peer = model.getDirection() == Direction.BUY ? Res.get("offer.seller") : Res.get("offer.buyer");
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(peer)
                .left()
                .minWidth(100)
                .setCellFactory(getMakerCellFactory())
                .comparator(Comparator.comparing(ListItem::getMakerUserName))
                .build());

        // Reputation
        BisqTableColumn<ListItem> reputationColumn = new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("bisqEasy.tradeWizard.review.table.reputation"))
                .minWidth(120)
                .setCellFactory(getReputationCellFactory())
                .comparator(Comparator.comparing(ListItem::getReputationScore))
                .sortType(TableColumn.SortType.DESCENDING)
                .build();
        tableView.getColumns().add(reputationColumn);
        tableView.getSortOrder().add(reputationColumn);

        // Price
        Comparator<ListItem> comparator = (o1, o2) -> {
            if (o1.getBisqEasyOffer().getDirection().isSell()) {
                return Long.compare(o1.getPriceAsLong(), o2.getPriceAsLong());
            } else {
                return Long.compare(o2.getPriceAsLong(), o1.getPriceAsLong());
            }
        };
        if (model.getDirection().isBuy()) {
            BisqTableColumn<ListItem> priceColumn = new BisqTableColumn.Builder<ListItem>()
                    .title(Res.get("bisqEasy.tradeWizard.review.table.price", model.getMarket().getMarketCodes()))
                    .minWidth(160)
                    .valueSupplier(ListItem::getPriceDisplayString)
                    .comparator(comparator)
                    .build();
            tableView.getColumns().add(priceColumn);
            tableView.getSortOrder().add(priceColumn);
        }

        // BTC amount
        String baseAmountTitle = model.getDirection().isBuy()
                ? Res.get("bisqEasy.tradeWizard.review.table.baseAmount.buyer")
                : Res.get("bisqEasy.tradeWizard.review.table.baseAmount.seller");
        tableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(baseAmountTitle)
                .minWidth(160)
                .valueSupplier(ListItem::getBaseAmountDisplayString)
                .comparator(Comparator.comparing(ListItem::getBaseAmountAsLong))
                .build());

        isTableViewConfigured = true;
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getMakerCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<ListItem, ListItem> call(TableColumn<ListItem, ListItem> column) {
                return new TableCell<>() {
                    private final Label userName = new Label();
                    private final ImageView catHashImageView = new ImageView();
                    private final HBox hBox = new HBox(10, catHashImageView, userName);

                    {
                        userName.setId("chat-user-name");
                        hBox.setAlignment(Pos.CENTER_LEFT);
                        catHashImageView.setFitWidth(40);
                        catHashImageView.setFitHeight(catHashImageView.getFitWidth());
                        HBox.setMargin(catHashImageView, new Insets(0, 0, 0, 5));
                    }

                    @Override
                    public void updateItem(final ListItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            userName.setText(item.getMakerUserName());
                            item.getAuthorUserProfile().ifPresent(userProfile ->
                                    catHashImageView.setImage(CatHash.getImage(userProfile)));
                            setGraphic(hBox);
                        } else {
                            catHashImageView.setImage(null);
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
                    private TableRow<ListItem> tableRow;

                    {
                        reputationScoreDisplay.setAlignment(Pos.CENTER);
                    }

                    @Override
                    public void updateItem(final ListItem item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            tableRow = getTableRow();
                            tableRow.setOnMouseClicked(e -> controller.onSelectRow(item));
                            reputationScoreDisplay.setReputationScore(item.getReputationScore());
                            setGraphic(reputationScoreDisplay);
                        } else {
                            if (tableRow != null) {
                                tableRow.setOnMouseClicked(null);
                                tableRow = null;
                            }
                            setGraphic(null);
                        }
                    }
                };
            }
        };
    }

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class ListItem {
        @EqualsAndHashCode.Include
        private final BisqEasyOffer bisqEasyOffer;

        private final Optional<UserProfile> authorUserProfile;
        private final String makerUserName, baseAmountDisplayString, priceDisplayString;
        private final long priceAsLong, baseAmountAsLong;
        private final ReputationScore reputationScore;

        public ListItem(BisqEasyOffer bisqEasyOffer,
                        TradeWizardSelectOfferModel model,
                        UserProfileService userProfileService,
                        ReputationService reputationService,
                        MarketPriceService marketPriceService) {
            this.bisqEasyOffer = bisqEasyOffer;

            authorUserProfile = userProfileService.findUserProfile(bisqEasyOffer.getMakersUserProfileId());
            makerUserName = authorUserProfile.map(UserProfile::getUserName).orElse("");
            priceAsLong = PriceUtil.findQuote(marketPriceService, bisqEasyOffer).map(PriceQuote::getValue).orElse(0L);
            priceDisplayString = OfferPriceFormatter.formatQuote(marketPriceService, bisqEasyOffer, false);
            Monetary baseAmountAsMonetary = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService,
                            model.getQuoteSideAmountSpec(),
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
