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
                        .map(e -> CookieMapEntry.newBuilder()
                                .setCookieKey(e.getKey().toProto())
                                .setValue(e.getValue()).build())
                        .collect(Collectors.toList()))
                .build();
    }

    static Cookie fromProto(bisq.settings.protobuf.Cookie proto) {
        return new Cookie(proto.getCookieMapEntriesList().stream()
                .collect(Collectors.toMap(
                        e -> CookieKey.fromProto(e.getCookieKey()),
                        CookieMapEntry::getValue)));
    }

    void put(CookieKey key, String value) {
        map.put(key, value);
    }

    void putAll(Map<CookieKey, String> map) {
        this.map.putAll(map);
    }

    @Nullable
    public String getValue(CookieKey key) {
        return map.get(key);
    }

    public Optional<Double> getAsOptionalDouble(CookieKey key) {
        try {
            return map.containsKey(key) ?
                    Optional.of(Double.parseDouble(map.get(key))) :
                    Optional.empty();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    public Optional<Boolean> getAsOptionalBoolean(CookieKey key) {
        return map.containsKey(key) ?
                Optional.of(map.get(key).equals("1")) :
                Optional.empty();
    }

    void putAsBoolean(CookieKey key, boolean value) {
        map.put(key, value ? "1" : "0");
    }

    void putAsDouble(CookieKey key, double value) {
        map.put(key, String.valueOf(value));
    }

    Map<CookieKey, String> getMap() {
        return map;
    }
}
