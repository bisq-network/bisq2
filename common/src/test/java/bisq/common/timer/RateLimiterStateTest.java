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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterStateTest {
    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);

    @Test
    void idleKeyIsRemovedAndActiveOneIsStillLimited() {
        MockClock clock = new MockClock();
        clock.setTime(DAY);
        RateLimiter rateLimiter = new RateLimiter(clock, 3, 30, 120, 300, 1000);
        List<String> removedIds = new ArrayList<>();
        rateLimiter.addRemovedKeyListener(removedIds::add);
        for (int i = 0; i < 1000; i++) {
            assertFalse(rateLimiter.exceedsLimit("idle-" + i));
        }

        // One day and one interval of the removal later, the next event removes the IDs without an event since then
        clock.advanceTime(DAY + MINUTE + 1);
        assertFalse(rateLimiter.exceedsLimit("active"));

        assertEquals(1, rateLimiter.getNumKeys());
        assertEquals(1000, removedIds.size());
        assertFalse(rateLimiter.exceedsLimit("active"));
        assertFalse(rateLimiter.exceedsLimit("active"));
        // The 4th event within one second exceeds the limit of 3 per second
        assertTrue(rateLimiter.exceedsLimit("active"));
    }

    @Test
    void keyWithEventWithinTheLastDayIsKept() {
        MockClock clock = new MockClock();
        clock.setTime(DAY);
        RateLimiter rateLimiter = new RateLimiter(clock, 3, 30, 120, 300, 1000);
        rateLimiter.exceedsLimit("recent");

        clock.advanceTime(DAY - 1);
        rateLimiter.exceedsLimit("other");

        assertEquals(2, rateLimiter.getNumKeys());
    }

    @Test
    void numberOfKeysIsBoundedAndLeastRecentlyUsedKeyIsRemoved() {
        MockClock clock = new MockClock();
        clock.setTime(DAY);
        RateLimiter rateLimiter = new RateLimiter(clock, 3, 30, 120, 300, 2);
        List<String> removedIds = new ArrayList<>();
        rateLimiter.addRemovedKeyListener(removedIds::add);

        assertFalse(rateLimiter.exceedsLimit("a"));
        assertFalse(rateLimiter.exceedsLimit("b"));
        assertFalse(rateLimiter.exceedsLimit("a"));
        assertFalse(rateLimiter.exceedsLimit("c"));

        assertEquals(2, rateLimiter.getNumKeys());
        assertEquals(List.of("b"), removedIds);
        // The kept ID keeps its events: its 3rd and 4th event within one second exceed the limit of 3 per second
        assertFalse(rateLimiter.exceedsLimit("a"));
        assertTrue(rateLimiter.exceedsLimit("a"));
    }

    @Test
    void eventsNotInTheOrderOfTheirTimeCountAsBefore() {
        MockClock clock = new MockClock();
        clock.setTime(10 * DAY);
        long now = clock.now();
        RateLimiter rateLimiter = new RateLimiter(clock, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 300, 1000);
        List<Long> eventTimes = new ArrayList<>();
        // 1000 events older than one day count in no window
        for (int i = 1; i <= 1000; i++) {
            eventTimes.add(now - DAY - i * MINUTE);
        }
        // 300 events within the last day reach the limit of 300 per day
        for (int i = 0; i < 300; i++) {
            eventTimes.add(now - i * MINUTE);
        }
        Collections.shuffle(eventTimes, new Random(1));
        for (long eventTime : eventTimes) {
            assertFalse(rateLimiter.exceedsLimit("user", eventTime));
        }

        assertTrue(rateLimiter.exceedsLimit("user", now));
    }

    @Test
    void keepingOnlyTheNewestEventsDoesNotChangeDecisions() {
        MockClock clock = new MockClock();
        clock.setTime(10 * DAY);
        long now = clock.now();
        // The limiter keeps the newest 301 events of an ID
        RateLimiter rateLimiter = new RateLimiter(clock, 3, 30, 120, 300, 1000);
        List<Long> eventTimes = new ArrayList<>();
        // 400 events within the last 20 hours, 3 minutes apart: only the limit per day is exceeded
        for (int i = 0; i < 400; i++) {
            eventTimes.add(now - i * 3 * MINUTE);
        }
        Collections.shuffle(eventTimes, new Random(2));
        eventTimes.forEach(eventTime -> rateLimiter.exceedsLimit("user", eventTime));
        assertTrue(rateLimiter.exceedsLimit("user", now));

        // 10 hours later, 282 of these events and the new one are within the last day
        clock.advanceTime(TimeUnit.HOURS.toMillis(10));
        assertFalse(rateLimiter.exceedsLimit("user"));
    }

    @Test
    void manyEventsWithinOneSecondExceedTheLimitAsBefore() {
        MockClock clock = new MockClock();
        clock.setTime(DAY);
        RateLimiter rateLimiter = new RateLimiter(clock, 3, 30, 120, 300, 1000);
        for (int i = 0; i < 3; i++) {
            assertFalse(rateLimiter.exceedsLimit("user"));
        }
        // More events than the limiter keeps per ID (the highest maximum + 1) still exceed every limit
        for (int i = 0; i < 1000; i++) {
            assertTrue(rateLimiter.exceedsLimit("user"));
        }

        // Two days later, the events count in no window anymore
        clock.advanceTime(2 * DAY);
        assertFalse(rateLimiter.exceedsLimit("user"));
    }
}
