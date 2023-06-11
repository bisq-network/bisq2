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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

@Getter
@EqualsAndHashCode
public abstract class PaymentMethod<R extends PaymentRail> implements Proto {
    protected final String name;
    protected final R paymentRail;

    @EqualsAndHashCode.Exclude
    protected transient final String displayString;
    @EqualsAndHashCode.Exclude
    protected transient final String shortDisplayString;

    /**
     * @param paymentRail The method to be associated with that payment method
     */
    protected PaymentMethod(R paymentRail) {
        this.name = paymentRail.name();
        this.paymentRail = paymentRail;

        displayString = createDisplayString();
        shortDisplayString = createShortDisplayString();
    }

    /**
     * @param customName Provide custom payment method name not covered by a Method enum.
     *                   In that case we set the method to the fallback method (e.g. USER_DEFINED).
     */
    protected PaymentMethod(String customName) {
        this.name = customName;
        this.paymentRail = getCustomPaymentRail();

        // Avoid accidentally using a translation string in case the customName would match a key
        displayString = name;
        shortDisplayString = name;
    }

    protected String createDisplayString() {
        return Res.has(name) ? Res.get(name) : name;
    }

    protected String createShortDisplayString() {
        String shortName = name + "_SHORT";
        return Res.has(shortName) ? Res.get(shortName) : createDisplayString();
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

    protected abstract R getCustomPaymentRail();

    public abstract List<TradeCurrency> getTradeCurrencies();

    public boolean isCustomPaymentMethod() {
        return paymentRail == getCustomPaymentRail();
    }
}
