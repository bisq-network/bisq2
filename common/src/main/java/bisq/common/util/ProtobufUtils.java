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

import com.google.common.base.Enums;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class ProtobufUtils {
    @Nullable
    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name) {
        String enumName = name != null ? name : "UNDEFINED";
        E result = Enums.getIfPresent(enumType, enumName).orNull();
        if (result == null) {
            result = Enums.getIfPresent(enumType, "UNDEFINED").orNull();
            log.warn("Enum with name {} for enum class {} is not present. We try to lookup for an enum entry with name 'UNDEFINED' and use that if available, " +
                    "otherwise the enum is null. enum={}", name, enumType.getSimpleName(), result);
            return result;
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