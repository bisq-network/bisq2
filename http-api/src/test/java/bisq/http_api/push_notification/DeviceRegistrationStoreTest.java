package bisq.http_api.push_notification;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistrationStoreTest {

    @Test
    void testDefaultConstructor() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();
        assertNotNull(store.getDevicesByUserProfileId());
        assertTrue(store.getDevicesByUserProfileId().isEmpty());
    }

    @Test
    void testConstructorWithMap() {
        Map<String, Set<DeviceRegistration>> map = new HashMap<>();
        Set<DeviceRegistration> devices = new HashSet<>();
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map.put("user1", devices);

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);

        assertEquals(1, store.getDevicesByUserProfileId().size());
        assertTrue(store.getDevicesByUserProfileId().containsKey("user1"));
    }

    @Test
    void testGetClone() {
        Map<String, Set<DeviceRegistration>> map = new HashMap<>();
        Set<DeviceRegistration> devices = new HashSet<>();
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map.put("user1", devices);

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);
        DeviceRegistrationStore clone = store.getClone();

        assertEquals(store.getDevicesByUserProfileId().size(), clone.getDevicesByUserProfileId().size());
        assertNotSame(store.getDevicesByUserProfileId(), clone.getDevicesByUserProfileId());
    }

    @Test
    void testApplyPersisted() {
        DeviceRegistrationStore store = new DeviceRegistrationStore();
        
        Map<String, Set<DeviceRegistration>> map = new HashMap<>();
        Set<DeviceRegistration> devices = new HashSet<>();
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map.put("user1", devices);
        
        DeviceRegistrationStore persisted = new DeviceRegistrationStore(map);
        
        store.applyPersisted(persisted);
        
        assertEquals(1, store.getDevicesByUserProfileId().size());
        assertTrue(store.getDevicesByUserProfileId().containsKey("user1"));
    }

    @Test
    void testApplyPersistedClearsExisting() {
        Map<String, Set<DeviceRegistration>> map1 = new HashMap<>();
        Set<DeviceRegistration> devices1 = new HashSet<>();
        devices1.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map1.put("user1", devices1);
        
        DeviceRegistrationStore store = new DeviceRegistrationStore(map1);
        assertEquals(1, store.getDevicesByUserProfileId().size());
        
        Map<String, Set<DeviceRegistration>> map2 = new HashMap<>();
        Set<DeviceRegistration> devices2 = new HashSet<>();
        devices2.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.ANDROID));
        map2.put("user2", devices2);
        
        DeviceRegistrationStore persisted = new DeviceRegistrationStore(map2);
        
        store.applyPersisted(persisted);
        
        assertEquals(1, store.getDevicesByUserProfileId().size());
        assertFalse(store.getDevicesByUserProfileId().containsKey("user1"));
        assertTrue(store.getDevicesByUserProfileId().containsKey("user2"));
    }

    @Test
    void testMultipleDevicesPerUser() {
        Map<String, Set<DeviceRegistration>> map = new HashMap<>();
        Set<DeviceRegistration> devices = new HashSet<>();
        devices.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        devices.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.ANDROID));
        map.put("user1", devices);

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);

        assertEquals(1, store.getDevicesByUserProfileId().size());
        assertEquals(2, store.getDevicesByUserProfileId().get("user1").size());
    }

    @Test
    void testMultipleUsers() {
        Map<String, Set<DeviceRegistration>> map = new HashMap<>();
        
        Set<DeviceRegistration> devices1 = new HashSet<>();
        devices1.add(new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS));
        map.put("user1", devices1);
        
        Set<DeviceRegistration> devices2 = new HashSet<>();
        devices2.add(new DeviceRegistration("token2", "key2", DeviceRegistration.Platform.ANDROID));
        map.put("user2", devices2);

        DeviceRegistrationStore store = new DeviceRegistrationStore(map);

        assertEquals(2, store.getDevicesByUserProfileId().size());
        assertTrue(store.getDevicesByUserProfileId().containsKey("user1"));
        assertTrue(store.getDevicesByUserProfileId().containsKey("user2"));
    }
}

