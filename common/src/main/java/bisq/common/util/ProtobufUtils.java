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

package bisq.common.util;

import bisq.common.proto.ProtoEnum;
import com.google.common.base.Enums;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class ProtobufUtils {
    @Nullable
    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name) {
        String info = "Enum type= " + enumType.getSimpleName() + "; name=" + name;
        checkNotNull(name, "Enum name must not be null. "+info);
        checkArgument(!name.endsWith("_UNSPECIFIED"), "Unspecified enum. "+info);

        //Remove prefix from enum name. Since enum is based on the enum's class name, we use that to extract the prefix
        String enumName = name.replace(ProtoEnum.getProtobufEnumPrefix(enumType),"");
        E result = Enums.getIfPresent(enumType, enumName).orNull();
        checkNotNull(result, "Enum could not be resolved. "+info);
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
        String enumName = name.replace(ProtoEnum.getProtobufEnumPrefix(enumType),"");
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

    public static byte[] getByteArrayFromProto(com.google.protobuf.Message proto) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            proto.writeDelimitedTo(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static Any toAny(byte[] bytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return Any.parseDelimitedFrom(inputStream);
        }
    }
}
