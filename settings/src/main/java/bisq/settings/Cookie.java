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

package bisq.settings;

import bisq.common.data.Pair;
import bisq.common.proto.PersistableProto;
import bisq.settings.protobuf.CookieMapEntry;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serves as flexible container for persisting UI states, layout,...
 * Should not be over-used for domain specific data where type safety and data integrity is important.
 * Does not support observable properties.
 */
@Slf4j
public final class Cookie implements PersistableProto {
    private final Map<CookieMapKey, String> map = new ConcurrentHashMap<>();

    public Cookie() {
    }

    private Cookie(Map<CookieMapKey, String> map) {
        this.map.putAll(map);
    }

    @Override
    public bisq.settings.protobuf.Cookie.Builder getBuilder(boolean serializeForHash) {
        return bisq.settings.protobuf.Cookie.newBuilder()
                .addAllCookieMapEntries(map.entrySet().stream()
                        .map(entry -> {
                            CookieMapKey cookieMapKey = entry.getKey();
                            CookieMapEntry.Builder builder = CookieMapEntry.newBuilder()
                                    .setKey(cookieMapKey.getCookieKey().name())
                                    .setValue(entry.getValue());
                            cookieMapKey.getSubKey().ifPresent(builder::setSubKey);
                            return builder.build();
                        })
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.settings.protobuf.Cookie toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    static Cookie fromProto(bisq.settings.protobuf.Cookie proto) {
        try {
            return new Cookie(proto.getCookieMapEntriesList().stream()
                    .map(entry -> {
                        try {
                            Optional<String> subKey = entry.hasSubKey() ? Optional.of(entry.getSubKey()) : Optional.empty();
                            // CookieMapKey.fromProto might fail...if so we ignore it
                            CookieMapKey cookieMapKey = CookieMapKey.fromProto(entry.getKey(), subKey);
                            String value = entry.getValue();
                            return new Pair<>(cookieMapKey, value);
                        } catch (Exception e) {
                            log.warn("CookieMapKey could not be resolved. We ignore the entry.", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
        } catch (Exception e) {
            log.error("Reading cookie from protobuf failed. We create a empty cookie instead.", e);
            return new Cookie();
        }
    }

    public Optional<String> asString(CookieKey key) {
        return asString(key, null);
    }

    public Optional<String> asString(CookieKey key, @Nullable String subKey) {
        return Optional.ofNullable(map.get(new CookieMapKey(key, subKey)));
    }

    public Optional<Double> asDouble(CookieKey key) {
        return asString(key)
                .flatMap(stringValue -> {
                    try {
                        return Optional.of(Double.parseDouble(stringValue));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }).stream().findAny();
    }

    public Optional<Double> asDouble(CookieKey key, String subKey) {
        return asString(key, subKey)
                .flatMap(stringValue -> {
                    try {
                        return Optional.of(Double.parseDouble(stringValue));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                }).stream().findAny();
    }

    public Optional<Boolean> asBoolean(CookieKey key) {
        return asString(key).map(stringValue -> stringValue.equals("1"));
    }

    public Optional<Boolean> asBoolean(CookieKey key, String subKey) {
        return asString(key, subKey)
                .map(stringValue -> stringValue.equals("1"));
    }

    void putAsString(CookieKey key, String value) {
        putAsString(key, null, value);
    }

    void putAsString(CookieKey key, @Nullable String subKey, String value) {
        map.put(new CookieMapKey(key, subKey), value);
    }

    void putAsBoolean(CookieKey key, boolean value) {
        putAsBoolean(key, null, value);
    }

    void putAsBoolean(CookieKey key, @Nullable String subKey, boolean value) {
        putAsString(key, subKey, value ? "1" : "0");
    }

    void putAsDouble(CookieKey key, double value) {
        putAsDouble(key, null, value);
    }

    void putAsDouble(CookieKey key, @Nullable String subKey, double value) {
        putAsString(key, subKey, String.valueOf(value));
    }

    void remove(CookieKey key) {
        remove(key, null);
    }

    void remove(CookieKey key, @Nullable String subKey) {
        map.remove(new CookieMapKey(key, subKey));
    }

    void putAll(Map<CookieMapKey, String> map) {
        map.forEach((key, value) -> putAsString(key.getCookieKey(), key.getSubKey().orElse(null), value));
    }

    Map<CookieMapKey, String> getMap() {
        return map;
    }
}
