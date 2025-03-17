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

package bisq.common.proto;

import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class PersistableProtoResolverMap<T extends PersistableProto> {
    public PersistableProtoResolverMap() {
    }

    private final Map<String, ProtoResolver<T>> map = new HashMap<>();

    public void addProtoResolver(String protoTypeName, ProtoResolver<T> resolver) {
        map.put(protoTypeName, resolver);
    }

    public T fromAny(Any anyProto) {
        String protoTypeName = ProtobufUtils.getProtoType(anyProto);
        return Optional.ofNullable(map.get(protoTypeName))
                .map(resolver -> resolver.fromAny(anyProto))
                .orElseThrow(() -> new UnresolvableProtobufMessageException(anyProto));
    }
}
