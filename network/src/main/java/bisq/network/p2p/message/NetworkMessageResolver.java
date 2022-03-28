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

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.ProtoResolverMap;
import com.google.protobuf.Any;

public class NetworkMessageResolver {
    private static final ProtoResolverMap<NetworkMessage> protoResolverMap = new ProtoResolverMap<>();

    public static void addResolver(ProtoResolver<NetworkMessage> resolver) {
        addResolver(ProtoResolver.getModuleName(resolver), resolver);
    }

    public static void addResolver(String moduleName, ProtoResolver<NetworkMessage> resolver) {
        protoResolverMap.addProtoResolver(moduleName, resolver);
    }

    static NetworkMessage resolve(Any any) {
        return protoResolverMap.resolve(any);
    }
}