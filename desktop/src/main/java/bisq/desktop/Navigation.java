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

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Navigation {
    public static void addListener(Listener listener, NavigationTarget... targets) {
        List.of(targets).forEach(navigationTarget -> {
            listeners.putIfAbsent(navigationTarget, new HashSet<>());
            listeners.get(navigationTarget).add(listener);
        });
    }

    public static void removeListener(Listener listener, NavigationTarget... targets) {
        List.of(targets).forEach(navigationTarget -> {
            if (listeners.containsKey(navigationTarget)) {
                listeners.get(navigationTarget).remove(listener);
            }
        });
    }

    public static void navigateTo(NavigationTarget target) {
        collectListeners(target).forEach(listener -> listener.onNavigate(target, Optional.empty()));
    }

    public static void navigateTo(NavigationTarget target, Object data) {
        collectListeners(target).forEach(listener -> listener.onNavigate(target, Optional.of(data)));
    }

    public interface Listener {
        void onNavigate(NavigationTarget target, Optional<Object> data);
    }

    private static Set<Listener> collectListeners(NavigationTarget target) {
        Set<NavigationTarget> targets = new HashSet<>(target.getPath());
        targets.add(target);
        return targets.stream()
                .filter(listeners::containsKey)
                .flatMap(e -> listeners.get(e).stream())
                .collect(Collectors.toSet());
    }

    private static final Map<NavigationTarget, Set<Listener>> listeners = new HashMap<>();

}