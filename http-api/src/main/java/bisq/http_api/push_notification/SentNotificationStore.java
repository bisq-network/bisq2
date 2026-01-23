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

package bisq.http_api.push_notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stores notification IDs that have been sent to prevent duplicate notifications.
 * Each notification is identified by: userProfileId + tradeId + eventType
 * Also tracks timestamps to enable purging of old notifications.
 */
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SentNotificationStore implements Serializable {
    private static final long serialVersionUID = 1L;

    @Getter
    private final Map<String, Long> sentNotificationIdsWithTimestamp;

    // Clock for time-based operations (injectable for testing)
    private final Clock clock;

    public SentNotificationStore() {
        this(new ConcurrentHashMap<>(), null, Clock.systemUTC());
    }

    /**
     * Constructor for testing with a custom clock.
     */
    public SentNotificationStore(Clock clock) {
        this(new ConcurrentHashMap<>(), null, clock);
    }

    /**
     * Constructor that handles both new format (Map) and legacy format (Set).
     * Jackson will call this with whichever property exists in the JSON.
     */
    @JsonCreator
    public SentNotificationStore(
            @JsonProperty("sentNotificationIdsWithTimestamp") Map<String, Long> sentNotificationIdsWithTimestamp,
            @JsonProperty("sentNotificationIds") Set<String> legacySentNotificationIds) {
        this(sentNotificationIdsWithTimestamp, legacySentNotificationIds, Clock.systemUTC());
    }

    /**
     * Internal constructor with clock injection.
     */
    private SentNotificationStore(
            Map<String, Long> sentNotificationIdsWithTimestamp,
            Set<String> legacySentNotificationIds,
            Clock clock) {

        this.clock = clock;
        this.sentNotificationIdsWithTimestamp = new ConcurrentHashMap<>();

        // Handle new format
        if (sentNotificationIdsWithTimestamp != null) {
            this.sentNotificationIdsWithTimestamp.putAll(sentNotificationIdsWithTimestamp);
            log.debug("Loaded {} notifications from new format", sentNotificationIdsWithTimestamp.size());
        }
        // Handle legacy format (backward compatibility)
        else if (legacySentNotificationIds != null) {
            long now = clock.millis();
            legacySentNotificationIds.forEach(id -> this.sentNotificationIdsWithTimestamp.put(id, now));
            log.info("Migrated {} notifications from legacy format to new format", legacySentNotificationIds.size());
        }
    }

    /**
     * Create a unique notification ID from the components.
     */
    public static String createNotificationId(String userProfileId, String tradeId, String eventType) {
        return userProfileId + ":" + tradeId + ":" + eventType;
    }

    /**
     * Check if a notification has already been sent.
     */
    public boolean wasNotificationSent(String userProfileId, String tradeId, String eventType) {
        String notificationId = createNotificationId(userProfileId, tradeId, eventType);
        return sentNotificationIdsWithTimestamp.containsKey(notificationId);
    }

    /**
     * Mark a notification as sent with current timestamp from the clock.
     */
    public void markNotificationAsSent(String userProfileId, String tradeId, String eventType) {
        String notificationId = createNotificationId(userProfileId, tradeId, eventType);
        sentNotificationIdsWithTimestamp.put(notificationId, clock.millis());
    }

    /**
     * Remove all notifications for a specific trade (e.g., when trade is removed).
     */
    public void removeNotificationsForTrade(String tradeId) {
        Set<String> toRemove = sentNotificationIdsWithTimestamp.keySet().stream()
                .filter(id -> id.contains(":" + tradeId + ":"))
                .collect(Collectors.toSet());
        toRemove.forEach(sentNotificationIdsWithTimestamp::remove);
        if (!toRemove.isEmpty()) {
            log.debug("Removed {} notification(s) for trade {}", toRemove.size(), tradeId);
        }
    }

    /**
     * Remove notifications older than the specified age in milliseconds.
     * This prevents unbounded growth of the store.
     *
     * @param maxAgeMs Maximum age in milliseconds (e.g., 90 days)
     * @return Number of notifications removed
     */
    public int purgeOldNotifications(long maxAgeMs) {
        long cutoffTime = clock.millis() - maxAgeMs;
        Set<String> toRemove = sentNotificationIdsWithTimestamp.entrySet().stream()
                .filter(entry -> entry.getValue() < cutoffTime)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        toRemove.forEach(sentNotificationIdsWithTimestamp::remove);

        if (!toRemove.isEmpty()) {
            log.info("Purged {} old notification(s) older than {} days",
                    toRemove.size(), maxAgeMs / (1000 * 60 * 60 * 24));
        }

        return toRemove.size();
    }

    /**
     * Clear all sent notifications (useful for testing or reset).
     */
    public void clear() {
        sentNotificationIdsWithTimestamp.clear();
    }

    /**
     * Get the number of tracked notifications.
     */
    public int size() {
        return sentNotificationIdsWithTimestamp.size();
    }
}

