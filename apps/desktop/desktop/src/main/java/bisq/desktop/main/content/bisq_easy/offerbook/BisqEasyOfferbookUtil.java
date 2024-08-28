package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.Badge;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.main.content.components.MarketImageComposition;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
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
            private final HBox hBox = new HBox(10, marketCode, numOffers);
            private final VBox vBox = new VBox(0, marketName, hBox);
            private final HBox container = new HBox(0, vBox, Spacer.fillHBox(), favouriteLabel);
            private final Tooltip marketDetailsTooltip = new BisqTooltip();
            private final Tooltip favouriteTooltip = new BisqTooltip();
            private StringBinding formattedNumOffersBindings;
            private StringBinding formattedTooltipBinding;

            {
                setCursor(Cursor.HAND);
                marketName.getStyleClass().add("market-name");
                hBox.setAlignment(Pos.CENTER_LEFT);
                vBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(vBox, marketDetailsTooltip);

                favouriteTooltip.textProperty().set(isFavouritesTableView
                        ? Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.removeFromFavourites")
                        : Res.get("bisqEasy.offerbook.marketListCell.favourites.tooltip.addToFavourites"));
                ImageView star = ImageUtil.getImageViewById(isFavouritesTableView
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

                if (item != null && !empty) {
                    String quoteCurrencyDisplayName = item.getMarket().getQuoteCurrencyDisplayName();
                    marketName.setText(quoteCurrencyDisplayName);
                    marketCode.setText(item.getMarket().getQuoteCurrencyCode());
                    int numOffersString = item.getNumOffers().get();
                    formattedNumOffersBindings = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedOfferNumber(numOffersString), item.getNumOffers());
                    numOffers.textProperty().bind(formattedNumOffersBindings);
                    formattedTooltipBinding = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedTooltip(numOffersString, quoteCurrencyDisplayName), item.getNumOffers());
                    marketDetailsTooltip.textProperty().bind(formattedTooltipBinding);

                    favouriteLabel.setOnMouseClicked(e -> item.toggleFavourite());

                    setGraphic(container);
                } else {
                    numOffers.textProperty().unbind();
                    marketDetailsTooltip.textProperty().unbind();
                    formattedNumOffersBindings.dispose();
                    formattedNumOffersBindings = null;
                    formattedTooltipBinding.dispose();
                    formattedTooltipBinding = null;
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
            private Subscription selectedPin;

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

                    Node marketLogo = MarketImageComposition.createMarketLogo(item.getMarket().getQuoteCurrencyCode());
                    marketLogo.setCache(true);
                    marketLogo.setCacheHint(CacheHint.SPEED);
                    marketLogo.setEffect(MarketChannelItem.DIMMED);

                    TableRow<MarketChannelItem> tableRow = getTableRow();
                    if (tableRow != null) {
                        selectedPin = EasyBind.subscribe(tableRow.selectedProperty(), isSelectedMarket -> {
                            marketLogo.setEffect(isSelectedMarket ? MarketChannelItem.SELECTED : MarketChannelItem.DIMMED);
                        });
                    }

                    StackPane pane = new StackPane(marketLogo, numMessagesBadge);
                    StackPane.setMargin(numMessagesBadge, new Insets(33, 0, 0, 35));
                    setGraphic(pane);
                } else {
                    numMessagesBadge.textProperty().unbind();
                    numMessagesBadge.setText("");
                    if (selectedPin != null) {
                        selectedPin.unsubscribe();
                        selectedPin = null;
                    }
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
}
