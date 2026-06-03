package bisq.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public interface Notification {
    String getId();

    String getTitle();

    String getMessage();

    /** Category surfaced to the mobile relay so the client can route / label the
     * notification without parsing the title. Default {@link Category#GENERAL}
     * keeps existing implementations (no category awareness) on the generic
     * banner rather than mis-tagging. */
    default Category getCategory() {
        return Category.GENERAL;
    }

    /**
     * Stable category surfaced to the mobile relay. Must stay in lock-step with
     * {@code BisqFirebaseMessagingService.NotificationCategory} on Android and
     * the iOS NSE category mapping — the {@link #getId() id} is the on-wire
     * value those clients compare against.
     */
    enum Category {
        GENERAL("general"),
        TRADE_UPDATE("trade_update"),
        CHAT_MESSAGE("chat_message"),
        OFFER_UPDATE("offer_update");

        private final String id;

        Category(String id) {
            this.id = id;
        }

        /**
         * Marked {@code @JsonValue} so Jackson serializes this enum as the
         * stable lowercase id (e.g. {@code "chat_message"}) the mobile clients
         * already expect — not the Java constant name {@code CHAT_MESSAGE}.
         */
        @JsonValue
        public String getId() {
            return id;
        }

        /**
         * Forward-compatible deserialization: unknown ids from a newer bisq2
         * (e.g. a future {@code "dispute_alert"}) deserialize to
         * {@link #GENERAL} instead of throwing, so older bisq2 instances and
         * tests don't break when wire payloads from newer producers arrive.
         */
        @JsonCreator
        public static Category fromId(String id) {
            if (id == null) {
                return GENERAL;
            }
            for (Category category : values()) {
                if (category.id.equals(id)) {
                    return category;
                }
            }
            return GENERAL;
        }
    }
}
