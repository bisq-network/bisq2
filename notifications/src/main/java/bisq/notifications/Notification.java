package bisq.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

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
     * Trade-scoped identifier surfaced to the mobile relay so the client can
     * deep-link a notification tap straight to the relevant trade screen
     * ({@code bisq://OpenTrade/<tradeId>}) instead of the generic trade list.
     * <p>
     * Default {@link Optional#empty()} keeps non-trade notifications (alerts,
     * generic announcements) on the category-based fallback route — mobile
     * clients use the category to choose the destination when no tradeId is
     * present (see {@code BisqFirebaseMessagingService.PushNotification.from}).
     */
    default Optional<String> getTradeId() {
        return Optional.empty();
    }

    /**
     * The chat channel this notification came from, if it has one. Surfaced to the
     * mobile relay so a tap on a private message can deep-link straight to that
     * conversation ({@code bisq://PrivateChat/<channelId>}) instead of the generic
     * fallback.
     * <p>
     * Report the channel whenever there is one; whether it reaches the wire is decided
     * at the transport boundary by {@code MobileNotificationPayload#from(Notification)}.
     * Default {@link Optional#empty()} keeps non-chat notifications unaffected.
     */
    default Optional<String> getChannelId() {
        return Optional.empty();
    }

    /**
     * The counterparty this notification is about, if there is one.
     * <p>
     * Surfaced as its own field rather than leaving the client to read {@link #getTitle()},
     * because that title is built here with {@code Res.get(...)} — in the <i>node's</i> locale.
     * A client that displayed it would show the banner in whatever language the node runs in.
     * With the name as data, the client composes the banner from its own resources, in the
     * user's locale, and never has to display {@link #getMessage()} — which for a chat
     * notification is the message body.
     * <p>
     * Only chat notifications report one, so the field never accompanies a trade update. That
     * is a property of the producers, not a rule imposed on the wire: nothing here forbids a
     * future notification type from reporting a name too.
     */
    default Optional<String> getPeerUserName() {
        return Optional.empty();
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
