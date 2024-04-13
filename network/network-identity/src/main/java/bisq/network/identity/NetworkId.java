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

import bisq.common.proto.NetworkProto;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.security.keys.PubKey;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode
public final class NetworkId implements NetworkProto {
    private final PubKey pubKey;
    private final AddressByTransportTypeMap addressByTransportTypeMap = new AddressByTransportTypeMap();

    public NetworkId(AddressByTransportTypeMap addressByTransportTypeMap, PubKey pubKey) {
        this.pubKey = pubKey;
        this.addressByTransportTypeMap.putAll(addressByTransportTypeMap);
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.identity.protobuf.NetworkId toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    @Override
    public bisq.network.identity.protobuf.NetworkId.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.network.identity.protobuf.NetworkId.newBuilder()
                .setAddressByNetworkTypeMap(addressByTransportTypeMap.toProto(ignoreAnnotation))
                .setPubKey(pubKey.toProto(ignoreAnnotation));
    }

    public static NetworkId fromProto(bisq.network.identity.protobuf.NetworkId proto) {
        return new NetworkId(AddressByTransportTypeMap.fromProto(proto.getAddressByNetworkTypeMap()),
                PubKey.fromProto(proto.getPubKey()));
    }

    public String getId() {
        return pubKey.getId();
    }

    public String getKeyId() {
        return pubKey.getKeyId();
    }

    public String getInfo() {
        return "ID: " + getId().substring(0, 8) + "; Addresses: " +
                Joiner.on(", ").join(addressByTransportTypeMap.values());
    }

    @Override
    public String toString() {
        return "NetworkId(" +
                "addressByTransportTypeMap=" + addressByTransportTypeMap +
                ", pubKey=" + pubKey +
                ")";
    }
}

