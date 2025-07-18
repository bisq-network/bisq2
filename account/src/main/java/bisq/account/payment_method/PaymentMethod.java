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

import bisq.account.payment_method.cbdc.CbdcPaymentMethod;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.stable_coin.StableCoinPaymentMethod;
import bisq.common.asset.Asset;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

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

    // Only paymentRail name is used for protobuf, thus other fields are transient.
    protected final String paymentRailName;

    // We do not persist the paymentRail but still include it in EqualsAndHashCode.
    protected transient final R paymentRail;


    /**
     * @param paymentRail The method to be associated with that payment method
     */
    protected PaymentMethod(R paymentRail) {
        this.paymentRailName = paymentRail.name();
        this.paymentRail = paymentRail;
    }

    /**
     * @param customPaymentRailName Provide custom payment method name not covered by a Method enum.
     */
    protected PaymentMethod(String customPaymentRailName) {
        this.paymentRailName = customPaymentRailName;
        this.paymentRail = getCustomPaymentRail();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(paymentRailName, MAX_NAME_LENGTH);
    }

    public abstract String getId();

    public String getDisplayString() {
        return Res.has(paymentRailName) ? Res.get(paymentRailName) : paymentRailName;
    }

    public String getShortDisplayString() {
        String shortName = paymentRailName + "_SHORT";
        return Res.has(shortName) ? Res.get(shortName) : getDisplayString();
    }

    @Override
    public abstract bisq.account.protobuf.PaymentMethod toProto(boolean serializeForHash);

    protected bisq.account.protobuf.PaymentMethod.Builder getPaymentMethodBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PaymentMethod.newBuilder().setPaymentRailName(paymentRailName);
    }

    public static PaymentMethod<? extends PaymentRail> fromProto(bisq.account.protobuf.PaymentMethod proto) {
        return switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHOD -> FiatPaymentMethod.fromProto(proto);
            case BITCOINPAYMENTMETHOD -> BitcoinPaymentMethod.fromProto(proto);
            case CRYPTOPAYMENTMETHOD -> CryptoPaymentMethod.fromProto(proto);
            case STABLECOINPAYMENTMETHOD -> StableCoinPaymentMethod.fromProto(proto);
            case CBDCPAYMENTMETHOD -> CbdcPaymentMethod.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }


    @Override
    public int compareTo(PaymentMethod<R> o) {
        return paymentRailName.compareTo(o.getPaymentRailName());
    }

    public List<String> getSupportedCurrencyCodes() {
        return getSupportedCurrencies().stream()
                .map(Asset::getCode)
                .collect(Collectors.toList());
    }

    public List<String> getSupportedCurrencyDisplayNameAndCode() {
        return getSupportedCurrencies().stream()
                .map(Asset::getDisplayNameAndCode)
                .collect(Collectors.toList());
    }

    public abstract List<? extends Asset> getSupportedCurrencies();

    public boolean isCustomPaymentMethod() {
        return paymentRail.equals(getCustomPaymentRail());
    }

    protected abstract R getCustomPaymentRail();
}
