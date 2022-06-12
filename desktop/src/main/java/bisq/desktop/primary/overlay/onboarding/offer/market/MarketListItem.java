package bisq.desktop.primary.overlay.onboarding.offer.market;

import bisq.desktop.components.table.TableItem;
import lombok.Getter;

@Getter
public class MarketListItem implements TableItem {
    private final String baseCurrencyCode;
    private final String quoteCurrencyCode;
    private final String offersCount;
    private final String onlinePeersCount;

    public MarketListItem(String baseCurrencyCode, String quoteCurrencyCode, String offersCount, String onlinePeersCount) {
        this.baseCurrencyCode = baseCurrencyCode;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.offersCount = offersCount;
        this.onlinePeersCount = onlinePeersCount;
    }
    
    public String getName () {
        return baseCurrencyCode + "/" + quoteCurrencyCode;
    }
}
