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
 * Tells whether a sender, identified by a key, has caused too many events in too little time. The key is chosen by the
 * caller, for example the user profile ID of a chat author. The limiter counts the events of each key in four time
 * windows: the last second, minute, hour and day. An event exceeds the limit if a window holds more events than its
 * maximum. The caller defines the maximums.
 * <p>
 * Keys can come from other users, who can create new keys cheaply. So the limiter keeps state only while it can change
 * a decision: it removes a key without an event within the last day, and it keeps at most maxKeys keys (see "Rate limit
 * of chat authors" in docs/specifications/security/derived-state.md).
 */
@Slf4j
public class RateLimiter {
    private final static long SECOND = 1000L;
    private final static long MINUTE = 60 * SECOND;
    private final static long HOUR = 60 * MINUTE;
    private final static long DAY = 24 * HOUR;

    // The limiter removes the keys without an event within the last day at most once in this interval
    private final static long REMOVE_IDLE_INTERVAL = MINUTE;

    private final Clock clock;
    private final int maxPerSecond;
    private final int maxPerMinute;
    private final int maxPerHour;
    private final int maxPerDay;
    private final int maxKeys;
    // A window which holds more events than the highest maximum exceeds its maximum. So the newest (highest maximum + 1)
    // events of a key decide as all of its events do.
    private final int maxTimestampsPerKey;
    // Guarded by this. In access order, so that the key whose state was used least recently comes first. A priority queue
    // keeps the oldest event first, also for events which are not added in the order of their time.
    private final LinkedHashMap<String, PriorityQueue<Long>> timestampsByKey =
            new LinkedHashMap<>(16, 0.75f, true);
    private final Set<Consumer<String>> removedKeyListeners = new CopyOnWriteArraySet<>();
    // Guarded by this
    private long lastRemoveIdleTime;

    public RateLimiter(int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay, int maxKeys) {
        this(new SystemClock(), maxPerSecond, maxPerMinute, maxPerHour, maxPerDay, maxKeys);
    }

    public RateLimiter(Clock clock, int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay, int maxKeys) {
        checkArgument(maxKeys > 0, "maxKeys must be positive");
        this.clock = clock;
        this.maxPerSecond = maxPerSecond;
        this.maxPerMinute = maxPerMinute;
        this.maxPerHour = maxPerHour;
        this.maxPerDay = maxPerDay;
        this.maxKeys = maxKeys;
        long highestMaximum = Math.max(Math.max(maxPerSecond, maxPerMinute), Math.max(maxPerHour, maxPerDay));
        maxTimestampsPerKey = (int) Math.min(Integer.MAX_VALUE, highestMaximum + 1);
    }

    /**
     * The listener gets each key whose state the limiter removed: a key without an event within the last day, or the
     * least recently used key when a new key exceeds maxKeys. The limiter calls the listener after
     * it has released its lock.
     */
    public void addRemovedKeyListener(Consumer<String> listener) {
        removedKeyListeners.add(listener);
    }

    public boolean exceedsLimit(String key) {
        return exceedsLimit(key, clock.now());
    }

    public boolean exceedsLimit(String key, long timeStamp) {
        return getExceedsLimitInfo(key, timeStamp).isPresent();
    }

    public Optional<String> getExceedsLimitInfo(String key) {
        return getExceedsLimitInfo(key, clock.now());
    }

    public Optional<String> getExceedsLimitInfo(String key, long timeStamp) {
        List<String> removedKeys = new ArrayList<>();
        Optional<String> exceedsLimitInfo;
        synchronized (this) {
            long now = clock.now();
            removeIdleKeys(now, removedKeys);

            PriorityQueue<Long> timestamps = timestampsByKey.get(key);
            if (timestamps == null) {
                if (timestampsByKey.size() >= maxKeys) {
                    removeLeastRecentlyUsedKey(removedKeys);
                }
                timestamps = new PriorityQueue<>();
                timestampsByKey.put(key, timestamps);
            }
            timestamps.add(timeStamp);
            removeEventsOlderThanOneDay(timestamps, now);
            while (timestamps.size() > maxTimestampsPerKey) {
                timestamps.poll();
            }

            exceedsLimitInfo = getExceedsLimitInfo(timestamps, now);
        }

        notifyRemovedKeyListeners(removedKeys);
        exceedsLimitInfo.ifPresent(log::info);
        return exceedsLimitInfo;
    }

    @VisibleForTesting
    synchronized int getNumKeys() {
        return timestampsByKey.size();
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

    private void removeIdleKeys(long now, List<String> removedKeys) {
        if (now - lastRemoveIdleTime < REMOVE_IDLE_INTERVAL) {
            return;
        }
        lastRemoveIdleTime = now;
        // Iterating does not change the access order
        Iterator<Map.Entry<String, PriorityQueue<Long>>> iterator = timestampsByKey.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, PriorityQueue<Long>> entry = iterator.next();
            removeEventsOlderThanOneDay(entry.getValue(), now);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
                removedKeys.add(entry.getKey());
            }
        }
    }

    private void removeLeastRecentlyUsedKey(List<String> removedKeys) {
        Iterator<String> iterator = timestampsByKey.keySet().iterator();
        removedKeys.add(iterator.next());
        iterator.remove();
    }

    private void notifyRemovedKeyListeners(List<String> removedKeys) {
        removedKeys.forEach(key ->
                removedKeyListeners.forEach(listener -> {
                    try {
                        listener.accept(key);
                    } catch (Exception e) {
                        log.error("Calling the listener of removed keys failed", e);
                    }
                }));
    }
}
