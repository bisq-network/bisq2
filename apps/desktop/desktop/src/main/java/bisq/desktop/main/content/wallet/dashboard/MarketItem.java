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

package bisq.desktop.main.content.wallet.dashboard;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Getter
public class MarketItem {
    @EqualsAndHashCode.Include
    private final Market market;
    private final MarketPriceService marketPriceService;
    private final SimpleStringProperty formattedConvertedAmount;
    private final String amountCode;
    private final SimpleBooleanProperty isSelected;

    MarketItem(Market market, MarketPriceService marketPriceService) {
        this.market = market;
        this.marketPriceService = marketPriceService;
        formattedConvertedAmount = new SimpleStringProperty("N/A");
        amountCode = WalletMarketUtil.getMarketCode(market);
        isSelected = new SimpleBooleanProperty(false);
    }

    void updateFormattedAmount(Coin btcBalance) {
        if (btcBalance == null) {
            getFormattedConvertedAmount().set("N/A");
            return;
        }

        Optional<MarketPrice> optionalMarketPrice = marketPriceService.findMarketPrice(market);
        if (optionalMarketPrice.isEmpty()) {
            getFormattedConvertedAmount().set("N/A");
            return;
        }

        String amount = WalletMarketUtil.getFormattedConvertedAmount(btcBalance, optionalMarketPrice.get(), true);
        getFormattedConvertedAmount().set(amount);
    }

    @Override
    public String toString() {
        return market.toString();
    }
}
