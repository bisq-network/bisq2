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

package bisq.desktop;

import bisq.desktop.common.view.Controller;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Navigation {

    public interface Listener extends Controller {
        void onNavigate(NavigationTarget navigationTarget, Optional<Object> data);
    }

    private static final Map<NavigationTarget, Set<Listener>> listeners = new ConcurrentHashMap<>();
    private static final LinkedList<NavigationTarget> history = new LinkedList<>();
    private static final LinkedList<NavigationTarget> alt = new LinkedList<>();

    public static void addListener(NavigationTarget host, Listener listener) {
        listeners.putIfAbsent(host, new CopyOnWriteArraySet<>());
        listeners.get(host).add(listener);
    }

    public static void removeListener(NavigationTarget host, Listener listener) {
        Optional.ofNullable(listeners.get(host)).ifPresent(set -> set.remove(listener));
    }

    public static void navigateTo(NavigationTarget navigationTarget) {
        history.add(navigationTarget);
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate(navigationTarget, Optional.empty()));
            }
        });
    }

    // If data is passed we don't add it to the history as we would need to store the data as well, and it could be 
    // stale anyway at a later moment.  
    public static void navigateTo(NavigationTarget navigationTarget, Object data) {
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate(navigationTarget, Optional.of(data)));
            }
        });
    }

    public static void back() {
        if (history.isEmpty()) {
            return;
        }
        NavigationTarget navigationTarget = history.pollLast();
        alt.add(navigationTarget);
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate(navigationTarget, Optional.empty()));
            }
        });
    }

    public static void forth() {
        if (alt.isEmpty()) {
            return;
        }
        NavigationTarget navigationTarget = alt.pollLast();
        history.add(navigationTarget);
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate(navigationTarget, Optional.empty()));
            }
        });
    }


}