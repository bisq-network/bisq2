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

package bisq.trade.mu_sig.messages.network.handler;

import bisq.common.currency.Market;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MusSigFeeRateProvider;

public class NonceSharesRequestUtil {
    public static long getTradeAmount(MuSigTrade trade) {
        Market market = trade.getMarket();
        long tradeAmount;
        if (market.getBaseCurrencyCode().equals("BTC")) {
            return trade.getContract().getBaseSideAmount();
        } else if (market.getQuoteCurrencyCode().equals("BTC")) {
            return trade.getContract().getQuoteSideAmount();
        } else {
            throw new UnsupportedOperationException("The extended protocol version without Bitcoin on any market leg is not yet supported");
        }
    }

    public static long getDepositTxFeeRate() {
        return MusSigFeeRateProvider.getDepositTxFeeRate();
    }

    public static long getPreparedTxFeeRate() {
        return MusSigFeeRateProvider.getPreparedTxFeeRate();
    }

    public static long getBuyerSecurityDeposit() {
        // TODO get from CollateralOptions
        return 30_000;
    }

    public static long getSellersSecurityDeposit() {
        // TODO get from CollateralOptions
        return 30_000;
    }
}
