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

package bisq.offer.mu_sig;

import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyMuSigOffersServiceTest {
    @Test
    void claimingDoesNotConsumeTheActivatedOffer() {
        MyMuSigOffersService service = createService();
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        service.addOffer(offer);

        assertThat(service.claimActivatedOffer("offer-id").orElseThrow()).isSameAs(offer);
        assertThat(service.claimActivatedOffer("offer-id").orElseThrow()).isSameAs(offer);
    }

    @Test
    void removedOfferCannotBeClaimed() {
        MyMuSigOffersService service = createService();
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        service.addOffer(offer);

        service.removeOffer(offer);

        assertThat(service.claimActivatedOffer("offer-id")).isEmpty();
    }

    @Test
    void deactivationThatWinsTheStateLockPreventsClaim() throws Exception {
        MyMuSigOffersService service = createService();
        MuSigOffer offer = mock(MuSigOffer.class);
        when(offer.getId()).thenReturn("offer-id");
        service.addOffer(offer);

        CountDownLatch claimThreadStarted = new CountDownLatch(1);
        AtomicReference<Optional<MuSigOffer>> result = new AtomicReference<>();
        Thread claimThread = new Thread(() -> {
            claimThreadStarted.countDown();
            result.set(service.claimActivatedOffer("offer-id"));
        });

        synchronized (service) {
            claimThread.start();
            assertThat(claimThreadStarted.await(5, TimeUnit.SECONDS)).isTrue();
            awaitBlocked(claimThread);
            service.deactivateOffer(offer);
        }

        claimThread.join(TimeUnit.SECONDS.toMillis(5));
        assertThat(claimThread.isAlive()).isFalse();
        assertThat(result.get()).isEmpty();
    }

    private static MyMuSigOffersService createService() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        @SuppressWarnings("unchecked")
        Persistence<MyMuSigOffersStore> persistence = mock(Persistence.class);
        when(persistence.persistAsync(any(MyMuSigOffersStore.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(persistenceService.getOrCreatePersistence(any(), eq(DbSubDirectory.PRIVATE),
                any(MyMuSigOffersStore.class)))
                .thenReturn(persistence);
        return new MyMuSigOffersService(persistenceService);
    }

    private static void awaitBlocked(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        assertThat(thread.getState()).isEqualTo(Thread.State.BLOCKED);
    }
}
