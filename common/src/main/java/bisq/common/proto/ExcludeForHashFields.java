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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Names of the fields annotated with {@link ExcludeForHash}, resolved by reflection once per class, as
 * serializeForHash() runs for every received, deduplicated and persisted item.
 * The result also depends on the instance's version, so lookups take both.
 */
final class ExcludeForHashFields {
    // Not a ClassValue: Android provides it only from API 34, and the mobile node app supports API 33
    private static final Map<Class<?>, ExcludeForHashFields> BY_CLASS = new ConcurrentHashMap<>();

    private final Set<String> excludedInAllVersions;
    private final Map<Integer, Set<String>> excludedByVersion;

    static Set<String> get(Class<?> clazz, int version) {
        // computeIfAbsent locks the bin on a hit unless the entry is the bin's first node, get never locks
        ExcludeForHashFields fields = BY_CLASS.get(clazz);
        if (fields == null) {
            fields = BY_CLASS.computeIfAbsent(clazz, ExcludeForHashFields::new);
        }
        return fields.forVersion(version);
    }

    private ExcludeForHashFields(Class<?> clazz) {
        Set<String> excludedInAllVersions = new HashSet<>();
        Map<Integer, Set<String>> excludedOnlyInVersion = new HashMap<>();
        for (Field field : Proto.getAllDeclaredFields(clazz)) {
            ExcludeForHash annotation = field.getAnnotation(ExcludeForHash.class);
            if (annotation == null) {
                continue;
            }
            int[] excludeOnlyInVersions = annotation.excludeOnlyInVersions();
            if (excludeOnlyInVersions.length == 0) {
                excludedInAllVersions.add(field.getName());
            }
            for (int version : excludeOnlyInVersions) {
                excludedOnlyInVersion.computeIfAbsent(version, key -> new HashSet<>()).add(field.getName());
            }
        }

        Map<Integer, Set<String>> excludedByVersion = new HashMap<>();
        excludedOnlyInVersion.forEach((version, fieldNames) -> {
            Set<String> excluded = new HashSet<>(excludedInAllVersions);
            excluded.addAll(fieldNames);
            excludedByVersion.put(version, Set.copyOf(excluded));
        });
        this.excludedInAllVersions = Set.copyOf(excludedInAllVersions);
        this.excludedByVersion = Map.copyOf(excludedByVersion);
    }

    private Set<String> forVersion(int version) {
        return excludedByVersion.getOrDefault(version, excludedInAllVersions);
    }
}
