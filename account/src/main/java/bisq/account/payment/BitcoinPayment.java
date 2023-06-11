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

package bisq.account.payment;

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BitcoinPayment extends Payment<BitcoinPayment.Method> {
    public static List<BitcoinPayment.Method> getPaymentMethods() {
        return List.of(BitcoinPayment.Method.values());
    }

    public static BitcoinPayment from(String paymentMethodName) {
        try {
            return new BitcoinPayment(BitcoinPayment.Method.valueOf(paymentMethodName));
        } catch (IllegalArgumentException e) {
            return new BitcoinPayment(paymentMethodName);
        }
    }

    public static List<BitcoinPayment.Method> getPaymentMethods(ProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
            case BISQ_MULTISIG:
                return BitcoinPayment.getPaymentMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
            case LIGHTNING_X:
                throw new IllegalArgumentException("No paymentMethods for that protocolType");
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Payment.Method {
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

    public BitcoinPayment(BitcoinPayment.Method method) {
        super(method);
    }

    public BitcoinPayment(String paymentMethodName) {
        super(paymentMethodName);
    }

    protected BitcoinPayment.Method getFallbackMethod() {
        return BitcoinPayment.Method.USER_DEFINED;
    }

    @Override
    public bisq.account.protobuf.Payment toProto() {
        return getPaymentBuilder().setBitcoinPayment(bisq.account.protobuf.BitcoinPayment.newBuilder()).build();
    }

    public static BitcoinPayment fromProto(bisq.account.protobuf.Payment proto) {
        return BitcoinPayment.from(proto.getPaymentMethodName());
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return List.of(CryptoCurrencyRepository.BITCOIN);
    }
}
