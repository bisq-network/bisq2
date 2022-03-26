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

package bisq.network.p2p.protobuf;

import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.ProtoPackageAndMessageName;
import bisq.common.util.ProtobufUtils;
import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import com.google.protobuf.Any;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class ProtoResolverRepository2<T extends Proto> {
    private static final Map<String, ProtoResolver<DistributedData>> distributedDataProtoResolverMap = new HashMap<>();
    private static final Map<String, ProtoResolver<NetworkMessage>> networkMessageProtoResolverMap = new HashMap<>();

    public static void addDistributedDataProtoResolver(String moduleName, ProtoResolver<DistributedData> resolver) {
        distributedDataProtoResolverMap.put(moduleName, resolver);
    }

    public static void addNetworkMessageProtoResolver(String moduleName, ProtoResolver<NetworkMessage> resolver) {
        networkMessageProtoResolverMap.put(moduleName, resolver);
    }

    public static DistributedData toDistributedData(Any any) {
        ProtoPackageAndMessageName protoPackageAndMessageName = ProtobufUtils.getProtoPackageAndMessageName(any);
        // We do not use reflection for security reasons
        String protoPackage = protoPackageAndMessageName.protoPackage();
        String protoMessageName = protoPackageAndMessageName.protoMessageName();
        return ProtoResolverRepository2.findModuleProtoResolver(protoPackage, distributedDataProtoResolverMap)
                .map(resolver -> resolver.resolve(any, protoMessageName))
                .orElseThrow(() -> new UnresolvableProtobufMessageException(any));
    }

    static NetworkMessage toNetworkMessage(Any any) {
        ProtoPackageAndMessageName protoPackageAndMessageName = ProtobufUtils.getProtoPackageAndMessageName(any);
        // We do not use reflection for security reasons
        String protoPackage = protoPackageAndMessageName.protoPackage();
        String protoMessageName = protoPackageAndMessageName.protoMessageName();
        return ProtoResolverRepository2.findModuleProtoResolver(protoPackage, networkMessageProtoResolverMap)
                .map(resolver -> resolver.resolve(any, protoMessageName))
                .orElseThrow(() -> new UnresolvableProtobufMessageException(any));
    }

    private static <T extends ProtoResolver> Optional<T> findModuleProtoResolver(String protoPackage,
                                                                                 Map<String, T> map) {
        return Optional.ofNullable(map.get(protoPackage));
    }

}
