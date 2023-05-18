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

package bisq.presentation.notifications;


import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.util.OperatingSystem;
import bisq.common.util.OsUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.linux.LinuxNotifications;
import bisq.presentation.notifications.osx.OsxNotifications;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NotificationsService implements PersistenceClient<NotificationsStore>, Service {
    public interface Listener {
        void onNotificationSent(String notificationId, String title, String message);

        void onAdded(String notificationId);

        void onRemoved(String notificationId);
    }

    @Getter
    private final NotificationsStore persistableStore = new NotificationsStore();
    @Getter
    private final Persistence<NotificationsStore> persistence;
    private NotificationsDelegate delegate;
    @Getter
    private final Observable<Integer> numNotifications = new Observable<>();
    private final Set<Listener> listeners = new HashSet<>();

    public NotificationsService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void notify(String notificationId, String title, String message) {
        getNotificationsDelegate().notify(title, message);
        listeners.forEach(listener -> listener.onNotificationSent(notificationId, title, message));
    }

    public boolean contains(String notificationId) {
        return persistableStore.getDateByNotificationId().containsKey(notificationId);
    }

    public void add(String notificationId) {
        persistableStore.getDateByNotificationId().put(notificationId, System.currentTimeMillis());
        numNotifications.set(persistableStore.getDateByNotificationId().size());
        listeners.forEach(listener -> listener.onAdded(notificationId));
        persist();
    }

    public void remove(String notificationId) {
        persistableStore.getDateByNotificationId().remove(notificationId);
        numNotifications.set(persistableStore.getDateByNotificationId().size());
        listeners.forEach(listener -> listener.onRemoved(notificationId));
        persist();
    }

    public Set<String> getNotificationIds() {
        return persistableStore.getDateByNotificationId().keySet();
    }

    private NotificationsDelegate getNotificationsDelegate() {
        if (delegate == null) {
            if (OsUtils.getOperatingSystem() == OperatingSystem.LINUX &&
                    LinuxNotifications.isSupported()) {
                delegate = new LinuxNotifications();
            } else if (OsUtils.getOperatingSystem() == OperatingSystem.MAC &&
                    OsxNotifications.isSupported()) {
                delegate = new OsxNotifications();
            } else {
                delegate = new AwtNotifications();
            }
        }
        return delegate;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
