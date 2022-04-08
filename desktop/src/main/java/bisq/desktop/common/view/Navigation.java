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

package bisq.desktop.common.view;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Navigation {

  /*  public interface Listener {
        void onNavigate2(NavigationTarget navigationTarget, Optional<Object> data);
    }*/

    private static final Map<NavigationTarget, Set<NavigationController>> listeners = new ConcurrentHashMap<>();
    // navigationControllerListeners are called first
   // private static final Map<NavigationTarget, Set<NavigationController>> navigationControllerListeners = new ConcurrentHashMap<>();
    private static final LinkedList<NavigationTarget> history = new LinkedList<>();
    private static final LinkedList<NavigationTarget> alt = new LinkedList<>();

     static void addListener(NavigationTarget host, NavigationController listener) {
        listeners.putIfAbsent(host, new CopyOnWriteArraySet<>());
        listeners.get(host).add(listener);
    }

     static void removeListener(NavigationTarget host, NavigationController listener) {
        Optional.ofNullable(listeners.get(host)).ifPresent(set -> set.remove(listener));
    }

/*    public static void addNavigationControllerListener(NavigationTarget host, NavigationController listener) {
        navigationControllerListeners.putIfAbsent(host, new CopyOnWriteArraySet<>());
        navigationControllerListeners.get(host).add(listener);
    }

    public static void removeNavigationControllerListener(NavigationTarget host, NavigationController listener) {
        Optional.ofNullable(navigationControllerListeners.get(host)).ifPresent(set -> set.remove(listener));
    }*/

    public static void navigateTo(NavigationTarget navigationTarget) {
        history.add(navigationTarget);
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate2(navigationTarget, Optional.empty()));
            }
        });
    }

    // If data is passed we don't add it to the history as we would need to store the data as well, and it could be 
    // stale anyway at a later moment.  
    public static void navigateTo(NavigationTarget navigationTarget, Object data) {
        listeners.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.onNavigate2(navigationTarget, Optional.of(data)));
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
                value.forEach(l -> l.onNavigate2(navigationTarget, Optional.empty()));
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
                value.forEach(l -> l.onNavigate2(navigationTarget, Optional.empty()));
            }
        });
    }


}