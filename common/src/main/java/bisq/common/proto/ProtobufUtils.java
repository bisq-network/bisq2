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

import bisq.common.util.ExceptionUtil;
import com.google.common.base.Enums;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ProtobufUtils {
    private static final int MAX_LOGGED_SKIPPED_ENTRIES = 3;
    private static final int MAX_LOGGED_STACK_TRACE_LINES = 10;

    // Parse exceptions can retain the entire partial message. Failures outlive parsing in futures and connection
    // close reasons, so discard those objects before propagating the failure. Keep the diagnostics and cause chain.
    public static void clearUnfinishedMessages(Throwable exception) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = exception; cause != null && visited.add(cause); cause = cause.getCause()) {
            if (cause instanceof InvalidProtocolBufferException parseException) {
                parseException.setUnfinishedMessage(null);
            }
        }
    }

    public static <E extends Enum<E>> E enumFromProto(Class<E> enumType,
                                                      String name) throws UnresolvableProtobufEnumException {
        try {

            String info = "Enum type= " + enumType.getSimpleName() + "; name=" + name;
            checkNotNull(name, "Enum name must not be null. " + info);
            checkArgument(!name.endsWith("_UNSPECIFIED"), "Unspecified enum. " + info);

            //Remove prefix from enum name. Since enum is based on the enum's class name, we use that to extract the prefix
            String enumName = name.replace(ProtoEnum.getProtobufEnumPrefix(enumType), "");
            E result = Enums.getIfPresent(enumType, enumName).orNull();
            checkNotNull(result, "Enum could not be resolved. " + info);
            return result;
        } catch (Exception e) {
            throw new UnresolvableProtobufEnumException(e);
        }
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

    /**
     * Converts all protos with the converter and returns the results in the order of the protos. Use it instead of
     * {@code protos.stream().map(Foo::fromProto)}, which does not compile with a checked exception. The conversion is
     * eager, so the first failure is thrown to the caller.
     */
    public static <P, T> Stream<T> fromProtoStream(Collection<P> protos,
                                                   ProtoConverter<P, T> converter)
                                                           throws UnresolvableProtobufException {
        List<T> results = new ArrayList<>(protos.size());
        for (P proto : protos) {
            results.add(converter.fromProto(proto));
        }
        return results.stream();
    }

    /**
     * Converts the values of a protobuf map with the converter and keeps the keys, in the order of the map. Use it
     * instead of a lambda like {@code e -> Foo.fromProto(e.getValue())} in a collector.
     */
    public static <K, P, T> Stream<Map.Entry<K, T>> fromProtoEntryStream(Map<K, P> protos,
                                                                          ProtoConverter<P, T> converter)
            throws UnresolvableProtobufException {
        List<Map.Entry<K, T>> results = new ArrayList<>(protos.size());
        for (Map.Entry<K, P> entry : protos.entrySet()) {
            results.add(Map.entry(entry.getKey(), converter.fromProto(entry.getValue())));
        }
        return results.stream();
    }

    /**
     * Converts all protos with the converter and skips each proto which it cannot convert, in the order of the protos.
     * Use it for a collection in which one invalid entry must not fail the others: the persisted entries of a store, so
     * that it loads the others (see docs/specifications/network/validation-at-parsing.md), or the reactions of a
     * received chat message. It logs at most three skipped entries, so that a peer which sends many invalid entries in
     * one message cannot flood the log.
     */
    public static <P, T> Stream<T> fromProtoStreamSkippingInvalid(Collection<P> protos,
                                                                ProtoConverter<P, T> converter) {
        List<T> results = new ArrayList<>(protos.size());
        int numSkipped = 0;
        for (P proto : protos) {
            try {
                results.add(converter.fromProto(proto));
            } catch (UnresolvableProtobufException | RuntimeException e) {
                SkippedEntryCounter.countSkippedEntry();
                logSkippedEntry(proto, e, ++numSkipped);
            }
        }
        logNumSkippedEntries(numSkipped, protos.size());
        return results.stream();
    }

    /**
     * Converts the values of a protobuf map with the converter, keeps the keys, and skips each entry whose value it
     * cannot convert. See {@link #fromProtoStreamSkippingInvalid}.
     */
    public static <K, P, T> Stream<Map.Entry<K, T>> fromProtoEntryStreamSkippingInvalid(
            Map<K, P> protos,
            ProtoConverter<P, T> converter) {
        List<Map.Entry<K, T>> results = new ArrayList<>(protos.size());
        int numSkipped = 0;
        for (Map.Entry<K, P> entry : protos.entrySet()) {
            try {
                results.add(Map.entry(entry.getKey(), converter.fromProto(entry.getValue())));
            } catch (UnresolvableProtobufException | RuntimeException e) {
                SkippedEntryCounter.countSkippedEntry();
                logSkippedEntry(entry.getValue(), e, ++numSkipped);
            }
        }
        logNumSkippedEntries(numSkipped, protos.size());
        return results.stream();
    }

    // Protos can hold private keys, payment account details or trade data, and exception messages can contain values
    // of them. Users share their log files, so we log only the type and the size of the proto, and the beginning of the
    // stack trace without messages (see UnresolvableProtobufMessageException).
    private static void logSkippedEntry(Object proto, Exception exception, int numSkipped) {
        if (numSkipped <= MAX_LOGGED_SKIPPED_ENTRIES) {
            String stackTrace = ExceptionUtil.getSafeStackTraceAsString(exception).lines()
                    .limit(MAX_LOGGED_STACK_TRACE_LINES)
                    .collect(Collectors.joining("\n"));
            log.error("We skip an entry which we cannot parse. {}, cause:\n{}", describe(proto), stackTrace);
        }
    }

    private static String describe(Object proto) {
        if (proto instanceof Message message) {
            return "type=" + message.getDescriptorForType().getFullName() + ", " + message.getSerializedSize() + " bytes";
        } else if (proto instanceof ProtocolMessageEnum && proto instanceof Enum<?> protoEnum) {
            // The name of an enum value is no private data. The number of an unknown value is not available.
            return "enum=" + protoEnum.getDeclaringClass().getSimpleName() + "." + protoEnum.name();
        } else {
            return "type=" + (proto == null ? "null" : proto.getClass().getSimpleName());
        }
    }

    private static void logNumSkippedEntries(int numSkipped, int numEntries) {
        if (numSkipped > MAX_LOGGED_SKIPPED_ENTRIES) {
            log.error("We skipped {} of {} entries which we cannot parse.", numSkipped, numEntries);
        }
    }

    // Convert list of proto enums to a stream of Enums. A value which cannot be resolved is skipped.
    public static <E extends Enum<E>, P> Stream<E> fromProtoEnumStream(Class<E> enumType, List<P> proto) {
        return resolveEnumNames(enumType, proto.stream().map(enumProto -> ((Enum<?>) enumProto).name()));
    }

    // Convert list of enum names (repeated string fields) to a list of Enums. A name which cannot be resolved is
    // skipped.
    public static <E extends Enum<E>> List<E> fromProtoEnumNameList(Class<E> enumType, List<String> names) {
        return resolveEnumNames(enumType, names.stream()).collect(Collectors.toList());
    }

    // A peer of a later version can send values which this version does not know, so a value which cannot be resolved
    // is expected and skipped. A list from a peer can hold millions of such values, for example the features of the
    // peers of a peer exchange message. So the values are resolved without exceptions, and one log line without the
    // values reports the number of skipped values (rule 8 of docs/specifications/network/validation-at-parsing.md).
    private static <E extends Enum<E>> Stream<E> resolveEnumNames(Class<E> enumType, Stream<String> names) {
        String prefix = ProtoEnum.getProtobufEnumPrefix(enumType);
        List<E> resolved = new ArrayList<>();
        int numSkipped = 0;
        for (String name : (Iterable<String>) names::iterator) {
            E result = name == null || name.endsWith("_UNSPECIFIED")
                    ? null
                    : Enums.getIfPresent(enumType, name.replace(prefix, "")).orNull();
            if (result == null) {
                numSkipped++;
            } else {
                resolved.add(result);
            }
        }
        if (numSkipped > 0) {
            log.debug("We skipped {} values of {} which could not be resolved.", numSkipped, enumType.getSimpleName());
        }
        return resolved.stream();
    }

    public static <E extends Enum<E>, P> List<E> fromProtoEnumList(Class<E> enumType, List<P> proto) {
        return fromProtoEnumStream(enumType, proto).collect(Collectors.toList());
    }

    public static <E extends Enum<E>, P> Set<E> fromProtoEnumSet(Class<E> enumType, List<P> proto) {
        return fromProtoEnumStream(enumType, proto).collect(Collectors.toSet());
    }
}
