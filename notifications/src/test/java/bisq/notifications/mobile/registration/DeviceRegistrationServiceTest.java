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

import bisq.persistence.PersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class DeviceRegistrationServiceTest {
    private static final String CLIENT_ID = "client-1";

    private DeviceRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new DeviceRegistrationService(mock(PersistenceService.class, RETURNS_DEEP_STUBS));
    }

    private boolean register(String deviceId, String clientId) {
        return registerWithResult(deviceId, clientId) == DeviceRegistrationResult.REGISTERED;
    }

    private DeviceRegistrationResult registerWithResult(String deviceId, String clientId) {
        return service.register(deviceId,
                "a".repeat(64),
                "publicKey",
                "descriptor",
                MobileDevicePlatform.ANDROID,
                Optional.empty(),
                clientId);
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
    void unregisterReturnsFalseForUnknownDevice() {
        assertFalse(service.unregister("unknown-device", CLIENT_ID));
    }

    @Test
    void registrationsWithoutOwnerAreNotMatched() {
        // Registrations persisted before the ownership link existed carry no client ID and can
        // only be removed by device ID.
        registerWithoutOwner("legacy-device");

        assertTrue(service.unregisterByClientId(CLIENT_ID).isEmpty());
        assertEquals(1, service.getMobileDeviceProfiles().size());
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



}
