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

import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
public class Navigation {
    private static final Map<NavigationTarget, Set<NavigationController>> navigationControllers = new ConcurrentHashMap<>();
    private static final LinkedList<NavigationTarget> history = new LinkedList<>();
    private static final LinkedList<NavigationTarget> alt = new LinkedList<>();
    private static SettingsService settingsService;
    @Getter
    private static Optional<NavigationTarget> persistedNavigationTarget = Optional.empty();

    public static void init(SettingsService settingsService) {
        Navigation.settingsService = settingsService;
    }

    public static void applyPersisted(NavigationTarget persistedNavigationTarget) {
        Navigation.persistedNavigationTarget = Optional.ofNullable(persistedNavigationTarget);
    }

    static void addNavigationController(NavigationTarget host, NavigationController listener) {
        navigationControllers.putIfAbsent(host, new CopyOnWriteArraySet<>());
        navigationControllers.get(host).add(listener);
    }

    static void removeNavigationController(NavigationTarget host, NavigationController listener) {
        Optional.ofNullable(navigationControllers.get(host)).ifPresent(set -> set.remove(listener));
    }

    public static void navigateTo(NavigationTarget navigationTarget) {
        history.add(navigationTarget);
        navigationControllers.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.processNavigationTarget(navigationTarget, Optional.empty()));
            }
        });
    }

    static void persistNavigationTarget(NavigationTarget navigationTarget) {
        if (navigationTarget.isAllowPersistence()) {
            settingsService.setCookie(CookieKey.NAVIGATION_TARGET, navigationTarget.name());
        }
    }

    // If data is passed we don't add it to the history as we would need to store the data as well, and it could be 
    // stale anyway at a later moment.  
    public static void navigateTo(NavigationTarget navigationTarget, Object data) {
        navigationControllers.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.processNavigationTarget(navigationTarget, Optional.of(data)));
            }
        });
    }

    public static void back() {
        if (history.isEmpty()) {
            return;
        }
        NavigationTarget navigationTarget = history.pollLast();
        alt.add(navigationTarget);
        navigationControllers.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.processNavigationTarget(navigationTarget, Optional.empty()));
            }
        });
    }

    public static void forth() {
        if (alt.isEmpty()) {
            return;
        }
        NavigationTarget navigationTarget = alt.pollLast();
        history.add(navigationTarget);
        navigationControllers.forEach((key, value) -> {
            if (navigationTarget.getPath().contains(key)) {
                value.forEach(l -> l.processNavigationTarget(navigationTarget, Optional.empty()));
            }
        });
    }
}