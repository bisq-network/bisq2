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

package bisq.offer.protocol;

import bisq.account.settlement.CryptoSettlement;
import bisq.account.settlement.FiatSettlement;
import bisq.account.settlement.Settlement;
import bisq.common.currency.BisqCurrency;
import bisq.common.monetary.Market;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ProtocolSpecifics {

    public static List<? extends Settlement.Method> getSettlementMethods(SwapProtocolType protocolType, String code) {
        if (BisqCurrency.isFiat(code)) {
            return getFiatSettlementMethods(protocolType);
        } else {
            return getCryptoSettlementMethods(protocolType);
        }
    }

    public static List<FiatSettlement.Method> getFiatSettlementMethods(SwapProtocolType protocolType) {
        return switch (protocolType) {
            case BTC_XMR_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case LIQUID_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case BSQ_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case LN_SWAP -> throw new IllegalArgumentException("No fiat support for that protocolType");
            case MULTISIG -> List.of(FiatSettlement.Method.values());
            case BSQ_BOND -> List.of(FiatSettlement.Method.values());
            case REPUTATION -> List.of(FiatSettlement.Method.values());
        };
    }

    public static List<CryptoSettlement.Method> getCryptoSettlementMethods(SwapProtocolType protocolType) {
        return switch (protocolType) {
            case BTC_XMR_SWAP -> List.of(CryptoSettlement.Method.MAINNET);
            case LIQUID_SWAP -> List.of(CryptoSettlement.Method.MAINNET);
            case BSQ_SWAP -> List.of(CryptoSettlement.Method.MAINNET);
            case LN_SWAP -> List.of(CryptoSettlement.Method.LN);
            case MULTISIG -> List.of(CryptoSettlement.Method.MAINNET);
            case BSQ_BOND -> List.of(CryptoSettlement.Method.values());
            case REPUTATION -> List.of(CryptoSettlement.Method.values());
        };
    }


    public static List<SwapProtocolType> getProtocols(Market market) {
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
        return false;//todo need some liquid asset lookup table
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