package bisq.http_api.push_notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SentNotificationStoreTest {

    private SentNotificationStore store;

    @BeforeEach
    void setUp() {
        store = new SentNotificationStore();
    }

    @Test
    void testMarkAndCheckNotification() {
        String userId = "user1";
        String tradeId = "trade123";
        String eventType = "TRADE_CREATED";

        assertFalse(store.wasNotificationSent(userId, tradeId, eventType));

        store.markNotificationAsSent(userId, tradeId, eventType);

        assertTrue(store.wasNotificationSent(userId, tradeId, eventType));
    }

    @Test
    void testRemoveNotificationsForTrade() {
        String userId = "user1";
        String tradeId = "trade123";

        store.markNotificationAsSent(userId, tradeId, "TRADE_CREATED");
        store.markNotificationAsSent(userId, tradeId, "PAYMENT_SENT");
        store.markNotificationAsSent(userId, "trade456", "TRADE_CREATED");

        assertEquals(3, store.size());

        store.removeNotificationsForTrade(tradeId);

        assertEquals(1, store.size());
        assertFalse(store.wasNotificationSent(userId, tradeId, "TRADE_CREATED"));
        assertFalse(store.wasNotificationSent(userId, tradeId, "PAYMENT_SENT"));
        assertTrue(store.wasNotificationSent(userId, "trade456", "TRADE_CREATED"));
    }

    @Test
    void testPurgeOldNotifications() throws InterruptedException {
        String userId = "user1";
        
        // Mark some notifications as sent
        store.markNotificationAsSent(userId, "trade1", "EVENT1");
        store.markNotificationAsSent(userId, "trade2", "EVENT2");
        
        assertEquals(2, store.size());
        
        // Wait a bit
        Thread.sleep(100);
        
        // Mark another notification
        store.markNotificationAsSent(userId, "trade3", "EVENT3");
        
        assertEquals(3, store.size());
        
        // Purge notifications older than 50ms (should remove first 2)
        int purged = store.purgeOldNotifications(50);
        
        assertEquals(2, purged);
        assertEquals(1, store.size());
        assertTrue(store.wasNotificationSent(userId, "trade3", "EVENT3"));
    }

    @Test
    void testPurgeWithNoOldNotifications() {
        store.markNotificationAsSent("user1", "trade1", "EVENT1");
        
        // Purge with very long age - nothing should be removed
        int purged = store.purgeOldNotifications(TimeUnit.DAYS.toMillis(365));
        
        assertEquals(0, purged);
        assertEquals(1, store.size());
    }

    @Test
    void testClear() {
        store.markNotificationAsSent("user1", "trade1", "EVENT1");
        store.markNotificationAsSent("user2", "trade2", "EVENT2");
        
        assertEquals(2, store.size());
        
        store.clear();
        
        assertEquals(0, store.size());
    }

    @Test
    void testCreateNotificationId() {
        String id = SentNotificationStore.createNotificationId("user123", "trade456", "PAYMENT_SENT");
        assertEquals("user123:trade456:PAYMENT_SENT", id);
    }

    @Test
    void testMultipleEventsForSameTrade() {
        String userId = "user1";
        String tradeId = "trade123";

        store.markNotificationAsSent(userId, tradeId, "TRADE_CREATED");
        store.markNotificationAsSent(userId, tradeId, "PAYMENT_SENT");
        store.markNotificationAsSent(userId, tradeId, "PAYMENT_RECEIVED");

        assertTrue(store.wasNotificationSent(userId, tradeId, "TRADE_CREATED"));
        assertTrue(store.wasNotificationSent(userId, tradeId, "PAYMENT_SENT"));
        assertTrue(store.wasNotificationSent(userId, tradeId, "PAYMENT_RECEIVED"));
        assertFalse(store.wasNotificationSent(userId, tradeId, "TRADE_COMPLETED"));

        assertEquals(3, store.size());
    }

    @Test
    void testBackwardCompatibilityWithLegacyFormat() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Simulate legacy JSON format with Set<String>
        String legacyJson = """
                {
                  "sentNotificationIds": [
                    "user1:trade1:EVENT1",
                    "user2:trade2:EVENT2",
                    "user3:trade3:EVENT3"
                  ]
                }
                """;

        // Deserialize legacy format
        SentNotificationStore migratedStore = mapper.readValue(legacyJson, SentNotificationStore.class);

        // Verify all notifications were migrated
        assertEquals(3, migratedStore.size());
        assertTrue(migratedStore.wasNotificationSent("user1", "trade1", "EVENT1"));
        assertTrue(migratedStore.wasNotificationSent("user2", "trade2", "EVENT2"));
        assertTrue(migratedStore.wasNotificationSent("user3", "trade3", "EVENT3"));

        // Verify timestamps were added (should be non-null)
        assertNotNull(migratedStore.getSentNotificationIdsWithTimestamp());
        assertTrue(migratedStore.getSentNotificationIdsWithTimestamp().values().stream()
                .allMatch(timestamp -> timestamp > 0));
    }

    @Test
    void testNewFormatSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create store with new format
        store.markNotificationAsSent("user1", "trade1", "EVENT1");
        store.markNotificationAsSent("user2", "trade2", "EVENT2");

        // Serialize to JSON
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(store);

        // Verify it contains the new format field
        assertTrue(json.contains("sentNotificationIdsWithTimestamp"));
        assertFalse(json.contains("\"sentNotificationIds\"")); // Old field should not be present

        // Deserialize and verify
        SentNotificationStore deserializedStore = mapper.readValue(json, SentNotificationStore.class);
        assertEquals(2, deserializedStore.size());
        assertTrue(deserializedStore.wasNotificationSent("user1", "trade1", "EVENT1"));
        assertTrue(deserializedStore.wasNotificationSent("user2", "trade2", "EVENT2"));
    }
}

