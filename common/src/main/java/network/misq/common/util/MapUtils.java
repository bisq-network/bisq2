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

package network.misq.common.util;

import network.misq.common.Disposable;

import java.util.Map;

public class MapUtils {
    public static void disposeAndRemove(String key, Map<String, ? extends Disposable> map) {
        if (map.containsKey(key)) {
            map.get(key).dispose();
            map.remove(key);
        }
    }

    public static void disposeAndRemoveAll(Map<String, ? extends Disposable> map) {
        map.keySet().forEach(Disposable::dispose);
        map.clear();
    }
}
