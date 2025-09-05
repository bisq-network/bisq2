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

import bisq.common.market.Market;
import bisq.trade.mu_sig.MuSigFeeRateProvider;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.protobuf.ReceiverAddressAndAmount;

public class NonceSharesRequestUtil {
    public static long getTradeAmount(MuSigTrade trade) {
        Market market = trade.getMarket();
        if (market.getBaseCurrencyCode().equals("BTC")) {
            return trade.getContract().getBaseSideAmount();
        } else if (market.getQuoteCurrencyCode().equals("BTC")) {
            return trade.getContract().getQuoteSideAmount();
        } else {
            throw new UnsupportedOperationException("The extended protocol version without Bitcoin on any market leg is not yet supported");
        }
    }

    public static long getDepositTxFeeRate() {
        return MuSigFeeRateProvider.getDepositTxFeeRate();
    }

    public static long getPreparedTxFeeRate() {
        return MuSigFeeRateProvider.getPreparedTxFeeRate();
    }

    public static long getBuyerSecurityDeposit() {
        // TODO get from CollateralOptions
        return 30_000;
    }

    public static long getSellersSecurityDeposit() {
        // TODO get from CollateralOptions
        return 30_000;
    }

    public static ReceiverAddressAndAmount getTradeFeeReceiver() {
        // NOTE: As implemented right now on the Rust side, the seller pays the entire trade fee (including the small
        //  amount needed to cover the increase in the mining fee from the optional trade fee output). In order to split
        //  the trade fee differently, the specified _buyer_ security deposit (but not the specified seller security
        //  deposit, which may be different) can be adjusted upwards by X sats, and the trade amount can be adjusted
        //  downwards by X sats. This has the effect of transferring an extra X sats from the buyer to the seller at the
        //  start of the trade. Thus, the Rust server would be using adjusted trade params (amount + security deposits)
        //  that are slightly different to those presented to the user on the Java side.

        // TODO: Determine the correct amount and pick address non-uniformly at random from oracle-provided BM list.
        //noinspection SpellCheckingInspection
        return ReceiverAddressAndAmount.newBuilder()
                .setAddress("bcrt1qwk6p86mzqmstcsg99qlu2mhsp3766u68jktv6k")
                .setAmount(5_000)
                .build();
    }
}
