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

import bisq.common.proto.PersistableProto;
import bisq.network.identity.NetworkId;
import bisq.network.identity.NetworkIdWithKeyPair;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.PubKey;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public final class Identity implements PersistableProto {
    // Reference to usage (e.g. offerId)
    @Getter
    private final String tag;
    @Getter
    private final NetworkId networkId;
    @Getter
    private final KeyBundle keyBundle;

    public Identity(String tag, NetworkId networkId, KeyBundle keyBundle) {
        this.tag = tag;
        this.networkId = networkId;
        this.keyBundle = keyBundle;
    }

    @Override
    public bisq.identity.protobuf.Identity.Builder getBuilder(boolean serializeForHash) {
        return bisq.identity.protobuf.Identity.newBuilder()
                .setDomainId(tag)
                .setNetworkId(networkId.toProto(serializeForHash))
                .setKeyBundle(keyBundle.toProto(serializeForHash));
    }

    @Override
    public bisq.identity.protobuf.Identity toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Identity fromProto(bisq.identity.protobuf.Identity proto) {
        return new Identity(proto.getDomainId(),
                NetworkId.fromProto(proto.getNetworkId()),
                KeyBundle.fromProto(proto.getKeyBundle()));
    }

    public NetworkIdWithKeyPair getNetworkIdWithKeyPair() {
        return new NetworkIdWithKeyPair(networkId, keyBundle.getKeyPair());
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

    public boolean isDefaultTag() {
        return tag.equals(IdentityService.DEFAULT_IDENTITY_TAG);
    }
}