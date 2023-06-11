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

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface PaymentMethodSpec extends Proto {
    String getPaymentMethodName();

    bisq.offer.protobuf.PaymentMethodSpec toProto();

    default bisq.offer.protobuf.PaymentMethodSpec.Builder getPaymentMethodSpecBuilder() {
        return bisq.offer.protobuf.PaymentMethodSpec.newBuilder();
    }

    static PaymentMethodSpec fromProto(bisq.offer.protobuf.PaymentMethodSpec proto) {
        switch (proto.getMessageCase()) {
            case FIATPAYMENTMETHODSPEC: {
                return FiatPaymentMethodSpec.fromProto(proto.getFiatPaymentMethodSpec());
            }
            case BITCOINPAYMENTMETHODSPEC: {
                return BitcoinPaymentMethodSpec.fromProto(proto.getBitcoinPaymentMethodSpec());
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
