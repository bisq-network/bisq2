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
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString
@Getter
@EqualsAndHashCode
public abstract class PaymentMethodSpec<T extends PaymentMethod<? extends PaymentRail>> implements NetworkProto {
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

    @Override
    public void verify() {
        NetworkDataValidation.validateText(saltedMakerAccountId, 100);
    }

    @Override
    public abstract bisq.offer.protobuf.PaymentMethodSpec toProto(boolean serializeForHash);

    public bisq.offer.protobuf.PaymentMethodSpec.Builder getPaymentMethodSpecBuilder(boolean serializeForHash) {
        bisq.offer.protobuf.PaymentMethodSpec.Builder builder = bisq.offer.protobuf.PaymentMethodSpec.newBuilder()
                .setPaymentMethod(paymentMethod.toProto(serializeForHash));
        saltedMakerAccountId.ifPresent(builder::setSaltedMakerAccountId);
        return builder;
    }

    // Alternative signature would be: `public static <T extends PaymentMethodSpec<?>> T fromProto(bisq.offer.protobuf.PaymentMethodSpec proto)`
    // This would require an unsafe cast  (T) for the return type.
    // The caller would provide the expected type. E.g. `PaymentMethodSpec::<BitcoinPaymentMethodSpec>fromProto`
    // By using specific methods we avoid those issues and the code is more readable without generics overhead.

    public static FiatPaymentMethodSpec protoToFiatPaymentMethodSpec(bisq.offer.protobuf.PaymentMethodSpec proto) {
        return switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHODSPEC -> FiatPaymentMethodSpec.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public static BitcoinPaymentMethodSpec protoToBitcoinPaymentMethodSpec(bisq.offer.protobuf.PaymentMethodSpec proto) {
        return switch (proto.getMessageCase()) {
            case BITCOINPAYMENTMETHODSPEC -> BitcoinPaymentMethodSpec.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }

    public String getPaymentMethodName() {
        return paymentMethod.getName();
    }

    public String getShortDisplayString() {
        return paymentMethod.getShortDisplayString();
    }

    public String getDisplayString() {
        return paymentMethod.getDisplayString();
    }
}
