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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public final class BitcoinPaymentMethodSpec implements PaymentMethodSpec {
    private final String address;
    private final Optional<String> saltedMakerAccountId;

    public BitcoinPaymentMethodSpec(String address) {
        this(address, Optional.empty());
    }

    public BitcoinPaymentMethodSpec(String address, Optional<String> saltedMakerAccountId) {
        this.address = address;
        this.saltedMakerAccountId = saltedMakerAccountId;
    }

    public bisq.offer.protobuf.PaymentMethodSpec toProto() {
        bisq.offer.protobuf.BitcoinPaymentMethodSpec.Builder builder = bisq.offer.protobuf.BitcoinPaymentMethodSpec.newBuilder()
                .setAddress(address);
        saltedMakerAccountId.ifPresent(builder::setSaltedMakerAccountId);
        return getPaymentMethodSpecBuilder().setBitcoinPaymentMethodSpec(builder).build();
    }

    public static BitcoinPaymentMethodSpec fromProto(bisq.offer.protobuf.BitcoinPaymentMethodSpec proto) {
        return new BitcoinPaymentMethodSpec(proto.getAddress(),
                proto.hasSaltedMakerAccountId() ? Optional.of(proto.getSaltedMakerAccountId()) : Optional.empty());
    }

    public String getPaymentMethodName() {
        return address;
    }
}