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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class BitcoinPaymentMethodSpec extends PaymentMethodSpec<BitcoinPaymentMethod> {
    public BitcoinPaymentMethodSpec(BitcoinPaymentMethod paymentMethod) {
        super(paymentMethod);
    }

    public BitcoinPaymentMethodSpec(BitcoinPaymentMethod paymentMethod, Optional<String> saltedMakerAccountId) {
        super(paymentMethod, saltedMakerAccountId);

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.account.protobuf.PaymentMethodSpec.Builder getBuilder(boolean serializeForHash) {
        return getPaymentMethodSpecBuilder(serializeForHash)
                .setBitcoinPaymentMethodSpec(bisq.account.protobuf.BitcoinPaymentMethodSpec.newBuilder());
    }

    @Override
    public bisq.account.protobuf.PaymentMethodSpec toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static BitcoinPaymentMethodSpec fromProto(bisq.account.protobuf.PaymentMethodSpec proto) {
        return new BitcoinPaymentMethodSpec(BitcoinPaymentMethod.fromProto(proto.getPaymentMethod()),
                proto.hasSaltedMakerAccountId() ? Optional.of(proto.getSaltedMakerAccountId()) : Optional.empty());
    }
}