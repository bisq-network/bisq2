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
