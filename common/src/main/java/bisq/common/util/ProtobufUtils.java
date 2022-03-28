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

import bisq.common.proto.ProtoPackageAndMessageName;
import com.google.common.base.Enums;
import com.google.protobuf.Any;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class ProtobufUtils {
    @Nullable
    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType, String name) {
        String enumName = name != null ? name : "UNDEFINED";
        E result = Enums.getIfPresent(enumType, enumName).orNull();
        if (result == null) {
            result = Enums.getIfPresent(enumType, "UNDEFINED").orNull();
            log.warn("We try to lookup for an enum entry with name 'UNDEFINED' and use that if available, " +
                    "otherwise the enum is null. enum={}", result);
            return result;
        }
        return result;
    }

    //todo remove
    public static ProtoPackageAndMessageName getProtoPackageAndMessageName(Any any) {
        // Convention:
        // We use the module name as proto package name and the java class name as the proto message name.
        // E.g. protoFQN = "offer.Offer"
        String protoFQN = any.getTypeUrl().split("/")[1];
        String protoPackage = protoFQN.split("\\.")[0];
        String protoMessageName = protoFQN.split("\\.")[1];
        // For the generated java package structure we use:
        // bisq.[protoPackage].protobuf.[protoMessageName]
        return new ProtoPackageAndMessageName(protoPackage, protoMessageName);
    }

    public static String getProtoType(Any any) {
        return any.getTypeUrl().split("/")[1];
    }
}