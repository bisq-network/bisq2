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

package bisq.offer;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public final class OpenOffer implements Proto {
    private final Offer offer;

    public OpenOffer(Offer offer) {
        this.offer = offer;
    }

    public bisq.offer.protobuf.OpenOffer toProto() {
        return bisq.offer.protobuf.OpenOffer.newBuilder()
                .setOffer(offer.toProto())
                .build();
    }

    public static OpenOffer fromProto(bisq.offer.protobuf.OpenOffer proto) {
        return new OpenOffer(Offer.fromProto(proto.getOffer()));
    }
}