package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.ReputationScoreDisplay;
import bisq.desktop.main.content.components.UserProfileIcon;
import bisq.i18n.Res;
import bisq.presentation.formatters.PercentageFormatter;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.Comparator;
import java.util.List;

@Slf4j
public class BisqEasyOfferbookUtil {
    static final List<Market> majorMarkets = MarketRepository.getMajorMarkets();

    static Comparator<MarketChannelItem> sortByNumOffers() {
        return (lhs, rhs) -> Integer.compare(rhs.getNumOffers().get(), lhs.getNumOffers().get());
    }

    static Comparator<MarketChannelItem> sortByMajorMarkets() {
        return (lhs, rhs) -> {
            int index1 = majorMarkets.indexOf(lhs.getMarket());
            int index2 = majorMarkets.indexOf(rhs.getMarket());
            return Integer.compare(index1, index2);
        };
    }

    static Comparator<MarketChannelItem> sortByMarketNameAsc() {
        return Comparator.comparing(MarketChannelItem::toString);
    }

    static Comparator<MarketChannelItem> sortByMarketNameDesc() {
        return Comparator.comparing(MarketChannelItem::toString).reversed();
    }

    static Comparator<MarketChannelItem> sortByMarketActivity() {
        return (lhs, rhs) -> BisqEasyOfferbookUtil.sortByNumOffers()
                .thenComparing(BisqEasyOfferbookUtil.sortByMajorMarkets())
                .thenComparing(BisqEasyOfferbookUtil.sortByMarketNameAsc())
                .compare(lhs, rhs);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // MARKETS' LIST
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLabelCellFactory(boolean isFavouritesTableView) {
        return column -> new TableCell<>() {
            private final Label marketName = new Label();
            private final Label marketCode = new Label();
            private final Label numOffers = new Label();
            private final Label favouriteLabel = new Label();
            private final ImageView star;
            private final HBox hBox = new HBox(10, marketCode, numOffers);
            private final VBox vBox = new VBox(0, marketName, hBox);
            private final HBox container = new HBox(0, vBox, Spacer.fillHBox(), favouriteLabel);
            private final Tooltip marketDetailsTooltip = new BisqTooltip();
            private final Tooltip favouriteTooltip = new BisqTooltip();
            private Subscription selectedPin;

            {
                setCursor(Cursor.HAND);
                marketName.getStyleClass().add("market-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
                vBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(vBox, marketDetailsTooltip);
                marketDetailsTooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");

                favouriteTooltip.textProperty().set(isFavouritesTableView
                        ? Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.removeFromFavourites")
                        : Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.addToFavourites"));
                favouriteTooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");
                star = ImageUtil.getImageViewById(isFavouritesTableView
                        ? "star-yellow"
                        : "star-grey-hollow");
                favouriteLabel.setGraphic(star);
                favouriteLabel.getStyleClass().add("favourite-label");
                Tooltip.install(favouriteLabel, favouriteTooltip);

                container.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);

                if (selectedPin != null) {
                    selectedPin.unsubscribe();
                }

                if (item != null && !empty) {
                    String quoteCurrencyDisplayName = item.getMarket().getQuoteCurrencyDisplayName();
                    marketName.setText(quoteCurrencyDisplayName);
                    marketCode.setText(item.getMarket().getQuoteCurrencyCode());
                    int numOffersString = item.getNumOffers().get();
                    StringExpression formattedNumOffers = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedOfferNumber(numOffersString), item.getNumOffers());
                    numOffers.textProperty().bind(formattedNumOffers);
                    StringExpression formattedTooltip = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedTooltip(numOffersString, quoteCurrencyDisplayName), item.getNumOffers());
                    marketDetailsTooltip.textProperty().bind(formattedTooltip);

                    TableRow<MarketChannelItem> tableRow = getTableRow();
                    if (tableRow != null) {
                        selectedPin = EasyBind.subscribe(tableRow.selectedProperty(), item::updateMarketLogoEffect);
                    }

                    favouriteLabel.setOnMouseClicked(e -> item.toggleFavourite());

                    setGraphic(container);
                } else {
                    numOffers.textProperty().unbind();
                    marketDetailsTooltip.textProperty().unbind();

                    favouriteLabel.setOnMouseClicked(null);

                    setGraphic(null);
                }
            }
        };
    }

    static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLogoCellFactory() {
        return column -> new TableCell<>() {
            private final Badge numMessagesBadge = new Badge(Pos.CENTER);

            {
                setCursor(Cursor.HAND);
                numMessagesBadge.getStyleClass().add("market-badge");
                numMessagesBadge.getLabel().setStyle("-fx-text-fill: -fx-dark-text-color !important; -fx-font-family: \"IBM Plex Sans SemiBold\";");
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    numMessagesBadge.textProperty().bind(item.getNumMarketNotifications());
                    Node marketLogo = item.getMarketLogo();
                    StackPane pane = new StackPane(marketLogo, numMessagesBadge);
                    StackPane.setMargin(numMessagesBadge, new Insets(33, 0, 0, 35));
                    setGraphic(pane);
                } else {
                    numMessagesBadge.textProperty().unbind();
                    numMessagesBadge.setText("");
                    setGraphic(null);
                }
            }
        };
    }

    private static String getFormattedOfferNumber(int numOffers) {
        if (numOffers == 0) {
            return "";
        }
        return String.format("(%s)",
                numOffers > 1
                        ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numOffers)
                        : Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numOffers)
        );
    }

    private static String getFormattedTooltip(int numOffers, String quoteCurrencyName) {
        if (numOffers == 0) {
            return Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none", quoteCurrencyName);
        }
        return numOffers > 1
                ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many", numOffers, quoteCurrencyName)
                : Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one", numOffers, quoteCurrencyName);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // OFFERS' LIST
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    static Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
            TableCell<OfferMessageItem, OfferMessageItem>> getOfferMessageUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private final Label userNameLabel = new Label();
            private final ReputationScoreDisplay reputationScoreDisplay = new ReputationScoreDisplay();
            private final VBox nameAndReputationBox = new VBox(userNameLabel, reputationScoreDisplay);
            private final UserProfileIcon userProfileIcon = new UserProfileIcon(30);
            private final HBox userProfileBox = new HBox(10, userProfileIcon, nameAndReputationBox);
            private Subscription reputationScorePin;

            {
                userNameLabel.setId("chat-user-name");
                HBox.setMargin(userProfileIcon, new Insets(0, 0, 0, -1));
                nameAndReputationBox.setAlignment(Pos.CENTER_LEFT);
                userProfileBox.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(OfferMessageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userNameLabel.setText(item.getUserNickname());
                    reputationScorePin = EasyBind.subscribe(item.getReputationScore(), reputationScoreDisplay::setReputationScore);
                    userProfileIcon.applyData(item.getUserProfile(), item.getLastSeenAsString(), item.getLastSeen());
                    setGraphic(userProfileBox);
                } else {
                    if (reputationScorePin != null) {
                        reputationScorePin.unsubscribe();
                    }
                    setGraphic(null);
                }
            }
        };
    }

    static Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
            TableCell<OfferMessageItem, OfferMessageItem>> getOfferMessagePriceCellFactory() {
        return column -> new TableCell<>() {
            private final Label priceSpecAsPercentLabel = new Label();

            @Override
            protected void updateItem(OfferMessageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    StringExpression priceSpecAsPercent = Bindings.createStringBinding(() ->
                            PercentageFormatter.formatToPercentWithSymbol(item.getPriceSpecAsPercent()));
                    priceSpecAsPercentLabel.textProperty().bind(priceSpecAsPercent);
                    setGraphic(priceSpecAsPercentLabel);
                } else {
                    priceSpecAsPercentLabel.textProperty().unbind();
                    setGraphic(null);
                }
            }
        };
    }

    static Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
            TableCell<OfferMessageItem, OfferMessageItem>> getOfferMessageFiatAmountCellFactory() {
        return column -> new TableCell<>() {
            private final Label fiatAmountLabel = new Label();

            @Override
            protected void updateItem(OfferMessageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    fiatAmountLabel.setText(item.getMinMaxAmountAsString());
                    setGraphic(fiatAmountLabel);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    static Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
            TableCell<OfferMessageItem, OfferMessageItem>> getOfferMessagePaymentCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(OfferMessageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    for (FiatPaymentMethod fiatPaymentMethod : item.getFiatPaymentMethods()) {
                        Label label = new Label();
                        label.setGraphic(ImageUtil.getImageViewById(fiatPaymentMethod.getName()));
                        BisqTooltip tooltip = new BisqTooltip();
                        tooltip.getStyleClass().add("medium-dark-tooltip");
                        tooltip.setText(fiatPaymentMethod.getDisplayString());
                        Tooltip.install(label, tooltip);
                        hbox.getChildren().add(label);
                    }
                    setGraphic(hbox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }

    static Callback<TableColumn<OfferMessageItem, OfferMessageItem>,
            TableCell<OfferMessageItem, OfferMessageItem>> getOfferMessageSettlementCellFactory() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(OfferMessageItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    for (BitcoinPaymentMethod bitcoinPaymentMethod : item.getBitcoinPaymentMethods()) {
                        Label label = new Label();
                        label.setGraphic(ImageUtil.getImageViewById(bitcoinPaymentMethod.getName()));
                        BisqTooltip tooltip = new BisqTooltip();
                        tooltip.getStyleClass().add("medium-dark-tooltip");
                        tooltip.setText(bitcoinPaymentMethod.getDisplayString());
                        Tooltip.install(label, tooltip);
                        hbox.getChildren().add(label);
                    }
                    setGraphic(hbox);
                } else {
                    setGraphic(null);
                }
            }
        };
    }
}
