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

import bisq.common.currency.TradeCurrency;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class PaymentMethod<T extends PaymentRail> implements Proto {
    protected final String name;
    protected final T paymentRail;

    /**
     * @param paymentRail The method to be associated with that payment method
     */
    public PaymentMethod(T paymentRail) {
        this.name = paymentRail.name();
        this.paymentRail = paymentRail;
    }

    /**
     * @param customName Provide custom payment method name not covered by a Method enum.
     *                   In that case we set the method to the fallback method (e.g. USER_DEFINED).
     */
    public PaymentMethod(String customName) {
        this.name = customName;
        this.paymentRail = getCustomPaymentRail();
    }

    public abstract bisq.account.protobuf.PaymentMethod toProto();

    protected bisq.account.protobuf.PaymentMethod.Builder getPaymentMethodBuilder() {
        return bisq.account.protobuf.PaymentMethod.newBuilder()
                .setName(name);
    }

    public static PaymentMethod<? extends PaymentRail> fromProto(bisq.account.protobuf.PaymentMethod proto) {
        switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHOD: {
                return FiatPaymentMethod.fromProto(proto);
            }
            case BITCOINPAYMENTMETHOD: {
                return BitcoinPaymentMethod.fromProto(proto);
            }
            case CRYPTOPAYMENTMETHOD: {
                return CryptoPaymentMethod.fromProto(proto);
            }

            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    protected abstract T getCustomPaymentRail();

    public abstract List<TradeCurrency> getTradeCurrencies();

    protected String getDisplayName(String code) {
        return Res.get(getName());
    }
}
