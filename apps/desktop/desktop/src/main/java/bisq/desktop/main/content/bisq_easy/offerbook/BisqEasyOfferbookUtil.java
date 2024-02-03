package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringExpression;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.util.Comparator;
import java.util.List;

public class BisqEasyOfferbookUtil {
    private static final List<Market> majorMarkets = MarketRepository.getMajorMarkets();

    public static Comparator<MarketChannelItem> SortByNumOffers() {
        return (lhs, rhs) -> Integer.compare(rhs.getNumOffers().get(), lhs.getNumOffers().get());
    }

    public static Comparator<MarketChannelItem> SortByMajorMarkets() {
        return (lhs, rhs) -> {
            int index1 = majorMarkets.indexOf(lhs.getMarket());
            int index2 = majorMarkets.indexOf(rhs.getMarket());
            return Integer.compare(index1, index2);
        };
    }

    public static Comparator<MarketChannelItem> SortByMarketNameAsc() {
        return Comparator.comparing(MarketChannelItem::getMarketString);
    }

    public static Comparator<MarketChannelItem> SortByMarketNameDesc() {
        return Comparator.comparing(MarketChannelItem::getMarketString).reversed();
    }

    public static Comparator<MarketChannelItem> SortByMarketActivity() {
        return (lhs, rhs) -> BisqEasyOfferbookUtil.SortByNumOffers()
                .thenComparing(BisqEasyOfferbookUtil.SortByMajorMarkets())
                .thenComparing(BisqEasyOfferbookUtil.SortByMarketNameAsc())
                .compare(lhs, rhs);
    }

    public static String getFormattedOfferNumber(int numOffers) {
        if (numOffers == 0) {
            return "";
        }
        return String.format("(%s)",
                numOffers > 1
                        ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.many", numOffers)
                        : Res.get("bisqEasy.offerbook.marketListCell.numOffers.one", numOffers)
        );
    }

    public static String getFormattedTooltip(int numOffers, String quoteCurrencyName) {
        if (numOffers == 0) {
            return Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.none", quoteCurrencyName);
        }
        return numOffers > 1
                ? Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.many", numOffers, quoteCurrencyName)
                : Res.get("bisqEasy.offerbook.marketListCell.numOffers.tooltip.one", numOffers, quoteCurrencyName);
    }

    public static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLabelCellFactory() {
        return column -> new TableCell<>() {
            private final Label market = new Label();
            private final Label numOffers = new Label();
            private final HBox hBox = new HBox(10, market, numOffers);
            private final Tooltip tooltip = new BisqTooltip();

            {
                setCursor(Cursor.HAND);
                hBox.setPadding(new Insets(10));
                hBox.setAlignment(Pos.CENTER_LEFT);
                Tooltip.install(hBox, tooltip);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    market.setText(item.getMarket().getQuoteCurrencyCode());
                    StringExpression formattedNumOffers = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedOfferNumber(item.getNumOffers().get()), item.getNumOffers());
                    numOffers.textProperty().bind(formattedNumOffers);
                    StringExpression formattedTooltip = Bindings.createStringBinding(() ->
                            BisqEasyOfferbookUtil.getFormattedTooltip(item.getNumOffers().get(), item.getMarket().getQuoteCurrencyName()), item.getNumOffers());
                    tooltip.textProperty().bind(formattedTooltip);
                    tooltip.setStyle("-fx-text-fill: -fx-dark-text-color;");

                    setGraphic(hBox);
                } else {
                    numOffers.textProperty().unbind();
                    tooltip.textProperty().unbind();

                    setGraphic(null);
                }
            }
        };
    }

    public static Callback<TableColumn<MarketChannelItem, MarketChannelItem>,
            TableCell<MarketChannelItem, MarketChannelItem>> getMarketLogoCellFactory() {
        return column -> new TableCell<>() {

            {
                setCursor(Cursor.HAND);
            }

            @Override
            protected void updateItem(MarketChannelItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setGraphic(item.getIcon());
                } else {
                    setGraphic(null);
                }
            }
        };
    }
}
