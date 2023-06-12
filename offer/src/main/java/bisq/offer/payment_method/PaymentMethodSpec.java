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

package bisq.offer.payment_method;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentRail;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

@Getter
@EqualsAndHashCode
public abstract class PaymentMethodSpec<T extends PaymentMethod<? extends PaymentRail>> implements Proto {
    protected final Optional<String> saltedMakerAccountId;
    protected final T paymentMethod;

    public PaymentMethodSpec(T paymentMethod) {
        this(paymentMethod, Optional.empty());
    }

    /**
     * @param paymentMethod        The paymentMethod
     * @param saltedMakerAccountId Salted local ID of maker's payment account.
     *                             In case maker had multiple payment accounts for same payment method they
     *                             can define which account to use for that offer.
     *                             We combine the local ID with an offer specific salt, to not leak identity of multiple
     *                             offers using different identities but the same payment account.
     */
    public PaymentMethodSpec(T paymentMethod, Optional<String> saltedMakerAccountId) {
        this.paymentMethod = paymentMethod;
        this.saltedMakerAccountId = saltedMakerAccountId;
    }

    public abstract bisq.offer.protobuf.PaymentMethodSpec toProto();

    public bisq.offer.protobuf.PaymentMethodSpec.Builder getPaymentMethodSpecBuilder() {
        bisq.offer.protobuf.PaymentMethodSpec.Builder builder = bisq.offer.protobuf.PaymentMethodSpec.newBuilder()
                .setPaymentMethod(paymentMethod.toProto());
        saltedMakerAccountId.ifPresent(builder::setSaltedMakerAccountId);
        return builder;
    }

    // We use unsafe cast here as in case the proto data would not match our expected class we would get anyway 
    // an exception.
    public static <T extends PaymentMethodSpec<?>> T fromProto(bisq.offer.protobuf.PaymentMethodSpec proto) {
        switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHODSPEC: {
                //noinspection unchecked
                return (T) FiatPaymentMethodSpec.fromProto(proto);
            }
            case BITCOINPAYMENTMETHODSPEC: {
                //noinspection unchecked
                return (T) BitcoinPaymentMethodSpec.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public static <T extends PaymentMethodSpec<? extends PaymentMethod<? extends PaymentRail>>> T fromProt1o(bisq.offer.protobuf.PaymentMethodSpec proto) {
        switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHODSPEC: {
                //noinspection unchecked
                return (T) FiatPaymentMethodSpec.fromProto(proto);
            }
            case BITCOINPAYMENTMETHODSPEC: {
                //noinspection unchecked
                return (T) BitcoinPaymentMethodSpec.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public String getPaymentMethodName() {
        return paymentMethod.getName();
    }
}
