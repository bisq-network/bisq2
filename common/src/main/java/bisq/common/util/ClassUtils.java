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

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class ClassUtils {

    public static <T> Optional<T> safeCast(Object object, Class<T> clazz) {
        if (clazz.isInstance(object)) {
            try {
                //noinspection unchecked
                T casted = (T) object;
                return Optional.of(casted);
            } catch (ClassCastException t) {
                log.error("Could not cast object {} to class {}", object, clazz);
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static String getClassName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null) {
            name = enclosingClass.getSimpleName() + "." + name;
        }
        return name;
    }
}