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

package bisq.common.timer;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Counts the events of each user profile ID in four time windows: the last second, minute, hour and day. An event
 * exceeds the limit if a window holds more events than its maximum.
 * <p>
 * Anybody can create user profile IDs, so the limiter keeps state only while it can change a decision. It removes an
 * ID without an event within the last day, and it keeps at most maxUserProfileIds IDs (see "Rate limit of chat
 * authors" in docs/specifications/security/derived-state.md).
 */
@Slf4j
public class RateLimiter {
    private final static long SECOND = 1000L;
    private final static long MINUTE = 60 * SECOND;
    private final static long HOUR = 60 * MINUTE;
    private final static long DAY = 24 * HOUR;

    public final static int DEFAULT_MAX_PER_SECOND = 3;
    public final static int DEFAULT_MAX_PER_MINUTE = 30;
    public final static int DEFAULT_MAX_PER_HOUR = 120;
    public final static int DEFAULT_MAX_PER_DAY = 300;
    // Far more than the authors of honest chat traffic of one day. At the limit, an ID with the newest events takes at
    // most about 7 KB, so the limiter takes at most about 70 MB.
    public final static int DEFAULT_MAX_USER_PROFILE_IDS = 10_000;
    // The limiter removes the IDs without an event within the last day at most once in this interval
    private final static long REMOVE_IDLE_INTERVAL = MINUTE;

    private final Clock clock;
    private final int maxPerSecond;
    private final int maxPerMinute;
    private final int maxPerHour;
    private final int maxPerDay;
    private final int maxUserProfileIds;
    // A window which holds more events than the highest maximum exceeds its maximum. So the newest (highest maximum + 1)
    // events of an ID decide as all of its events do.
    private final int maxTimestampsPerUserProfileId;
    // Guarded by this. In access order, so that the ID whose state was used least recently comes first. A priority queue
    // keeps the oldest event first, also for events which are not added in the order of their time.
    private final LinkedHashMap<String, PriorityQueue<Long>> timestampsByUserProfileId =
            new LinkedHashMap<>(16, 0.75f, true);
    private final Set<Consumer<String>> removedUserProfileIdListeners = new CopyOnWriteArraySet<>();
    // Guarded by this
    private long lastRemoveIdleTime;

    public RateLimiter() {
        this(DEFAULT_MAX_PER_SECOND, DEFAULT_MAX_PER_MINUTE, DEFAULT_MAX_PER_HOUR, DEFAULT_MAX_PER_DAY);
    }

    public RateLimiter(int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay) {
        this(new SystemClock(), maxPerSecond, maxPerMinute, maxPerHour, maxPerDay);
    }

    public RateLimiter(Clock clock, int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay) {
        this(clock, maxPerSecond, maxPerMinute, maxPerHour, maxPerDay, DEFAULT_MAX_USER_PROFILE_IDS);
    }

    @VisibleForTesting
    RateLimiter(Clock clock, int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay, int maxUserProfileIds) {
        checkArgument(maxUserProfileIds > 0, "maxUserProfileIds must be positive");
        this.clock = clock;
        this.maxPerSecond = maxPerSecond;
        this.maxPerMinute = maxPerMinute;
        this.maxPerHour = maxPerHour;
        this.maxPerDay = maxPerDay;
        this.maxUserProfileIds = maxUserProfileIds;
        long highestMaximum = Math.max(Math.max(maxPerSecond, maxPerMinute), Math.max(maxPerHour, maxPerDay));
        maxTimestampsPerUserProfileId = (int) Math.min(Integer.MAX_VALUE, highestMaximum + 1);
    }

    /**
     * The listener gets each user profile ID whose state the limiter removed: an ID without an event within the last
     * day, or the least recently used ID when a new ID exceeds maxUserProfileIds. The limiter calls the listener after
     * it has released its lock.
     */
    public void addRemovedUserProfileIdListener(Consumer<String> listener) {
        removedUserProfileIdListeners.add(listener);
    }

    public boolean exceedsLimit(String userProfileId) {
        return exceedsLimit(userProfileId, clock.now());
    }

    public boolean exceedsLimit(String userProfileId, long timeStamp) {
        return getExceedsLimitInfo(userProfileId, timeStamp).isPresent();
    }

    public Optional<String> getExceedsLimitInfo(String userProfileId) {
        return getExceedsLimitInfo(userProfileId, clock.now());
    }

    public Optional<String> getExceedsLimitInfo(String userProfileId, long timeStamp) {
        List<String> removedUserProfileIds = new ArrayList<>();
        Optional<String> exceedsLimitInfo;
        synchronized (this) {
            long now = clock.now();
            removeIdleUserProfileIds(now, removedUserProfileIds);

            PriorityQueue<Long> timestamps = timestampsByUserProfileId.get(userProfileId);
            if (timestamps == null) {
                if (timestampsByUserProfileId.size() >= maxUserProfileIds) {
                    removeLeastRecentlyUsedUserProfileId(removedUserProfileIds);
                }
                timestamps = new PriorityQueue<>();
                timestampsByUserProfileId.put(userProfileId, timestamps);
            }
            timestamps.add(timeStamp);
            removeEventsOlderThanOneDay(timestamps, now);
            while (timestamps.size() > maxTimestampsPerUserProfileId) {
                timestamps.poll();
            }

            exceedsLimitInfo = getExceedsLimitInfo(timestamps, now);
        }

        notifyRemovedUserProfileIdListeners(removedUserProfileIds);
        exceedsLimitInfo.ifPresent(log::info);
        return exceedsLimitInfo;
    }

    @VisibleForTesting
    synchronized int getNumUserProfileIds() {
        return timestampsByUserProfileId.size();
    }

    private Optional<String> getExceedsLimitInfo(PriorityQueue<Long> timestamps, long now) {
        long countLastSecond = timestamps.stream().filter(t -> now - t <= SECOND).count();
        long countLastMinute = timestamps.stream().filter(t -> now - t <= MINUTE).count();
        long countLastHour = timestamps.stream().filter(t -> now - t <= HOUR).count();
        long countLastDay = timestamps.stream().filter(t -> now - t <= DAY).count();
        if (countLastSecond > maxPerSecond ||
                countLastMinute > maxPerMinute ||
                countLastHour > maxPerHour ||
                countLastDay > maxPerDay) {
            return Optional.of(String.format("Rate limit exceeded: countLastSecond %d, countLastMinute %d, countLastHour %d, countLastDay=%d",
                    countLastSecond, countLastMinute, countLastHour, countLastDay));
        }
        return Optional.empty();
    }

    // An event older than one day counts in no window, now or later
    private static void removeEventsOlderThanOneDay(PriorityQueue<Long> timestamps, long now) {
        while (!timestamps.isEmpty() && now - timestamps.peek() > DAY) {
            timestamps.poll();
        }
    }

    private void removeIdleUserProfileIds(long now, List<String> removedUserProfileIds) {
        if (now - lastRemoveIdleTime < REMOVE_IDLE_INTERVAL) {
            return;
        }
        lastRemoveIdleTime = now;
        // Iterating does not change the access order
        Iterator<Map.Entry<String, PriorityQueue<Long>>> iterator = timestampsByUserProfileId.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PriorityQueue<Long>> entry = iterator.next();
            removeEventsOlderThanOneDay(entry.getValue(), now);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                removedUserProfileIds.add(entry.getKey());
            }
        }
    }

    private void removeLeastRecentlyUsedUserProfileId(List<String> removedUserProfileIds) {
        Iterator<String> iterator = timestampsByUserProfileId.keySet().iterator();
        removedUserProfileIds.add(iterator.next());
        iterator.remove();
    }

    private void notifyRemovedUserProfileIdListeners(List<String> removedUserProfileIds) {
        removedUserProfileIds.forEach(userProfileId ->
                removedUserProfileIdListeners.forEach(listener -> {
                    try {
                        listener.accept(userProfileId);
                    } catch (Exception e) {
                        log.error("Calling the listener of removed user profile IDs failed", e);
                    }
                }));
    }
}
