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
import bisq.common.util.OperatingSystem;
import bisq.common.util.OsUtils;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.presentation.notifications.linux.LinuxNotifications;
import bisq.presentation.notifications.osx.OsxNotifications;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class NotificationsService implements PersistenceClient<NotificationsStore>, Service {
    @Getter
    private final NotificationsStore persistableStore = new NotificationsStore();
    @Getter
    private final Persistence<NotificationsStore> persistence;
    private NotificationsDelegate delegate;

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

    public void notify(String title, String message) {
        getNotificationsDelegate().notify(title, message);
    }

    public boolean contains(String id) {
        return persistableStore.getDateByMessageId().containsKey(id);
    }

    public void add(String id) {
        persistableStore.getDateByMessageId().put(id, System.currentTimeMillis());
        persist();
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
}
