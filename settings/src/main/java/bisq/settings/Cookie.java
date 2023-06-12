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

import bisq.common.proto.Proto;
import bisq.settings.protobuf.CookieMapEntry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serves as flexible container for persisting UI states, layout,...
 * Should not be over-used for domain specific data where type safety and data integrity is important.
 * Does not support observable properties.
 */
public final class Cookie implements Proto {
    private final Map<CookieKey, String> map = new HashMap<>();

    public Cookie() {
    }

    private Cookie(Map<CookieKey, String> map) {
        this.map.putAll(map);
    }

    @Override
    public bisq.settings.protobuf.Cookie toProto() {
        return bisq.settings.protobuf.Cookie.newBuilder()
                .addAllCookieMapEntries(map.entrySet().stream()
                        .map(entry -> CookieMapEntry.newBuilder()
                                .setCookieKey(entry.getKey().getKeyForProto())
                                .setValue(entry.getValue()).build())
                        .collect(Collectors.toList()))
                .build();
    }

    static Cookie fromProto(bisq.settings.protobuf.Cookie proto) {
        return new Cookie(proto.getCookieMapEntriesList().stream()
                .collect(Collectors.toMap(
                        entry -> CookieKey.fromProto(entry.getCookieKey()),
                        CookieMapEntry::getValue)));
    }


    public Optional<String> asString(CookieKey key) {
        return asString(key, null);
    }

    public Optional<String> asString(CookieKey key, @Nullable String subKey) {
        return Optional.ofNullable(map.get(key))
                .map(stringValue -> {
                    if (subKey != null && subKey.equals(key.getSubKey())) {
                        stringValue = stringValue.replace(key.getSubKey(), "");
                    }
                    return stringValue;
                });
    }

    public Optional<Double> asDouble(CookieKey key) {
        return asDouble(key, null);
    }

    public Optional<Double> asDouble(CookieKey key, @Nullable String subKey) {
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
        return asBoolean(key, null);
    }

    public Optional<Boolean> asBoolean(CookieKey key, @Nullable String subKey) {
        return asString(key, subKey)
                .map(stringValue -> stringValue.equals("1"));
    }

    void putAsString(CookieKey key, String value) {
        if (key.isUseSubKey()) {
            value = key.getSubKey() + value;
        }
        map.put(key, value);
    }

    void putAll(Map<CookieKey, String> map) {
        map.forEach(this::putAsString);
    }

    void putAsBoolean(CookieKey key, boolean value) {
        putAsString(key, value ? "1" : "0");
    }

    void putAsDouble(CookieKey key, double value) {
        putAsString(key, String.valueOf(value));
    }

    void remove(CookieKey key) {
        map.remove(key);
    }

    Map<CookieKey, String> getMap() {
        return map;
    }
}
