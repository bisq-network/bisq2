package bisq.common.timer;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {
    @SneakyThrows
    @Test
    public void testRateLimiter() {
        MockClock clock = new MockClock();
        clock.setTime(0);
        RateLimiter rateLimiter = new RateLimiter(clock, 5, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        String userId = "user123";
        for (int i = 0; i < 5; i++) {
            clock.advanceTime(200);
            assertFalse(rateLimiter.exceedsLimit(userId));
        }
        // Time is now 1000 ms, so 6th message exceeds limit
        assertTrue(rateLimiter.exceedsLimit(userId));

        // We pass another second. This cleans up the first 4 entries. We have only 2 now
        clock.advanceTime(1000);
        assertFalse(rateLimiter.exceedsLimit(userId));
        // We added the 3rd item for the last second
        assertFalse(rateLimiter.exceedsLimit(userId));
        assertFalse(rateLimiter.exceedsLimit(userId));
        // 6th fails again
        assertTrue(rateLimiter.exceedsLimit(userId));

        // Reset
        clock.setTime(0);
        rateLimiter = new RateLimiter(clock, Integer.MAX_VALUE, 20, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // Simulate 20 messages in 1 minute
        for (int i = 0; i < 20; i++) {
            clock.advanceTime(3000);
            assertFalse(rateLimiter.exceedsLimit(userId));
        }

        // 21st message within the same minute should be spam
        assertTrue(rateLimiter.exceedsLimit(userId));

        // Reset
        clock.setTime(0);
        rateLimiter = new RateLimiter(clock, Integer.MAX_VALUE, Integer.MAX_VALUE, 100, Integer.MAX_VALUE);
        for (int i = 0; i < 100; i++) {
            clock.advanceTime(36);
            assertFalse(rateLimiter.exceedsLimit(userId));
        }

        // 101st message within the same minute should be spam
        assertTrue(rateLimiter.exceedsLimit(userId));

        // Reset
        clock.setTime(0);
        rateLimiter = new RateLimiter(clock, 5, 20, 100, Integer.MAX_VALUE);
        for (int i = 0; i < 100; i++) {
            clock.advanceTime(3000);
            if (i < 20) {
                assertFalse(rateLimiter.exceedsLimit(userId));
            } else {
                assertTrue(rateLimiter.exceedsLimit(userId));
            }
        }

        // Different users or each message
        clock.setTime(0);
        rateLimiter = new RateLimiter(clock, 5, 20, 100, Integer.MAX_VALUE);
        for (int i = 0; i < 100; i++) {
            clock.advanceTime(3000);
            assertFalse(rateLimiter.exceedsLimit("user" + i));
        }

        // One user with 20 messages, then diff. users
        clock.setTime(0);
        rateLimiter = new RateLimiter(clock, 5, 20, 100, Integer.MAX_VALUE);
        for (int i = 0; i < 100; i++) {
            clock.advanceTime(3000);
            int userIndex = i < 20 ? 1 : i;
            assertFalse(rateLimiter.exceedsLimit("user" + i));
        }

        // Using system clock, only the first 5 messages pass.
        rateLimiter = new RateLimiter();
        for (int i = 0; i < 100; i++) {
            if (i < 5) {
                assertFalse(rateLimiter.exceedsLimit(userId));
            } else {
                assertTrue(rateLimiter.exceedsLimit(userId));
            }
        }
    }
}