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

package bisq.network.identity;

import bisq.common.proto.Proto;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.security.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class NetworkId implements Proto {
    private final PubKey pubKey;
    private final AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();

    @EqualsAndHashCode.Include
    private final TorIdentity torIdentity;

    public NetworkId(AddressByTransportTypeMap addressByTransportTypeMap, PubKey pubKey, TorIdentity torIdentity) {
        this.pubKey = pubKey;
        this.torIdentity = torIdentity;

        checkArgument(!addressByTransportTypeMap.isEmpty(),
                "We require at least 1 addressByNetworkType for a valid NetworkId");
        this.addressByTransportTypeMap.putAll(addressByTransportTypeMap);
    }

    public bisq.network.identity.protobuf.NetworkId toProto() {
        return bisq.network.identity.protobuf.NetworkId.newBuilder()
                .setAddressByNetworkTypeMap(addressByTransportTypeMap.toProto())
                .setPubKey(pubKey.toProto())
                .setTorIdentity(torIdentity.toProto())
                .build();
    }

    public static NetworkId fromProto(bisq.network.identity.protobuf.NetworkId proto) {
        return new NetworkId(AddressByTransportTypeMap.fromProto(proto.getAddressByNetworkTypeMap()),
                PubKey.fromProto(proto.getPubKey()),
                TorIdentity.fromProto(proto.getTorIdentity()));
    }

    public String getId() {
        return pubKey.getId();
    }

    @Override
    public String toString() {
        return "NetworkId(" +
                "addressByTransportTypeMap=" + addressByTransportTypeMap +
                ", pubKey=" + pubKey +
                ")";
    }
}

