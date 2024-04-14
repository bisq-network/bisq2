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

import bisq.common.annotation.ExcludeForHash;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface for any object which gets serialized using protobuf.
 * <p>
 * We require deterministic serialisation (e.g. used for hashes) for most data.
 * We need to ensure that Collections are deterministically sorted.
 * HashMap are not allowed as they do not guarantee that (even if Java have deterministic implementation for it as
 * in HashMap - there is no guarantee that all JVms will support that and non-Java implementations need to be able
 * to deal with it as well. Rust for instance randomize the key set in maps by default for security reasons).
 * If a map is needed we can use the TreeMap as it provides a deterministic order.
 */
public interface Proto {
    Message toProto();

    default Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    default Message toProto(boolean serializeForHash) {
        return buildProto(serializeForHash);
    }

    default <T extends Message> T buildProto(boolean serializeForHash) {
        var builder = serializeForHash ? clearAnnotatedFields(getBuilder(serializeForHash)) : getBuilder(serializeForHash);
        return (T) builder.build();
    }

    default byte[] serialize() {
        return buildProto(false).toByteArray();
    }

    default byte[] serializeForHash() {
        return buildProto(true).toByteArray();
    }

    default int getSerializedSize() {
        return buildProto(false).getSerializedSize();
    }

    default void writeDelimitedTo(OutputStream outputStream) throws IOException {
        toProto(true).writeDelimitedTo(outputStream);
    }

    default Set<String> getExcludedFields() {
        return Arrays.stream(getClass().getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .filter(field -> field.isAnnotationPresent(ExcludeForHash.class))
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Requires that the name of the java fields is the same as the name of the proto definition.
     *
     * @param builder The builder we transform by clearing the ExcludeForHash annotated fields.
     * @return Builder with the fields annotated with ExcludeForHash cleared.
     */
    default <B extends Message.Builder> B clearAnnotatedFields(B builder) {
        Set<String> excludedFields = getExcludedFields();
        getLogger().info("Clear fields in builder annotated with @ExcludeForHash: {}", excludedFields);
        for (Descriptors.FieldDescriptor fieldDesc : builder.getAllFields().keySet()) {
            if (excludedFields.contains(fieldDesc.getName())) {
                builder.clearField(fieldDesc);
            }
        }
        return (B) builder;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getSimpleName());
    }
}
