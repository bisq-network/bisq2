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
import com.google.protobuf.Internal;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

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

    public static String getProtoType(Any any) {
        log.info("TypeUrl={}", any.getTypeUrl());
        String fullName = any.getTypeUrl().split("/")[1];
        String[] tokens = fullName.split("[.]");
        String protoTypeResolverName = fullName;
        if(tokens.length > 2) {
            protoTypeResolverName = tokens[1] + "." + tokens[tokens.length - 1];
        }
        log.info("PrototypeResolverName={}", protoTypeResolverName);
        return protoTypeResolverName;
    }

    public static <T extends MessageLite> Any pack(T message) {
        return Any.newBuilder()
                .setTypeUrl("type.googleapis.com/" + message.getClass().getName())
                .setValue(message.toByteString())
                .build();
    }

    public static <T extends MessageLite> T unpack(Any any, Class<T> clazz) throws InvalidProtocolBufferException {
        boolean invalidClazz = false;
        Object val = getCachedUnpackValue(any, clazz);//TODO really returns toProto
        if (val != null) {
            if (val.getClass() == clazz) {
                return (T) val;
            }
            invalidClazz = true;
        }

        if (!invalidClazz && isAny(any, clazz)) {
            return getResult(any, clazz);
        } else {
            throw new InvalidProtocolBufferException("Type of the Any message does not match the given class.");
        }
    }

    private static <T extends MessageLite> T getResult(Any any, Class<T> clazz) {
        T defaultInstance = (T) Internal.getDefaultInstance(clazz);
        T result = null;
        try {
            result = (T) defaultInstance.getParserForType().parseFrom(any.getValue());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        setCachedUnpackValue(any, result);
        return result;
    }

    private static <T extends MessageLite> void setCachedUnpackValue(Any any, T val) {
        //TODO really does nothing for now. The cachedUnpackValue will be a property
        //TODO of type MessageLite that that holds the parsed value for quick reference
    }

    private static <T extends MessageLite> T getCachedUnpackValue(Any any, Class<T> clazz) {
        //TODO First try to get the cached value already set before calling getResult
        return getResult(any, clazz);
    }

    private static Object invokeMethod(Object object, String methodName) {
        java.lang.reflect.Method method = null;
        Object val = null;
        try {
            method = object.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            val = method.invoke(object);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return val;
    }

    private static Object invokeMethod(Object object, String methodName, Object... args) {
        java.lang.reflect.Method method = null;
        Object val = null;
        try {
            method = object.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            val = method.invoke(object, args);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return val;
    }

    private static <T extends MessageLite> boolean isAny(Any any, Class<T> clazz) {
        T defaultInstance = Internal.getDefaultInstance(clazz);
        Object o = invokeMethod(invokeMethod(defaultInstance, "getDescriptorForType"),"getFullName");

        Object o1 = invokeMethod(any, "getTypeNameFromTypeUrl", any.getTypeUrl());
        return o1.equals(o);
    }
}