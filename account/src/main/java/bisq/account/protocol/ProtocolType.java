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

package bisq.account.protocol;

import bisq.common.monetary.Market;
import bisq.common.encoding.Proto;

import java.util.ArrayList;
import java.util.List;

public interface ProtocolType extends Proto {

    static List<SwapProtocolType> getProtocols(Market market) {
        List<SwapProtocolType> result = new ArrayList<>();
        if (isBtcXmrSwapSupported(market)) {
            result.add(SwapProtocolType.BTC_XMR_SWAP);
        }
        if (isLiquidSwapSupported(market)) {
            result.add(SwapProtocolType.LIQUID_SWAP);
        }
        if (isBsqSwapSupported(market)) {
            result.add(SwapProtocolType.BSQ_SWAP);
        }
        if (isLNSwapSupported(market)) {
            result.add(SwapProtocolType.LN_SWAP);
        }
        if (isMultiSigSupported(market)) {
            result.add(SwapProtocolType.MULTISIG);
        }
        if (isBsqBondSupported(market)) {
            result.add(SwapProtocolType.BSQ_BOND);
        }
        if (isReputationSupported(market)) {
            result.add(SwapProtocolType.REPUTATION);
        }
        return result;
    }

    private static boolean isBtcXmrSwapSupported(Market market) {
        Market pair1 = new Market("BTC", "XMR");
        Market pair2 = new Market("XMR", "BTC");
        return market.equals(pair1) || market.equals(pair2);
    }

    private static boolean isLiquidSwapSupported(Market market) {
        //todo we need a asset repository to check if any asset is a liquid asset
        return (market.baseCurrencyCode().equals("L-BTC") ||
                market.quoteCurrencyCode().equals("L-BTC"));
    }

    private static boolean isBsqSwapSupported(Market market) {
        Market pair1 = new Market("BTC", "BSQ");
        Market pair2 = new Market("BSQ", "BTC");
        return market.equals(pair1) || market.equals(pair2);
    }

    private static boolean isLNSwapSupported(Market market) {
        return false;//todo need some liquid asset lookup table
    }

    private static boolean isMultiSigSupported(Market market) {
        return market.quoteCurrencyCode().equals("BTC") || market.baseCurrencyCode().equals("BTC");
    }

    private static boolean isBsqBondSupported(Market market) {
        return true;
    }

    private static boolean isReputationSupported(Market market) {
        return true;
    }
}
