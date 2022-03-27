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

package bisq.offer.spec;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface PriceSpec extends Proto {
    bisq.offer.protobuf.PriceSpec toProto();

    default bisq.offer.protobuf.PriceSpec.Builder getPriceSpecBuilder() {
        return bisq.offer.protobuf.PriceSpec.newBuilder();
    }

    static PriceSpec fromProto(bisq.offer.protobuf.PriceSpec proto) {
        switch (proto.getMessageCase()) {
            case FIXPRICE -> {
                return FixPrice.fromProto(proto.getFixPrice());
            }
            case FLOATPRICE -> {
                return FloatPrice.fromProto(proto.getFloatPrice());
            }
            case MESSAGE_NOT_SET -> {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
