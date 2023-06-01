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

package bisq.account.settlement;

import bisq.account.protocol_type.SwapProtocolType;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CryptoSettlement extends Settlement<CryptoSettlement.Method> {
    public static List<CryptoSettlement.Method> getSettlementMethods() {
        return List.of(CryptoSettlement.Method.values());
    }

    public static CryptoSettlement from(String settlementMethodName, String currencyCode) {
        try {
            return new CryptoSettlement(CryptoSettlement.Method.valueOf(settlementMethodName), currencyCode);
        } catch (IllegalArgumentException e) {
            return new CryptoSettlement(settlementMethodName, currencyCode);
        }
    }

    public static List<CryptoSettlement.Method> getSettlementMethods(SwapProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
                throw new IllegalArgumentException("No support for CryptoSettlements for BISQ_EASY");
            case BISQ_MULTISIG:
            case LIGHTNING_X:
                return CryptoSettlement.getSettlementMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
                return List.of(CryptoSettlement.Method.NATIVE_CHAIN);
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Settlement.Method {
        USER_DEFINED,
        NATIVE_CHAIN,
        OTHER;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private final String currencyCode;

    public CryptoSettlement(CryptoSettlement.Method method, String currencyCode) {
        super(method);
        this.currencyCode = currencyCode;
    }

    public CryptoSettlement(String settlementMethodName, String currencyCode) {
        super(settlementMethodName);
        this.currencyCode = currencyCode;
    }

    @Override
    protected CryptoSettlement.Method getFallbackMethod() {
        return CryptoSettlement.Method.USER_DEFINED;
    }

    @Override
    public bisq.account.protobuf.Settlement toProto() {
        return toProtoBuilder().setCryptoSettlement(
                        bisq.account.protobuf.CryptoSettlement.newBuilder()
                                .setCurrencyCode(currencyCode))
                .build();
    }

    public static CryptoSettlement fromProto(bisq.account.protobuf.Settlement proto) {
        return CryptoSettlement.from(proto.getSettlementMethodName(), proto.getCryptoSettlement().getCurrencyCode());
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return CryptoCurrencyRepository.find(currencyCode)
                .map(e -> List.of((TradeCurrency) e))
                .orElse(new ArrayList<>());
    }
}
