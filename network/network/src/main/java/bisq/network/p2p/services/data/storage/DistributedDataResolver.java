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

package bisq.network.p2p.services.data.storage;

import bisq.common.proto.NetworkProtoResolverMap;
import bisq.common.proto.ProtoResolver;
import com.google.protobuf.Any;

public class DistributedDataResolver {
    private static final NetworkProtoResolverMap<DistributedData> protoResolverMap = new NetworkProtoResolverMap<>();

    public static void addResolver(String protoTypeName, ProtoResolver<DistributedData> resolver) {
        protoResolverMap.addProtoResolver(protoTypeName, resolver);
    }

    static DistributedData fromAny(Any any) {
        return protoResolverMap.fromAny(any);
    }
}