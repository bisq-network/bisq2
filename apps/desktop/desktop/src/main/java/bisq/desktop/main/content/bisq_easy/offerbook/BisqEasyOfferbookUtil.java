package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class BisqEasyOfferbookUtil {
    private static final List<Market> majorMarkets = MarketRepository.getMajorMarkets();

    public static Comparator<MarketChannelItem> SortByNumMessages(Function<Market, Integer> getNumMessages) {
        return (lhs, rhs) -> Integer.compare(
                getNumMessages.apply(rhs.getMarket()),
                getNumMessages.apply(lhs.getMarket())
        );
    }

    public static Comparator<MarketChannelItem> SortByMajorMarkets() {
        return (lhs, rhs) -> {
            int index1 = majorMarkets.indexOf(lhs.getMarket());
            int index2 = majorMarkets.indexOf(rhs.getMarket());
            return Integer.compare(index1, index2);
        };
    }

    public static Comparator<MarketChannelItem> SortByMarketString() {
        return Comparator.comparing(MarketChannelItem::getMarketString);
    }
}
