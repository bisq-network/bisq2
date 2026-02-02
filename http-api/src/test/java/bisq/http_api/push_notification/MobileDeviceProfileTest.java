package bisq.http_api.push_notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobileDeviceProfileTest {

    @Test
    void testConstructorAndGetters() {
        String deviceId = "device-123";
        String deviceToken = "test-device-token-123";
        String publicKeyBase64 = "test-public-key";
        String deviceDescriptor = "iPhone 15 Pro";
        MobileDevicePlatform platform = MobileDevicePlatform.IOS;
        long timestamp = System.currentTimeMillis();

        MobileDeviceProfile profile = new MobileDeviceProfile(deviceId, deviceToken, publicKeyBase64, deviceDescriptor, platform, timestamp);

        assertEquals(deviceId, profile.getDeviceId());
        assertEquals(deviceToken, profile.getDeviceToken());
        assertEquals(publicKeyBase64, profile.getPublicKeyBase64());
        assertEquals(deviceDescriptor, profile.getDeviceDescriptor());
        assertEquals(platform, profile.getPlatform());
        assertEquals(timestamp, profile.getRegistrationTimestamp());
    }

    @Test
    void testEquality() {
        long timestamp = System.currentTimeMillis();
        MobileDeviceProfile profile1 = new MobileDeviceProfile("device1", "token1", "key1", "iPhone", MobileDevicePlatform.IOS, timestamp);
        MobileDeviceProfile profile2 = new MobileDeviceProfile("device1", "token1", "key1", "iPhone", MobileDevicePlatform.IOS, timestamp);
        MobileDeviceProfile profile3 = new MobileDeviceProfile("device2", "token1", "key1", "iPhone", MobileDevicePlatform.IOS, timestamp);

        assertEquals(profile1, profile2);
        assertNotEquals(profile1, profile3);
    }

    @Test
    void testHashCode() {
        long timestamp = System.currentTimeMillis();
        MobileDeviceProfile profile1 = new MobileDeviceProfile("device1", "token1", "key1", "iPhone", MobileDevicePlatform.IOS, timestamp);
        MobileDeviceProfile profile2 = new MobileDeviceProfile("device1", "token1", "key1", "iPhone", MobileDevicePlatform.IOS, timestamp);

        assertEquals(profile1.hashCode(), profile2.hashCode());
    }

    @Test
    void testToString() {
        MobileDeviceProfile profile = new MobileDeviceProfile("device1", "token", "key", "iPhone", MobileDevicePlatform.IOS, 0);
        String toString = profile.toString();

        assertTrue(toString.contains("device1"));
        assertTrue(toString.contains("token"));
        assertTrue(toString.contains("IOS"));
    }

    @Test
    void testWithLongValues() {
        String longToken = "a".repeat(1000);
        String longKey = "b".repeat(2000);
        MobileDevicePlatform platform = MobileDevicePlatform.IOS;

        MobileDeviceProfile profile = new MobileDeviceProfile("device1", longToken, longKey, "iPhone", platform, 0);

        assertEquals(longToken, profile.getDeviceToken());
        assertEquals(longKey, profile.getPublicKeyBase64());
        assertEquals(platform, profile.getPlatform());
    }

    @Test
    void testDifferentPlatforms() {
        MobileDeviceProfile ios = new MobileDeviceProfile("device1", "token", "key", "iPhone", MobileDevicePlatform.IOS, 0);
        MobileDeviceProfile android = new MobileDeviceProfile("device2", "token", "key", "Pixel", MobileDevicePlatform.ANDROID, 0);

        assertNotEquals(ios, android);
        assertEquals(MobileDevicePlatform.IOS, ios.getPlatform());
        assertEquals(MobileDevicePlatform.ANDROID, android.getPlatform());
    }
}

