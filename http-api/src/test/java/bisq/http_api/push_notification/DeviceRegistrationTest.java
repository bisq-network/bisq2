package bisq.http_api.push_notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceRegistrationTest {

    @Test
    void testConstructorAndGetters() {
        String deviceToken = "test-device-token-123";
        String publicKey = "test-public-key";
        DeviceRegistration.Platform platform = DeviceRegistration.Platform.IOS;

        DeviceRegistration registration = new DeviceRegistration(deviceToken, publicKey, platform);

        assertEquals(deviceToken, registration.getDeviceToken());
        assertEquals(publicKey, registration.getPublicKey());
        assertEquals(platform, registration.getPlatform());
    }

    @Test
    void testEquality() {
        DeviceRegistration reg1 = new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS);
        DeviceRegistration reg2 = new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS);
        DeviceRegistration reg3 = new DeviceRegistration("token2", "key1", DeviceRegistration.Platform.IOS);

        assertEquals(reg1, reg2);
        assertNotEquals(reg1, reg3);
    }

    @Test
    void testHashCode() {
        DeviceRegistration reg1 = new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS);
        DeviceRegistration reg2 = new DeviceRegistration("token1", "key1", DeviceRegistration.Platform.IOS);

        assertEquals(reg1.hashCode(), reg2.hashCode());
    }

    @Test
    void testToString() {
        DeviceRegistration registration = new DeviceRegistration("token", "key", DeviceRegistration.Platform.IOS);
        String toString = registration.toString();

        assertTrue(toString.contains("token"));
        assertTrue(toString.contains("key"));
        assertTrue(toString.contains("IOS"));
    }

    @Test
    void testWithNullValues() {
        // Test that null values are handled (though in practice they shouldn't be null)
        DeviceRegistration registration = new DeviceRegistration(null, null, null);

        assertNull(registration.getDeviceToken());
        assertNull(registration.getPublicKey());
        assertNull(registration.getPlatform());
    }

    @Test
    void testWithLongValues() {
        String longToken = "a".repeat(1000);
        String longKey = "b".repeat(2000);
        DeviceRegistration.Platform platform = DeviceRegistration.Platform.IOS;

        DeviceRegistration registration = new DeviceRegistration(longToken, longKey, platform);

        assertEquals(longToken, registration.getDeviceToken());
        assertEquals(longKey, registration.getPublicKey());
        assertEquals(platform, registration.getPlatform());
    }

    @Test
    void testDifferentPlatforms() {
        DeviceRegistration ios = new DeviceRegistration("token", "key", DeviceRegistration.Platform.IOS);
        DeviceRegistration android = new DeviceRegistration("token", "key", DeviceRegistration.Platform.ANDROID);

        assertNotEquals(ios, android);
        assertEquals(DeviceRegistration.Platform.IOS, ios.getPlatform());
        assertEquals(DeviceRegistration.Platform.ANDROID, android.getPlatform());
    }
}

