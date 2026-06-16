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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public final class ViewLifecycleObservers {
    private static final List<ViewLifecycleObserver> observers = new CopyOnWriteArrayList<>();

    private ViewLifecycleObservers() {
    }

    public static AutoCloseable register(ViewLifecycleObserver observer) {
        observers.add(observer);
        return () -> observers.remove(observer);
    }

    static void onViewAttached(View<?, ?, ?> view) {
        observers.forEach(observer -> notifyObserver(observer, view, ViewLifecycleObserver::onViewAttached));
    }

    static void onViewDetached(View<?, ?, ?> view) {
        observers.forEach(observer -> notifyObserver(observer, view, ViewLifecycleObserver::onViewDetached));
    }

    private static void notifyObserver(ViewLifecycleObserver observer,
                                       View<?, ?, ?> view,
                                       ObserverNotification notification) {
        try {
            notification.notify(observer, view);
        } catch (Exception e) {
            log.warn("View lifecycle observer failed for {}", view.getClass().getName(), e);
        }
    }

    @FunctionalInterface
    private interface ObserverNotification {
        void notify(ViewLifecycleObserver observer, View<?, ?, ?> view);
    }
}
