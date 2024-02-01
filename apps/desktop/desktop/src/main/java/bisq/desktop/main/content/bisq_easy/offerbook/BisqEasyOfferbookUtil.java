package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.common.currency.Market;
import bisq.common.currency.MarketRepository;

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
}
