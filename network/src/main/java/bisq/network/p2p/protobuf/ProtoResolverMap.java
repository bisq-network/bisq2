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
import com.google.protobuf.Any;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public class ProtoResolverMap<T extends Proto> {
    private final Map<String, ProtoResolver<T>> map = new HashMap<>();
            
    public void addProtoResolver(String moduleName, ProtoResolver<T> resolver) {
        map.put(moduleName, resolver);
    }

    public T resolve(Any any) {
        ProtoPackageAndMessageName protoPackageAndMessageName = ProtobufUtils.getProtoPackageAndMessageName(any);
        // We do not use reflection for security reasons
        String protoPackage = protoPackageAndMessageName.protoPackage();
        String protoMessageName = protoPackageAndMessageName.protoMessageName();
        return Optional.ofNullable(map.get(protoPackage))
                .map(resolver -> resolver.resolve(any, protoMessageName))
                .orElseThrow(() -> new UnresolvableProtobufMessageException(any));
    }
}
