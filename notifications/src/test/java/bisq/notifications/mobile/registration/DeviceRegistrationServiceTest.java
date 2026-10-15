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

package bisq.notifications.mobile.registration;

import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class DeviceRegistrationServiceTest {
    private static final String CLIENT_ID = "client-1";

    @SuppressWarnings("rawtypes")
    private Persistence persistence;
    private DeviceRegistrationService service;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        PersistenceService persistenceService = mock(PersistenceService.class, RETURNS_DEEP_STUBS);
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        service = new DeviceRegistrationService(persistenceService);
    }

    private boolean register(String deviceId, String clientId) {
        return registerWithResult(deviceId, clientId) == DeviceRegistrationResult.REGISTERED;
    }

    private DeviceRegistrationResult registerWithResult(String deviceId, String clientId) {
        return registerWithResult(deviceId, clientId, authorizedClientId -> true);
    }

    private DeviceRegistrationResult registerWithResult(String deviceId,
                                                        String clientId,
                                                        Predicate<String> clientAuthorizedCheck) {
        return service.register(deviceId,
                "a".repeat(64),
                "publicKey",
                "descriptor",
                MobileDevicePlatform.ANDROID,
                Optional.empty(),
                clientId,
                clientAuthorizedCheck);
    }

    /** Simulates a registration persisted before registrations carried an owner. */
    private void registerWithoutOwner(String deviceId) {
        service.getPersistableStore().getDeviceByDeviceId().put(deviceId,
                new MobileDeviceProfile(deviceId,
                        "a".repeat(64),
                        "publicKey",
                        "descriptor",
                        MobileDevicePlatform.ANDROID,
                        Optional.empty(),
                        Optional.empty()));
    }

    @Test
    void aRevocationRemovesTheRegistrationsOfThatClientOnly() {
        register("device-1", CLIENT_ID);
        register("device-2", "other-client");

        assertEquals(Set.of("device-1"), service.unregisterByClientId(CLIENT_ID));
        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void registerRejectsADeviceIdOwnedByAnotherClient() {
        // Device IDs are chosen by the client, so without this check any client could claim
        // another client's device and redirect its push notifications to its own token.
        register("device-1", CLIENT_ID);

        assertFalse(register("device-1", "other-client"));
        assertEquals(Optional.of(CLIENT_ID), service.getMobileDeviceProfiles().iterator().next().getClientId());
    }

    @Test
    void registerClaimsAnUnownedRegistration() {
        registerWithoutOwner("legacy-device");

        assertTrue(register("legacy-device", CLIENT_ID));
        assertEquals(Optional.of(CLIENT_ID), service.getMobileDeviceProfiles().iterator().next().getClientId());
    }

    @Test
    void registerUpdatesTheOwnersOwnDevice() {
        register("device-1", CLIENT_ID);

        assertTrue(register("device-1", CLIENT_ID));
        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void unregisterByClientIdRemovesOnlyThatClientsDevices() {
        register("device-1", CLIENT_ID);
        register("device-2", CLIENT_ID);
        register("device-3", "other-client");

        assertEquals(Set.of("device-1", "device-2"), service.unregisterByClientId(CLIENT_ID));
        assertEquals(Set.of("device-3"), service.getMobileDeviceProfiles().stream()
                .map(MobileDeviceProfile::getDeviceId)
                .collect(Collectors.toSet()));
    }

    @Test
    void unregisterRejectsDevicesOwnedByAnotherClient() {
        register("device-1", "other-client");

        assertFalse(service.unregister("device-1", CLIENT_ID));
        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void unregisterRemovesOwnDevice() {
        register("device-1", CLIENT_ID);

        assertTrue(service.unregister("device-1", CLIENT_ID));
        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }

    @Test
    void unregisterRemovesUnownedLegacyDevice() {
        // Registrations from before the ownership link have no owner to check against, so they
        // stay removable until the owning app registers again and claims them.
        registerWithoutOwner("legacy-device");

        assertTrue(service.unregister("legacy-device", CLIENT_ID));
        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }

    @Test
    void pruneDeadRegistrationRemovesTheDeviceWhileItsRejectedTokenIsCurrent() {
        register("device-1", CLIENT_ID);

        assertTrue(service.pruneDeadRegistration("device-1", "a".repeat(64)));
        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }

    @Test
    void pruneDeadRegistrationKeepsADeviceThatReRegisteredWithAFreshToken() {
        // Between the dispatch snapshot and the gateway's verdict the device may have
        // re-registered; the verdict names the old token only, so the live registration stays.
        register("device-1", CLIENT_ID);

        assertFalse(service.pruneDeadRegistration("device-1", "stale-token-from-snapshot"));
        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void pruneDeadRegistrationReturnsFalseForUnknownDevice() {
        assertFalse(service.pruneDeadRegistration("unknown-device", "some-token"));
    }

    @Test
    void unregisterReturnsFalseForUnknownDevice() {
        assertFalse(service.unregister("unknown-device", CLIENT_ID));
    }


    @Test
    void concurrentRegistrationsOfANewDeviceIdProduceExactlyOneOwner() throws Exception {
        // The owner check and the write must be one step: otherwise every racing client passes
        // the check on an unknown device ID and the last write silently takes the device.
        int numClients = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        try {
            List<Future<Boolean>> results = IntStream.range(0, numClients)
                    .mapToObj(i -> executor.submit(() -> {
                        start.await();
                        return register("contested-device", "client-" + i);
                    }))
                    .toList();
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(1, accepted);
            assertEquals(1, service.getMobileDeviceProfiles().size());
        } finally {
            executor.shutdownNow();
        }
    }



    @Test
    void revocationAlsoDropsRegistrationsItCannotAttribute() {
        // An ownerless record cannot be excluded from being the revoked client's, so leaving it
        // would make revocation report a completeness it does not have.
        register("device-1", CLIENT_ID);
        registerWithoutOwner("legacy-device");
        register("device-2", "other-client");

        assertEquals(Set.of("device-1", "legacy-device"), service.unregisterByClientId(CLIENT_ID));
        assertEquals(Set.of("device-2"), service.getMobileDeviceProfiles().stream()
                .map(MobileDeviceProfile::getDeviceId)
                .collect(Collectors.toSet()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRegistrationAndItsRemovalBothReachPersistence() {
        // Store then remove, microseconds apart, as when a revocation follows a registration, which
        // is what a write rate limit drops. Losing the removal is unrecoverable: the revocation has
        // moved on and nothing revisits it.
        register("device-1", CLIENT_ID);

        service.unregister("device-1", CLIENT_ID);

        verify(persistence, times(2)).persistAsync(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFailedWriteOfARevocationIsReportedAndTheRegistrationStays() {
        // Otherwise the revocation reports success while the next start brings the registration
        // back for a client that no longer exists, so nothing would ever remove it.
        register("device-1", CLIENT_ID);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("disk full")));

        assertThrows(IllegalStateException.class, () -> service.unregisterByClientId(CLIENT_ID));

        assertEquals(1, service.getMobileDeviceProfiles().size(), "memory must keep matching the disk");
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        assertEquals(Set.of("device-1"), service.unregisterByClientId(CLIENT_ID), "the retry repeats the removal");
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFailedWriteOfARegistrationIsReportedAndNotKeptInMemory() {
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("disk full")));

        assertThrows(IllegalStateException.class, () -> registerWithResult("device-1", CLIENT_ID));

        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFailedWriteOfAPruneKeepsTheRegistration() {
        register("device-1", CLIENT_ID);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("disk full")));

        assertThrows(IllegalStateException.class, () -> service.pruneDeadRegistration("device-1", "a".repeat(64)));

        assertEquals(1, service.getMobileDeviceProfiles().size());
    }

    @Test
    void anEmptyClientIdInAPersistedProfileIsNoOwner() throws Exception {
        MobileDeviceProfile profile = MobileDeviceProfile.fromProto(
                bisq.notifications.protobuf.MobileDeviceProfile.newBuilder()
                        .setDeviceId("device-1")
                        .setDeviceToken("a".repeat(64))
                        .setPublicKeyBase64("publicKey")
                        .setDeviceDescriptor("descriptor")
                        .setPlatform(MobileDevicePlatform.ANDROID.toProtoEnum())
                        .setClientId("")
                        .build());

        assertEquals(Optional.empty(), profile.getClientId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aWriteFromOutsideTheServiceDoesNotSnapshotAChangeThatAFailedWriteUndoes() throws Exception {
        // The backup of all stores persists every client without the service's monitor. Taken
        // between the removal and its undo, its snapshot would put the removal on disk while
        // memory keeps the registration.
        register("device-1", CLIENT_ID);
        CompletableFuture<Void> revocationWrite = new CompletableFuture<>();
        CountDownLatch revocationWriteStarted = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger();
        when(persistence.persistAsync(any())).thenAnswer(invocation -> {
            if (writes.getAndIncrement() == 0) {
                revocationWriteStarted.countDown();
                return revocationWrite;
            }
            return CompletableFuture.completedFuture(null);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> revocation = executor.submit(() -> service.unregisterByClientId(CLIENT_ID));
            assertTrue(revocationWriteStarted.await(5, TimeUnit.SECONDS));

            CompletableFuture<Thread> backupThread = new CompletableFuture<>();
            Future<Boolean> backup = executor.submit(() -> {
                backupThread.complete(Thread.currentThread());
                return service.persist().join();
            });
            Thread thread = backupThread.get(5, TimeUnit.SECONDS);
            // Either the backup has taken its snapshot already, or it waits for the monitor.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (writes.get() < 2 && thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }

            revocationWrite.completeExceptionally(new RuntimeException("disk full"));
            ExecutionException failure = assertThrows(ExecutionException.class, () -> revocation.get(5, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof IllegalStateException);
            assertTrue(backup.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        ArgumentCaptor<DeviceRegistrationStore> written = ArgumentCaptor.forClass(DeviceRegistrationStore.class);
        verify(persistence, times(3)).persistAsync(written.capture());
        assertEquals(Set.of("device-1"), service.getMobileDeviceProfiles().stream()
                .map(MobileDeviceProfile::getDeviceId)
                .collect(Collectors.toSet()), "the failed revocation keeps the registration in memory");
        assertTrue(written.getValue().getDeviceByDeviceId().containsKey("device-1"),
                "the backup must write what memory holds, not the undone removal");
    }

    @Test
    void aRegistrationOfAClientWithoutAccessIsRefusedAndNothingIsWritten() {
        assertEquals(DeviceRegistrationResult.CLIENT_NOT_AUTHORIZED,
                registerWithResult("device-1", CLIENT_ID, clientId -> false));

        assertTrue(service.getMobileDeviceProfiles().isEmpty());
        verify(persistence, never()).persistAsync(any());
    }

    @Test
    void aRegistrationAdmittedBeforeACompletedRevocationIsRefused() {
        // The request passed authorization while the client still had access, then the whole
        // revocation ran: access withdrawn, registrations removed. Stored now, the registration
        // would be owned by a client nothing revokes again and keep receiving notifications.
        AtomicBoolean authorized = new AtomicBoolean(true);
        register("device-1", CLIENT_ID);
        authorized.set(false);
        assertEquals(Set.of("device-1"), service.unregisterByClientId(CLIENT_ID));

        assertEquals(DeviceRegistrationResult.CLIENT_NOT_AUTHORIZED,
                registerWithResult("device-2", CLIENT_ID, clientId -> authorized.get()));

        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRevocationDuringAnInFlightRegistrationRemovesIt() throws Exception {
        // The other order: the registration passed its check and is being written when the
        // revocation withdraws access. Whenever the revocation's removal runs, it runs after the
        // registration released the monitor, so it finds the registration.
        AtomicBoolean authorized = new AtomicBoolean(true);
        CompletableFuture<Void> registrationWrite = new CompletableFuture<>();
        CountDownLatch registrationWriteStarted = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger();
        when(persistence.persistAsync(any())).thenAnswer(invocation -> {
            if (writes.getAndIncrement() == 0) {
                registrationWriteStarted.countDown();
                return registrationWrite;
            }
            return CompletableFuture.completedFuture(null);
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<DeviceRegistrationResult> registration = executor.submit(() ->
                    registerWithResult("device-1", CLIENT_ID, clientId -> authorized.get()));
            assertTrue(registrationWriteStarted.await(5, TimeUnit.SECONDS));

            authorized.set(false);
            Future<Set<String>> revocation = executor.submit(() -> service.unregisterByClientId(CLIENT_ID));
            registrationWrite.complete(null);

            assertEquals(DeviceRegistrationResult.REGISTERED, registration.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("device-1"), revocation.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertTrue(service.getMobileDeviceProfiles().isEmpty());
    }
}
