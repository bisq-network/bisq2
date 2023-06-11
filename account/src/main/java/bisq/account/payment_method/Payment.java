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
import bisq.common.currency.TradeCurrency;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class Payment<M extends Payment.Method> implements Proto {
    public static List<? extends Method> getPaymentMethods(ProtocolType protocolType, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatPayment.getPaymentMethodsForProtocolType(protocolType);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinPayment.getPaymentMethods(protocolType);
            } else {
                return CryptoPayment.getPaymentMethods(protocolType);
            }
        }
    }

    public static Payment<? extends Method> from(String paymentMethodName, String currencyCode) {
        if (TradeCurrency.isFiat(currencyCode)) {
            return FiatPayment.fromName(paymentMethodName);
        } else {
            if (currencyCode.equals("BTC")) {
                return BitcoinPayment.from(paymentMethodName);
            } else {
                return CryptoPayment.from(paymentMethodName, currencyCode);
            }
        }
    }

    public static Method getPaymentMethod(String name, String currencyCode) {
        return from(name, currencyCode).getMethod();
    }

    public interface Method {
        String name();
    }

    protected final String paymentMethodName;
    protected final M method;

    public Payment(M method) {
        this.paymentMethodName = method.name();
        this.method = method;
    }

    public Payment(String paymentMethodName) {
        this.paymentMethodName = paymentMethodName;
        this.method = getFallbackMethod();
    }

    public abstract bisq.account.protobuf.Payment toProto();

    protected bisq.account.protobuf.Payment.Builder getPaymentBuilder() {
        return bisq.account.protobuf.Payment.newBuilder()
                .setPaymentMethodName(paymentMethodName);
    }

    public static Payment<? extends Method> fromProto(bisq.account.protobuf.Payment proto) {
        switch (proto.getMessageCase()) {
            case FIATPAYMENT: {
                return FiatPayment.fromProto(proto);
            }
            case BITCOINPAYMENT: {
                return BitcoinPayment.fromProto(proto);
            }
            case CRYPTOPAYMENT: {
                return CryptoPayment.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }


    protected abstract M getFallbackMethod();

    public abstract List<TradeCurrency> getTradeCurrencies();

    protected String getDisplayName(String code) {
        return Res.get(getPaymentMethodName());
    }
}
