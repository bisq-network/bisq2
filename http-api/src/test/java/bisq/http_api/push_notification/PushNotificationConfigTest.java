package bisq.http_api.push_notification;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PushNotificationConfigTest {

    @Test
    void testConstructorAndGetters() {
        PushNotificationConfig config = new PushNotificationConfig(true, "http://localhost:8080");

        assertTrue(config.isEnabled());
        assertEquals("http://localhost:8080", config.getBisqRelayUrl());
    }

    @Test
    void testFromConfig() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("bisqRelayUrl", "http://relay.example.com");

        Config typesafeConfig = ConfigFactory.parseMap(configMap);
        PushNotificationConfig config = PushNotificationConfig.from(typesafeConfig);

        assertTrue(config.isEnabled());
        assertEquals("http://relay.example.com", config.getBisqRelayUrl());
    }

    @Test
    void testFromConfigDisabled() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", false);
        configMap.put("bisqRelayUrl", "http://localhost:8080");

        Config typesafeConfig = ConfigFactory.parseMap(configMap);
        PushNotificationConfig config = PushNotificationConfig.from(typesafeConfig);

        assertFalse(config.isEnabled());
        assertEquals("http://localhost:8080", config.getBisqRelayUrl());
    }

    @Test
    void testFromConfigWithOnionAddress() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("bisqRelayUrl", "http://example123456789.onion");

        Config typesafeConfig = ConfigFactory.parseMap(configMap);
        PushNotificationConfig config = PushNotificationConfig.from(typesafeConfig);

        assertTrue(config.isEnabled());
        assertEquals("http://example123456789.onion", config.getBisqRelayUrl());
    }

    @Test
    void testFromConfigWithHttpsUrl() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("enabled", true);
        configMap.put("bisqRelayUrl", "https://relay.example.com:8443");

        Config typesafeConfig = ConfigFactory.parseMap(configMap);
        PushNotificationConfig config = PushNotificationConfig.from(typesafeConfig);

        assertTrue(config.isEnabled());
        assertEquals("https://relay.example.com:8443", config.getBisqRelayUrl());
    }

    @Test
    void testDisabledConfig() {
        PushNotificationConfig config = new PushNotificationConfig(false, "http://localhost:8080");

        assertFalse(config.isEnabled());
        // URL should still be accessible even when disabled
        assertEquals("http://localhost:8080", config.getBisqRelayUrl());
    }

    @Test
    void testWithEmptyUrl() {
        PushNotificationConfig config = new PushNotificationConfig(true, "");

        assertTrue(config.isEnabled());
        assertEquals("", config.getBisqRelayUrl());
    }

    @Test
    void testWithNullUrl() {
        PushNotificationConfig config = new PushNotificationConfig(true, null);

        assertTrue(config.isEnabled());
        assertNull(config.getBisqRelayUrl());
    }
}

