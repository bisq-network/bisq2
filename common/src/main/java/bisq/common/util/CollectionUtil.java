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

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionUtil {
    @Nullable
    public static <T> T getRandomElement(Collection<T> collection) {
        // Got a weird exception with size = -1 (Hashset), so we use the size check instead of isEmpty.
        return collection.isEmpty() ?
                null :
                new ArrayList<>(collection).get(new Random().nextInt(collection.size()));
    }

    public static <T> List<T> toShuffledList(List<T> list) {
        Collections.shuffle(list);
        return list;
    }

    public static <T> List<T> toShuffledList(Stream<T> stream) {
        return toShuffledList(stream.collect(Collectors.toList()));
    }

    public static <T> List<T> toShuffledList(Set<T> set) {
        return toShuffledList(set.stream());
    }

    public static Stream<String> streamFromCsv(String addresses) {
        return Stream.of(addresses.split(",")).filter(Objects::nonNull).map(String::trim);
    }
}
