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
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.network.identity.TorIdentity;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyPairProtoUtil;
import bisq.security.keys.PubKey;
import lombok.Getter;
import lombok.ToString;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Objects;

@Getter
@ToString
public final class Identity implements Proto {
  /*  public static Identity from(String domainId, Identity identity) {
        return new Identity(domainId, identity.getNetworkId(), identity.getTorIdentity(), identity.getKeyPair(), null);
    }*/

    // Reference to usage (e.g. offerId)
    private final String tag;
    private final NetworkId networkId;
    private final TorIdentity torIdentity;
    private final KeyPair keyPair;
    private final KeyBundle keyBundle;

    public Identity(String tag, NetworkId networkId, TorIdentity torIdentity, KeyPair keyPair, KeyBundle keyBundle) {
        this.tag = tag;
        this.networkId = networkId;
        this.torIdentity = torIdentity;
        this.keyPair = keyPair;
        this.keyBundle = keyBundle;
    }

    @Override
    public bisq.identity.protobuf.Identity toProto() {
        return bisq.identity.protobuf.Identity.newBuilder()
                .setDomainId(tag)
                .setNetworkId(networkId.toProto())
                .setTorIdentity(torIdentity.toProto())
                .setKeyPair(KeyPairProtoUtil.toProto(keyPair))
                .build();
    }

    public static Identity fromProto(bisq.identity.protobuf.Identity proto) {
        return new Identity(proto.getDomainId(),
                NetworkId.fromProto(proto.getNetworkId()),
                TorIdentity.fromProto(proto.getTorIdentity()), KeyPairProtoUtil.fromProto(proto.getKeyPair()),
                KeyBundle.fromProto(proto.getKeyBundle()));
    }

    public NetworkIdWithKeyPair getNetworkIdWithKeyPair() {
        return new NetworkIdWithKeyPair(networkId, keyPair);
    }

    public String getId() {
        return networkId.getPubKey().getId();
    }

    public PubKey getPubKey() {
        return networkId.getPubKey();
    }

    public byte[] getPubKeyHash() {
        return networkId.getPubKey().getHash();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return Objects.equals(tag, identity.tag) &&
                Objects.equals(networkId, identity.networkId) &&
                Arrays.equals(keyPair.getPublic().getEncoded(), identity.keyPair.getPublic().getEncoded()) &&
                Arrays.equals(keyPair.getPrivate().getEncoded(), identity.keyPair.getPrivate().getEncoded());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tag, networkId);
        result = 31 * result + Arrays.hashCode(keyPair.getPublic().getEncoded());
        result = 31 * result + Arrays.hashCode(keyPair.getPrivate().getEncoded());
        return result;
    }
}