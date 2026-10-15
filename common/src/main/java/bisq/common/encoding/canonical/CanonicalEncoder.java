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

package bisq.common.encoding.canonical;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CanonicalEncoder {
    public static final CanonicalEncoder DEFAULT = new CanonicalEncoder();

    /**
     * Encodes the value with the schema. The encoding purpose applies to the whole preimage: it is passed to every
     * nested schema and, for a {@link CanonicalSchema.Builder#canonical canonical} field, to the
     * {@link Canonical#encodeCanonical(CanonicalEncoder, EncodingPurpose) encodeCanonical} of the nested object.
     */
    public <T> byte[] encode(T value, CanonicalSchema<T> schema, EncodingPurpose encodingPurpose) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(schema);
        Objects.requireNonNull(encodingPurpose);

        CanonicalWriter writer = new CanonicalWriter();
        schema.getFields().forEach(field -> writeField(writer, field, value, encodingPurpose));
        return writer.toByteArray();
    }

    private <T> void writeField(CanonicalWriter writer,
                                CanonicalSchema.Field<T> field,
                                T value,
                                EncodingPurpose encodingPurpose) {
        @Nullable
        Object fieldValue = field.getValue(value);

        switch (field.getType()) {
            case INT32:
                writer.writeInt32(field.getNumber(), (int) fieldValue);
                break;
            case UINT32:
                writer.writeUInt32(field.getNumber(), (int) fieldValue);
                break;
            case INT64:
                writer.writeInt64(field.getNumber(), (long) fieldValue);
                break;
            case SINT32:
                writer.writeSInt32(field.getNumber(), (int) fieldValue);
                break;
            case SINT64:
                writer.writeSInt64(field.getNumber(), (long) fieldValue);
                break;
            case DOUBLE:
                writer.writeDouble(field.getNumber(), (double) fieldValue);
                break;
            case BOOL:
                writer.writeBool(field.getNumber(), (boolean) fieldValue);
                break;
            case ENUM:
                writer.writeEnum(field.getNumber(), (CanonicalEnum) fieldValue);
                break;
            case STRING:
                writer.writeString(field.getNumber(), (String) fieldValue);
                break;
            case BYTES:
                writer.writeBytes(field.getNumber(), (byte[]) fieldValue);
                break;
            case COMPOSE:
                writeCompose(writer, field, fieldValue, encodingPurpose);
                break;
            case EXTEND:
                writeExtend(writer, field, fieldValue, encodingPurpose);
                break;
            case ONEOF:
                writeOneof(writer, field, fieldValue, encodingPurpose);
                break;
            case CANONICAL:
                writeCanonical(writer, field, fieldValue, encodingPurpose);
                break;
            case ENCODED_MESSAGE:
                writer.writeCompose(field.getNumber(), (byte[]) fieldValue);
                break;
            case REPEATED_COMPOSE:
                writeRepeatedCompose(writer, field, fieldValue, encodingPurpose);
                break;
            case PACKED_REPEATED_INT32:
                writePackedRepeatedInt32(writer, field, fieldValue);
                break;
            case REPEATED_STRING:
                writeRepeatedString(writer, field, fieldValue);
                break;
            case MAP:
                writeMap(writer, field, fieldValue, encodingPurpose);
                break;
            default:
                throw new IllegalStateException("Unsupported canonical field type " + field.getType() +
                        " for field " + field.getNumber());
        }
    }

    private <T> void writeCompose(CanonicalWriter writer,
                                  CanonicalSchema.Field<T> field,
                                  @Nullable Object fieldValue,
                                  EncodingPurpose encodingPurpose) {
        if (fieldValue != null) {
            writer.writeCompose(field.getNumber(),
                    encodeNested(fieldValue, Objects.requireNonNull(field.getSchema()), encodingPurpose));
        }
    }

    private <T> void writeExtend(CanonicalWriter writer,
                                 CanonicalSchema.Field<T> field,
                                 @Nullable Object fieldValue,
                                 EncodingPurpose encodingPurpose) {
        if (fieldValue != null) {
            writer.writeExtend(field.getNumber(),
                    encodeNested(fieldValue, Objects.requireNonNull(field.getSchema()), encodingPurpose));
        }
    }

    private <T> void writeOneof(CanonicalWriter writer,
                                CanonicalSchema.Field<T> field,
                                @Nullable Object fieldValue,
                                EncodingPurpose encodingPurpose) {
        if (fieldValue != null) {
            writer.writeOneof(field.getNumber(),
                    encodeNested(fieldValue, Objects.requireNonNull(field.getSchema()), encodingPurpose));
        }
    }

    private <T> void writeCanonical(CanonicalWriter writer,
                                    CanonicalSchema.Field<T> field,
                                    @Nullable Object fieldValue,
                                    EncodingPurpose encodingPurpose) {
        if (fieldValue != null) {
            writer.writeCompose(field.getNumber(), ((Canonical) fieldValue).encodeCanonical(this, encodingPurpose));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void writeRepeatedString(CanonicalWriter writer, CanonicalSchema.Field<T> field, Object fieldValue) {
        writer.writeRepeatedString(field.getNumber(), (List<String>) fieldValue);
    }

    private <T> void writeRepeatedCompose(CanonicalWriter writer,
                                          CanonicalSchema.Field<T> field,
                                          Object fieldValue,
                                          EncodingPurpose encodingPurpose) {
        List<?> values = (List<?>) Objects.requireNonNull(fieldValue);
        CanonicalSchema<?> schema = Objects.requireNonNull(field.getSchema());
        values.forEach(value -> writer.writeCompose(field.getNumber(), encodeNested(value, schema, encodingPurpose)));
    }

    @SuppressWarnings("unchecked")
    private <T> void writePackedRepeatedInt32(CanonicalWriter writer, CanonicalSchema.Field<T> field, Object fieldValue) {
        writer.writePackedRepeatedInt32(field.getNumber(), (List<Integer>) fieldValue);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void writeMap(CanonicalWriter writer,
                              CanonicalSchema.Field<T> field,
                              Object fieldValue,
                              EncodingPurpose encodingPurpose) {
        Map<?, ?> source = (Map<?, ?>) Objects.requireNonNull(fieldValue);
        if (source.isEmpty()) {
            return;
        }

        CanonicalSchema.MapEncoding mapEncoding = Objects.requireNonNull(field.getMapEncoding());
        // Cached bytes do not record the encoding purpose which produced them, and a map value can contain nested
        // objects with a purpose-specific encoding. The cache is therefore only used for the default purpose.
        @Nullable
        CanonicalMapEntryByteCache cache = encodingPurpose == EncodingPurpose.DEFAULT &&
                source instanceof CanonicalMapEntryByteCache ?
                (CanonicalMapEntryByteCache) source :
                null;
        if (cache != null) {
            byte[] encodedMap = cache.getEncodedMap(field);
            if (encodedMap != null) {
                writer.writeRawBytes(encodedMap);
                return;
            }
        }

        // After a full-map cache miss, cache-enabled maps use a temporary writer to contain only the map field bytes,
        // not the parent message. At the end we write the mapWriter bytes to the parent writer.
        CanonicalWriter mapWriter = cache == null ? writer : new CanonicalWriter();
        List<Map.Entry<?, ?>> entries = mapEncoding.getEntries((Map) source);
        mapEncoding.iterate(entries).forEachRemaining(entry -> {
            Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) entry;
            byte[] encodedEntry = cache == null ?
                    null :
                    cache.getEncodedMapEntry(mapEntry.getKey(), mapEntry.getValue());
            if (encodedEntry == null) {
                encodedEntry = encodeMapEntry(mapEntry, mapEncoding, encodingPurpose);
                if (cache != null) {
                    cache.putEncodedMapEntry(mapEntry.getKey(), mapEntry.getValue(), encodedEntry);
                }
            }
            mapWriter.writeMapEntry(field.getNumber(), encodedEntry);
        });
        if (cache != null) {
            byte[] encodedMap = mapWriter.toByteArray();
            cache.putEncodedMap(field, encodedMap);
            writer.writeRawBytes(encodedMap);
        }
    }

    private byte[] encodeMapEntry(Map.Entry<?, ?> entry,
                                  CanonicalSchema.MapEncoding<?, ?, ?, ?> mapEncoding,
                                  EncodingPurpose encodingPurpose) {
        CanonicalWriter writer = new CanonicalWriter();
        writeMapEntryValue(writer, 1, mapEncoding.getKeyType(), entry.getKey(), null, encodingPurpose);
        writeMapEntryValue(writer,
                2,
                mapEncoding.getValueType(),
                entry.getValue(),
                mapEncoding.getValueSchema(),
                encodingPurpose);
        return writer.toByteArray();
    }

    private void writeMapEntryValue(CanonicalWriter writer,
                                    int fieldNumber,
                                    CanonicalSchema.FieldType type,
                                    @Nullable Object fieldValue,
                                    @Nullable CanonicalSchema<?> schema,
                                    EncodingPurpose encodingPurpose) {
        Objects.requireNonNull(fieldValue, "Canonical map entry values must not be null");

        switch (type) {
            case INT32:
                writer.writeInt32Value(fieldNumber, (int) fieldValue);
                break;
            case UINT32:
                writer.writeUInt32Value(fieldNumber, (int) fieldValue);
                break;
            case INT64:
                writer.writeInt64Value(fieldNumber, (long) fieldValue);
                break;
            case DOUBLE:
                writer.writeDoubleValue(fieldNumber, (double) fieldValue);
                break;
            case BOOL:
                writer.writeBoolValue(fieldNumber, (boolean) fieldValue);
                break;
            case ENUM:
                if (fieldValue instanceof CanonicalEnum canonicalEnum) {
                    writer.writeEnumValue(fieldNumber, canonicalEnum.getCode());
                } else {
                    writer.writeEnumValue(fieldNumber, (int) fieldValue);
                }
                break;
            case STRING:
                writer.writeStringValue(fieldNumber, (String) fieldValue);
                break;
            case BYTES:
                writer.writeLengthDelimitedValue(fieldNumber, (byte[]) fieldValue);
                break;
            case COMPOSE:
                writer.writeLengthDelimitedValue(fieldNumber,
                        encodeNested(fieldValue, Objects.requireNonNull(schema), encodingPurpose));
                break;
            case EXTEND:
                writer.writeLengthDelimitedValue(fieldNumber,
                        encodeNested(fieldValue, Objects.requireNonNull(schema), encodingPurpose));
                break;
            case ONEOF:
            case CANONICAL:
            case ENCODED_MESSAGE:
            case SINT32:
            case SINT64:
            case PACKED_REPEATED_INT32:
            case REPEATED_COMPOSE:
            case REPEATED_STRING:
            case MAP:
                throw new IllegalArgumentException("Unsupported canonical map entry type " + type);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private byte[] encodeNested(Object value, CanonicalSchema<?> schema, EncodingPurpose encodingPurpose) {
        return encode(value, (CanonicalSchema) schema, encodingPurpose);
    }
}
