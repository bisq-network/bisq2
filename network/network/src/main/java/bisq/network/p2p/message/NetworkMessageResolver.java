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

package bisq.network.p2p.message;

import bisq.common.proto.NetworkProtoResolverMap;
import bisq.common.proto.NetworkStorageWhiteList;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StoragePolicyAware;
import bisq.common.proto.ProtoResolver;
import com.google.protobuf.Any;

public class NetworkMessageResolver {
    private static final NetworkProtoResolverMap<ExternalNetworkMessage> protoResolverMap = new NetworkProtoResolverMap<>();

    /**
     * Registers a concrete message type: its simple name becomes a store key if the message can be stored, so it is
     * added to the whitelist and, when it is a stored type, its storage policy is verified here rather than when the
     * first message of that type arrives. ExternalNetworkMessage does not imply a stored type, so a direct only
     * message declares no policy.
     */
    public static void addResolver(String protoTypeName,
                                   Class<? extends ExternalNetworkMessage> clazz,
                                   ProtoResolver<ExternalNetworkMessage> resolver) {
        NetworkStorageWhiteList.add(clazz);
        if (StoragePolicyAware.class.isAssignableFrom(clazz)) {
            MetaData.verifyStoragePolicyDeclared(clazz);
        }
        protoResolverMap.addProtoResolver(protoTypeName, resolver);
    }

    /**
     * Registers an abstract base which dispatches to its subclasses. The base is never a store key, so it is not
     * whitelisted and declares no policy of its own.
     */
    public static void addBaseTypeResolver(String protoTypeName, ProtoResolver<ExternalNetworkMessage> resolver) {
        protoResolverMap.addProtoResolver(protoTypeName, resolver);
    }

    static ExternalNetworkMessage fromAny(Any any) {
        return protoResolverMap.fromAny(any);
    }
}