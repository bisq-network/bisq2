package bisq.http_api.push_notification;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistrationStoreTest {

    private MobileDeviceProfile createProfile(String deviceId, String token, String key, MobileDevicePlatform platform) {
        return new MobileDeviceProfile(deviceId, token, key, "Test Device", platform, System.currentTimeMillis());
    }

    @Test
    void testDefaultConstructor() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();
        assertNotNull(store.getDeviceByDeviceId());
        assertTrue(store.getDeviceByDeviceId().isEmpty());
    }

    @Test
    void testConstructorWithMap() {
        Map<String, MobileDeviceProfile> map = new HashMap<>();
        map.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);

        assertEquals(1, store.getDeviceByDeviceId().size());
        assertTrue(store.getDeviceByDeviceId().containsKey("device1"));
    }

    @Test
    void testGetClone() {
        Map<String, MobileDeviceProfile> map = new HashMap<>();
        map.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);
        DeviceRegistrationStore clone = store.getClone();

        assertEquals(store.getDeviceByDeviceId().size(), clone.getDeviceByDeviceId().size());
        assertNotSame(store.getDeviceByDeviceId(), clone.getDeviceByDeviceId());
    }

    @Test
    void testApplyPersisted() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();

        Map<String, MobileDeviceProfile> map = new HashMap<>();
        map.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        DeviceRegistrationStore persisted = new DeviceRegistrationStore(map);

        store.applyPersisted(persisted);

        assertEquals(1, store.getDeviceByDeviceId().size());
        assertTrue(store.getDeviceByDeviceId().containsKey("device1"));
    }

    @Test
    void testApplyPersistedClearsExisting() {
        Map<String, MobileDeviceProfile> map1 = new HashMap<>();
        map1.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));

        DeviceRegistrationStore store = new DeviceRegistrationStore(map1);
        assertEquals(1, store.getDeviceByDeviceId().size());

        Map<String, MobileDeviceProfile> map2 = new HashMap<>();
        map2.put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.ANDROID));

        DeviceRegistrationStore persisted = new DeviceRegistrationStore(map2);

        store.applyPersisted(persisted);

        assertEquals(1, store.getDeviceByDeviceId().size());
        assertFalse(store.getDeviceByDeviceId().containsKey("device1"));
        assertTrue(store.getDeviceByDeviceId().containsKey("device2"));
    }

    @Test
    void testMultipleDevices() {
        Map<String, MobileDeviceProfile> map = new HashMap<>();
        map.put("device1", createProfile("device1", "token1", "key1", MobileDevicePlatform.IOS));
        map.put("device2", createProfile("device2", "token2", "key2", MobileDevicePlatform.ANDROID));

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);

        assertEquals(2, store.getDeviceByDeviceId().size());
        assertTrue(store.getDeviceByDeviceId().containsKey("device1"));
        assertTrue(store.getDeviceByDeviceId().containsKey("device2"));
    }
}

