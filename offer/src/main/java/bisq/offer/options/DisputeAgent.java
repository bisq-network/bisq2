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

package bisq.offer.options;

import bisq.common.util.ProtobufUtils;
import bisq.network.NetworkId;

public record DisputeAgent(Type type, NetworkId networkId) {
    public bisq.offer.protobuf.DisputeAgent toProto() {
        return bisq.offer.protobuf.DisputeAgent.newBuilder()
                .setType(type.name())
                .setNetworkId(networkId.toProto())
                .build();
    }

    public static DisputeAgent fromProto(bisq.offer.protobuf.DisputeAgent proto) {
        return new DisputeAgent(ProtobufUtils.enumFromProto(Type.class, proto.getType()), 
                NetworkId.fromProto(proto.getNetworkId()));
    }

    public enum Type {
        MEDIATOR,
        ARBITRATOR
    }
}