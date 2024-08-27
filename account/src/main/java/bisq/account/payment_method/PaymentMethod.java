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
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * PaymentMethod wraps the PaymentRail by its enum name and provides util methods.
 * Its main purpose is to support custom payment methods and to provide a backward compatible solution when
 * new PaymentRail gets added.
 * For that reason do not persist the paymentRail. In case a user with a newer version would have
 * a paymentRail not existing at another peer the peer with the older version would consider it as a custom
 * payment method and would still be able to deal with it.
 */

@ToString
@EqualsAndHashCode
@Getter
public abstract class PaymentMethod<R extends PaymentRail> implements Comparable<PaymentMethod<R>>, NetworkProto {
    public final static int MAX_NAME_LENGTH = 50;

    // Only name is used for protobuf, thus other fields are transient.
    protected final String name;

    // We do not persist the paymentRail but still include it in EqualsAndHashCode.
    protected transient final R paymentRail;

    protected transient final String displayString;
    protected transient final String shortDisplayString;

    /**
     * @param paymentRail The method to be associated with that payment method
     */
    protected PaymentMethod(R paymentRail) {
        this.name = paymentRail.name();

        this.paymentRail = paymentRail;

        displayString = createDisplayString();
        shortDisplayString = createShortDisplayString();

        NetworkDataValidation.validateText(name, MAX_NAME_LENGTH);
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

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(name, 100);
    }

    protected String createDisplayString() {
        return Res.has(name) ? Res.get(name) : name;
    }

    protected String createShortDisplayString() {
        String shortName = name + "_SHORT";
        return Res.has(shortName) ? Res.get(shortName) : createDisplayString();
    }

    @Override
    public abstract bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash);

    protected bisq.account.protobuf.PaymentMethod.Builder getPaymentMethodBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PaymentMethod.newBuilder().setName(name);
    }

    public static PaymentMethod<? extends PaymentRail> fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHOD -> FiatPaymentMethod.fromProto(proto);
            case BITCOINPAYMENTMETHOD -> BitcoinPaymentMethod.fromProto(proto);
            case CRYPTOPAYMENTMETHOD -> CryptoPaymentMethod.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    protected abstract R getCustomPaymentRail();

    public abstract List<TradeCurrency> getTradeCurrencies();

    public boolean isCustomPaymentMethod() {
        return paymentRail == getCustomPaymentRail();
    }

    @Override
    public int compareTo(PaymentMethod<R> o) {
        return name.compareTo(o.getName());
    }
}
