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

package bisq.user.banned;

import bisq.bonded_roles.bonded_role.AuthorizedBondedRolesService;
import bisq.common.timer.RateLimiter;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BannedUserServiceTest {
    private static final String USER_PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";
    private static final String OTHER_USER_PROFILE_ID = "76543210fedcba9876543210fedcba9876543210";

    @Test
    void disabledRateLimitAlwaysReturnsNotExceeding() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        BannedUserService service = createService(false, rateLimiter);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            service.checkRateLimit(USER_PROFILE_ID, now + i);
        }

        assertFalse(service.isRateLimitExceeding(USER_PROFILE_ID));
        assertTrue(service.getExceedsLimitInfo(USER_PROFILE_ID).isEmpty());
        verifyNoInteractions(rateLimiter);
    }

    @Test
    void enabledRateLimitMarksProfileAsExceeding() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.exceedsLimit(eq(USER_PROFILE_ID), anyLong()))
                .thenReturn(false)
                .thenReturn(true);
        when(rateLimiter.exceedsLimit(USER_PROFILE_ID)).thenReturn(true);

        BannedUserService service = createService(true, rateLimiter);
        long now = 1_000L;
        service.checkRateLimit(USER_PROFILE_ID, now);
        service.checkRateLimit(USER_PROFILE_ID, now);

        assertTrue(service.isRateLimitExceeding(USER_PROFILE_ID));
    }

    @Test
    void flagOfIdleProfileExpiresAndActiveProfileIsStillFlagged() {
        AtomicLong time = new AtomicLong(TimeUnit.DAYS.toMillis(10));
        RateLimiter rateLimiter = new RateLimiter(time::get, 3, 30, 120, 300);
        BannedUserService service = createService(true, rateLimiter);
        // The 4th message within one second exceeds the limit of 3 per second
        for (int i = 0; i < 4; i++) {
            service.checkRateLimit(USER_PROFILE_ID, time.get());
        }
        assertTrue(service.getRateLimitExceedingUserProfiles().contains(USER_PROFILE_ID));

        // The profile sends nothing more. A message of another profile one day and one minute later removes the
        // rate limit state of the idle profile, and with it the flag.
        time.addAndGet(TimeUnit.DAYS.toMillis(1) + TimeUnit.MINUTES.toMillis(1) + 1);
        service.checkRateLimit(OTHER_USER_PROFILE_ID, time.get());

        assertFalse(service.getRateLimitExceedingUserProfiles().contains(USER_PROFILE_ID));
        for (int i = 0; i < 3; i++) {
            service.checkRateLimit(OTHER_USER_PROFILE_ID, time.get());
        }
        assertTrue(service.isRateLimitExceeding(OTHER_USER_PROFILE_ID));
    }

    private static BannedUserService createService(boolean rateLimitEnabled, RateLimiter rateLimiter) {
        PersistenceService persistenceService = mock(PersistenceService.class);
        AuthorizedBondedRolesService authorizedBondedRolesService = mock(AuthorizedBondedRolesService.class);
        @SuppressWarnings("unchecked")
        Persistence<BannedUserStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(BannedUserStore.class))).thenReturn(CompletableFuture.completedFuture(null));
        when(persistence.getStorePath()).thenReturn(Path.of("test-store"));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.CACHE), any(BannedUserStore.class)))
                .thenReturn(persistence);
        return new BannedUserService(persistenceService, authorizedBondedRolesService, rateLimiter, rateLimitEnabled);
    }

}
