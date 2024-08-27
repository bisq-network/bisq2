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

import com.google.common.base.Enums;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.*;

@Slf4j
public class ProtobufUtils {
    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name) {
        String info = "Enum type= " + enumType.getSimpleName() + "; name=" + name;
        checkNotNull(name, "Enum name must not be null. " + info);
        checkArgument(!name.endsWith("_UNSPECIFIED"), "Unspecified enum. " + info);

        //Remove prefix from enum name. Since enum is based on the enum's class name, we use that to extract the prefix
        String enumName = name.replace(ProtoEnum.getProtobufEnumPrefix(enumType), "");
        E result = Enums.getIfPresent(enumType, enumName).orNull();
        checkNotNull(result, "Enum could not be resolved. " + info);
        return result;
    }

    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name, E fallBack) {
        String info = "Enum type= " + enumType.getSimpleName() + "; name=" + name + "; fallBack=" + fallBack;
        if (name == null) {
            log.error("Enum name is null. We use the fallback value instead. {}", info);
            return fallBack;
        }
        if (name.endsWith("_UNSPECIFIED")) {
            log.warn("Unspecified enum. We use the fallback value instead. {}", info);
            return fallBack;
        }
        //Remove prefix from enum name. Since enum is based on the enum's class name, we use that to extract the prefix
        String enumName = name.replace(ProtoEnum.getProtobufEnumPrefix(enumType), "");
        E result = Enums.getIfPresent(enumType, enumName).orNull();
        if (result == null) {
            log.error("Enum could not be resolved. We use the fallback value instead. {}", info);
            return fallBack;
        }
        return result;
    }

    public static String getProtoType(Any any) {
        String[] tokens = any.getTypeUrl().split("/");
        return tokens.length > 1 ? tokens[1] : "";
    }

    public static Any toAny(byte[] bytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return Any.parseDelimitedFrom(inputStream);
        }
    }

    // Convert list of proto enums to a stream of Enums. If the enumFromProto fails we skip the enum.
    public static <E extends Enum<E>, P> Stream<E> fromProtoEnumStream(Class<E> enumType, List<P> proto) {
        return proto.stream()
                .map(enumProto -> {
                    try {
                        return enumFromProto(enumType, ((Enum<?>) enumProto).name());
                    } catch (Exception e) {
                        log.warn("Could not resolve enum for proto {}.", enumProto, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    public static <E extends Enum<E>, P> List<E> fromProtoEnumList(Class<E> enumType, List<P> proto) {
        return fromProtoEnumStream(enumType, proto).collect(Collectors.toList());
    }

    public static <E extends Enum<E>, P> Set<E> fromProtoEnumSet(Class<E> enumType, List<P> proto) {
        return fromProtoEnumStream(enumType, proto).collect(Collectors.toSet());
    }
}
