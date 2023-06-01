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

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BitcoinSettlement extends Settlement<BitcoinSettlement.Method> {
    public static List<BitcoinSettlement.Method> getSettlementMethods() {
        return List.of(BitcoinSettlement.Method.values());
    }

    public static BitcoinSettlement from(String settlementMethodName) {
        try {
            return new BitcoinSettlement(BitcoinSettlement.Method.valueOf(settlementMethodName));
        } catch (IllegalArgumentException e) {
            return new BitcoinSettlement(settlementMethodName);
        }
    }

    public static List<BitcoinSettlement.Method> getSettlementMethods(ProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
                return BitcoinSettlement.getSettlementMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_X:
                throw new IllegalArgumentException("No settlementMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Settlement.Method {
        USER_DEFINED,
        MAINCHAIN,
        LN,
        LBTC,
        RBTC,
        WBTC,
        OTHER;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public BitcoinSettlement(BitcoinSettlement.Method method) {
        super(method);
    }

    public BitcoinSettlement(String settlementMethodName) {
        super(settlementMethodName);
    }

    protected BitcoinSettlement.Method getFallbackMethod() {
        return BitcoinSettlement.Method.USER_DEFINED;
    }

    @Override
    public bisq.account.protobuf.Settlement toProto() {
        return getSettlementBuilder().setBitcoinSettlement(bisq.account.protobuf.BitcoinSettlement.newBuilder()).build();
    }

    public static BitcoinSettlement fromProto(bisq.account.protobuf.Settlement proto) {
        return BitcoinSettlement.from(proto.getSettlementMethodName());
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return List.of(CryptoCurrencyRepository.BITCOIN);
    }
}
