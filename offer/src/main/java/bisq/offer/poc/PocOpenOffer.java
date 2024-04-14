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

package bisq.offer.poc;

import bisq.common.proto.NetworkProto;
import com.google.protobuf.Message;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public final class PocOpenOffer implements NetworkProto {
    private final PocOffer offer;

    public PocOpenOffer(PocOffer offer) {
        this.offer = offer;
        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    @Override
    public Message toProto(boolean serializeForHash) {
        return null;
    }
}