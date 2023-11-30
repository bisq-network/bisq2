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
import bisq.presentation.notifications.linux.LinuxNotificationSender;
import bisq.presentation.notifications.osx.OsxNotificationSender;
import bisq.presentation.notifications.other.AwtNotificationSender;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class NotificationsService implements PersistenceClient<NotificationsStore>, Service {
    private static final long MAX_AGE = TimeUnit.DAYS.toMillis(30);

    public interface Subscriber {
        void onChanged(String notificationId);
    }

    @Getter
    private final NotificationsStore persistableStore = new NotificationsStore();
    @Getter
    private final Persistence<NotificationsStore> persistence;
    private NotificationSender delegate;
    private final Set<Subscriber> subscribers = new HashSet<>();

    // We do not persist the state of a closed notification panel as we prefer to show the panel again at restart.
    // If any new notification gets added the panel will also be shown again.
    @Getter
    private final Observable<Boolean> isNotificationPanelDismissed = new Observable<>(false);

    public NotificationsService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, persistableStore);
    }

    @Override
    public NotificationsStore prunePersisted(NotificationsStore persisted) {
        long pruneDate = System.currentTimeMillis() - MAX_AGE;
        Map<String, DateAndConsumedFlag> pruned = persisted.getNotificationIdMap().entrySet().stream()
                .filter(entry -> entry.getValue().getDate() > pruneDate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        persisted.getNotificationIdMap().clear();
        persisted.getNotificationIdMap().putAll(pruned);
        return persisted;
    }

    private Map<String, DateAndConsumedFlag> prune(Map<String, DateAndConsumedFlag> dateByNotificationId) {
        long pruneDate = System.currentTimeMillis() - MAX_AGE;
        return dateByNotificationId.entrySet().stream()
                .filter(entry -> entry.getValue().getDate() > pruneDate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void sendNotification(String notificationId, String title, String message) {
        getNotificationsDelegate().sendNotification(title, message);
    }

    public boolean containsNotificationId(String notificationId) {
        return getNotificationIdMap().containsKey(notificationId);
    }

    public void addNotificationId(String notificationId) {
        synchronized (persistableStore) {
            if (!containsNotificationId(notificationId)) {
                getNotificationIdMap().put(notificationId,
                        new DateAndConsumedFlag(System.currentTimeMillis(), false));
                subscribers.forEach(subscriber -> subscriber.onChanged(notificationId));
                persist();
            }
        }
    }

    public void consumeNotificationId(String notificationId) {
        synchronized (persistableStore) {
            if (containsNotificationId(notificationId) &&
                    !getNotificationIdMap().get(notificationId).isConsumed()) {
                getNotificationIdMap().get(notificationId).setConsumed(true);
                subscribers.forEach(subscriber -> subscriber.onChanged(notificationId));
                persist();
            }
        }
    }

    public void removeNotificationId(String notificationId) {
        synchronized (persistableStore) {
            DateAndConsumedFlag previous = getNotificationIdMap().remove(notificationId);
            if (previous != null) {
                subscribers.forEach(subscriber -> subscriber.onChanged(notificationId));
                persist();
            }
        }
    }

    public Set<String> getNotConsumedNotificationIds() {
        return getNotificationIdMap().entrySet().stream()
                .filter(entry -> !entry.getValue().isConsumed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllNotificationIds() {
        return getNotificationIdMap().keySet();
    }

    private NotificationSender getNotificationsDelegate() {
        if (delegate == null) {
            if (OsUtils.getOperatingSystem() == OperatingSystem.LINUX &&
                    LinuxNotificationSender.isSupported()) {
                delegate = new LinuxNotificationSender();
            } else if (OsUtils.getOperatingSystem() == OperatingSystem.MAC &&
                    OsxNotificationSender.isSupported()) {
                delegate = new OsxNotificationSender();
            } else {
                delegate = new AwtNotificationSender();
            }
        }
        return delegate;
    }

    public void subscribe(Subscriber subscriber) {
        subscribers.add(subscriber);
        getNotConsumedNotificationIds().forEach(subscriber::onChanged);
    }

    public void unsubscribe(Subscriber subscriber) {
        subscribers.remove(subscriber);
    }

    private Map<String, DateAndConsumedFlag> getNotificationIdMap() {
        return persistableStore.getNotificationIdMap();
    }
}
