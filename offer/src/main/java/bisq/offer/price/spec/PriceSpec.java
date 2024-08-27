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

package bisq.offer.price.spec;

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;

public interface PriceSpec extends NetworkProto {

    default bisq.offer.protobuf.PriceSpec.Builder getPriceSpecBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.PriceSpec.newBuilder();
    }

    @Override
    bisq.offer.protobuf.PriceSpec toProto(boolean serializeForHash);

    static PriceSpec fromProto(bisq.offer.protobuf.PriceSpec proto) {
        return switch (proto.getMessageCase()) {
            case FIXPRICE -> FixPriceSpec.fromProto(proto.getFixPrice());
            case FLOATPRICE -> FloatPriceSpec.fromProto(proto.getFloatPrice());
            case MARKETPRICE -> MarketPriceSpec.fromProto(proto.getMarketPrice());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }
}
