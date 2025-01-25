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

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RateLimiter {
    private final static long SECOND = 1000L;
    private final static long MINUTE = 60 * SECOND;
    private final static long HOUR = 60 * MINUTE;
    private final static long DAY = 24 * HOUR;

    private final static int DEFAULT_MAX_PER_SECOND = 3;
    private final static int DEFAULT_MAX_PER_MINUTE = 30;
    private final static int DEFAULT_MAX_PER_HOUR = 120;
    private final static int DEFAULT_MAX_PER_DAY = 300;

    private final Clock clock;
    private final int maxPerSecond;
    private final int maxPerMinute;
    private final int maxPerHour;
    private final int maxPerDay;

    private final Map<String, Deque<Long>> timestampsByUserProfileId = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(DEFAULT_MAX_PER_SECOND, DEFAULT_MAX_PER_MINUTE, DEFAULT_MAX_PER_HOUR, DEFAULT_MAX_PER_DAY);
    }

    public RateLimiter(int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay) {
        this(new SystemClock(), maxPerSecond, maxPerMinute, maxPerHour, maxPerDay);
    }

    public RateLimiter(Clock clock, int maxPerSecond, int maxPerMinute, int maxPerHour, int maxPerDay) {
        this.clock = clock;
        this.maxPerSecond = maxPerSecond;
        this.maxPerMinute = maxPerMinute;
        this.maxPerHour = maxPerHour;
        this.maxPerDay = maxPerDay;
    }

    public boolean exceedsLimit(String userProfileId) {
        return exceedsLimit(userProfileId, clock.now());
    }

    public boolean exceedsLimit(String userProfileId, long timeStamp) {
        timestampsByUserProfileId.putIfAbsent(userProfileId, new ArrayDeque<>());
        Deque<Long> timestamps = timestampsByUserProfileId.get(userProfileId);
        synchronized (timestamps) {
            timestamps.addLast(timeStamp);

            while (!timestamps.isEmpty() && timeStamp - timestamps.peekFirst() > DAY) {
                timestamps.pollFirst();
            }

            long now = clock.now();
            long countLastSecond = timestamps.stream().filter(t -> now - t <= SECOND).count();
            long countLastMinute = timestamps.stream().filter(t -> now - t <= MINUTE).count();
            long countLastHour = timestamps.stream().filter(t -> now - t <= HOUR).count();
            long countLastDay = timestamps.stream().filter(t -> now - t <= DAY).count();
            if (countLastSecond > maxPerSecond ||
                    countLastMinute > maxPerMinute ||
                    countLastHour > maxPerHour ||
                    countLastDay > maxPerDay) {
                log.info("Rate limit exceeded: countLastSecond {}, countLastMinute {}, countLastHour {}, countLastDay={}", countLastSecond, countLastMinute, countLastHour, countLastDay);
                return true;
            }
        }

        return false;
    }
}
