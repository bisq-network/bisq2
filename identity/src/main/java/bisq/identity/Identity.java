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

package bisq.identity;

import bisq.common.proto.Proto;
import bisq.network.NetworkId;
import bisq.network.NetworkIdWithKeyPair;
import bisq.security.KeyPairProtoUtil;
import bisq.security.PubKey;

import java.security.KeyPair;

public record Identity(String domainId, NetworkId networkId, KeyPair keyPair) implements Proto {
    public bisq.identity.protobuf.Identity toProto() {
        return bisq.identity.protobuf.Identity.newBuilder()
                .setDomainId(domainId)
                .setNetworkId(networkId.toProto())
                .setKeyPair(KeyPairProtoUtil.toProto(keyPair))
                .build();
    }

    public static Identity fromProto(bisq.identity.protobuf.Identity proto) {
        return new Identity(proto.getDomainId(),
                NetworkId.fromProto(proto.getNetworkId()),
                KeyPairProtoUtil.fromProto(proto.getKeyPair()));
    }

    public NetworkIdWithKeyPair getNodeIdAndKeyPair() {
        return new NetworkIdWithKeyPair(networkId, keyPair);
    }

    public String nodeId() {
        return networkId.getNodeId();
    }

    public PubKey pubKey() {
        return networkId.getPubKey();
    }
}