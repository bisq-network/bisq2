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

package bisq.account.protocol_type;

import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.common.proto.ProtoEnum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public interface ProtocolType extends ProtoEnum {

    static List<SwapProtocolType> getProtocols(Market market) {
        List<SwapProtocolType> result = new ArrayList<>();
        if (isBisqEasySupported(market)) {
            result.add(SwapProtocolType.BISQ_EASY);
        }
        if (isBtcXmrSwapSupported(market)) {
            result.add(SwapProtocolType.MONERO_SWAP);
        }
        if (isLiquidSwapSupported(market)) {
            result.add(SwapProtocolType.LIQUID_SWAP);
        }
        if (isBsqSwapSupported(market)) {
            result.add(SwapProtocolType.BSQ_SWAP);
        }
        if (isLNSwapSupported(market)) {
            result.add(SwapProtocolType.LIGHTNING_X);
        }
        if (isMultiSigSupported(market)) {
            result.add(SwapProtocolType.BISQ_MULTISIG);
        }

        result.sort(Comparator.comparingInt(SwapProtocolType::ordinal));
        return result;
    }

    private static boolean isBisqEasySupported(Market market) {
        String baseCurrencyCode = market.getBaseCurrencyCode();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        return (baseCurrencyCode.equals("BTC") && TradeCurrency.isFiat(quoteCurrencyCode));
    }

    private static boolean isBtcXmrSwapSupported(Market market) {
        String baseCurrencyCode = market.getBaseCurrencyCode();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        return (baseCurrencyCode.equals("BTC") && quoteCurrencyCode.equals("XMR")) ||
                (quoteCurrencyCode.equals("BTC") && baseCurrencyCode.equals("XMR"));
    }

    private static boolean isLiquidSwapSupported(Market market) {
        //todo we need a asset repository to check if any asset is a liquid asset
        return (market.getBaseCurrencyCode().equals("L-BTC") ||
                market.getQuoteCurrencyCode().equals("L-BTC"));
    }

    private static boolean isBsqSwapSupported(Market market) {
        String baseCurrencyCode = market.getBaseCurrencyCode();
        String quoteCurrencyCode = market.getQuoteCurrencyCode();
        return (baseCurrencyCode.equals("BTC") && quoteCurrencyCode.equals("BSQ")) ||
                (quoteCurrencyCode.equals("BTC") && baseCurrencyCode.equals("BSQ"));
    }

    private static boolean isLNSwapSupported(Market market) {
        return false;//todo need some liquid asset lookup table
    }

    private static boolean isMultiSigSupported(Market market) {
        return market.getQuoteCurrencyCode().equals("BTC") || market.getBaseCurrencyCode().equals("BTC");
    }
}
