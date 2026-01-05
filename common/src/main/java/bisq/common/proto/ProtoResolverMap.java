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

@Slf4j
public abstract class ProtoResolverMap<T extends Proto> {
    public ProtoResolverMap() {
    }

    private final Map<String, ProtoResolver<T>> map = new HashMap<>();

    public void addProtoResolver(String protoTypeName, ProtoResolver<T> resolver) {
        map.put(protoTypeName, resolver);
    }

    public T fromAny(Any anyProto) {
        String protoTypeName = ProtobufUtils.getProtoType(anyProto);
        ProtoResolver<T> resolver = map.get(protoTypeName);

        if (resolver == null) {
            log.error("resolver is null. protoTypeName={}", protoTypeName);
            throw new UnresolvableProtobufMessageException(anyProto);
        }

        try {
            return resolver.fromAny(anyProto);
        } catch (Exception e) {
            log.error("Could not resolve proto {}. protoTypeName={}", anyProto, protoTypeName, e);
            throw new UnresolvableProtobufMessageException(anyProto, e);
        }
    }
}
