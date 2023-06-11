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

package bisq.account.payment_method;

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.CryptoCurrencyRepository;
import bisq.common.currency.TradeCurrency;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CryptoPayment extends Payment<CryptoPayment.Method> {
    public static List<CryptoPayment.Method> getPaymentMethods() {
        return List.of(CryptoPayment.Method.values());
    }

    public static CryptoPayment from(String paymentMethodName, String currencyCode) {
        try {
            return new CryptoPayment(CryptoPayment.Method.valueOf(paymentMethodName), currencyCode);
        } catch (IllegalArgumentException e) {
            return new CryptoPayment(paymentMethodName, currencyCode);
        }
    }

    public static List<CryptoPayment.Method> getPaymentMethods(ProtocolType protocolType) {
        switch (protocolType) {
            case BISQ_EASY:
                throw new IllegalArgumentException("No support for CryptoPayments for BISQ_EASY");
            case BISQ_MULTISIG:
            case LIGHTNING_X:
                return CryptoPayment.getPaymentMethods();
            case MONERO_SWAP:
            case LIQUID_SWAP:
            case BSQ_SWAP:
                return List.of(CryptoPayment.Method.NATIVE_CHAIN);
            default:
                throw new RuntimeException("Not handled case: protocolType=" + protocolType);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Method enum
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public enum Method implements Payment.Method {
        USER_DEFINED,
        NATIVE_CHAIN,
        OTHER;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Class instance
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private final String currencyCode;

    public CryptoPayment(CryptoPayment.Method method, String currencyCode) {
        super(method);
        this.currencyCode = currencyCode;
    }

    public CryptoPayment(String paymentMethodName, String currencyCode) {
        super(paymentMethodName);
        this.currencyCode = currencyCode;
    }

    @Override
    protected CryptoPayment.Method getFallbackMethod() {
        return CryptoPayment.Method.USER_DEFINED;
    }

    @Override
    public bisq.account.protobuf.Payment toProto() {
        return getPaymentBuilder().setCryptoPayment(
                        bisq.account.protobuf.CryptoPayment.newBuilder()
                                .setCurrencyCode(currencyCode))
                .build();
    }

    public static CryptoPayment fromProto(bisq.account.protobuf.Payment proto) {
        return CryptoPayment.from(proto.getPaymentMethodName(), proto.getCryptoPayment().getCurrencyCode());
    }

    @Override
    public List<TradeCurrency> getTradeCurrencies() {
        return CryptoCurrencyRepository.find(currencyCode)
                .map(e -> List.of((TradeCurrency) e))
                .orElse(new ArrayList<>());
    }
}
