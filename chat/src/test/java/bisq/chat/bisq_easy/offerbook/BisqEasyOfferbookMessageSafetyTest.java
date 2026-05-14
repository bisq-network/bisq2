package bisq.chat.bisq_easy.offerbook;

import bisq.chat.protobuf.ChatChannelDomain;
import bisq.chat.protobuf.ChatMessageType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BisqEasyOfferbookMessageSafetyTest {

    private static bisq.chat.protobuf.ChatMessage.Builder baseMessageBuilder() {
        return bisq.chat.protobuf.ChatMessage.newBuilder()
                .setId("test-msg-id")
                .setChatChannelDomain(ChatChannelDomain.CHATCHANNELDOMAIN_BISQ_EASY_OFFERBOOK)
                .setChannelId("test-channel")
                .setAuthorUserProfileId("test-author")
                .setDate(System.currentTimeMillis())
                .setWasEdited(false)
                .setChatMessageType(ChatMessageType.CHATMESSAGETYPE_TEXT);
    }

    @Nested
    @DisplayName("T3: Version-based offer skipping")
    class VersionBasedOfferSkipping {

        @Test
        @DisplayName("T3a: version 0 with no offer field passes normally")
        void version_zero_no_offer_passes() {
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(0)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertFalse(result.isUnsupportedOffer());
            assertTrue(result.getBisqEasyOffer().isEmpty());
            assertEquals(0, result.getMinSupportedVersion());
        }

        @Test
        @DisplayName("T3b: version 999 (future) skips gracefully, sets unsupported flag")
        void future_version_skips_gracefully() {
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(999)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertFalse(result.hasBisqEasyOffer(), "Offer should be empty because version is too high");
            assertTrue(result.isUnsupportedOffer(), "Should be flagged as unsupported");
            assertEquals(999, result.getMinSupportedVersion());
        }

        @Test
        @DisplayName("T3c: absent version field treated as version 0, processes normally")
        void absent_version_treated_as_zero() {
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertFalse(result.isUnsupportedOffer());
            assertEquals(0, result.getMinSupportedVersion());
        }

        @Test
        @DisplayName("T3d: exactly CURRENT_VERSION processes normally")
        void exact_current_version_passes() {
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(BisqEasyOfferbookMessage.CURRENT_VERSION)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertFalse(result.isUnsupportedOffer());
            assertEquals(BisqEasyOfferbookMessage.CURRENT_VERSION, result.getMinSupportedVersion());
        }

        @Test
        @DisplayName("T3e: CURRENT_VERSION + 1 is skipped")
        void version_above_current_is_skipped() {
            int futureVersion = BisqEasyOfferbookMessage.CURRENT_VERSION + 1;
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(futureVersion)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertTrue(result.isUnsupportedOffer());
            assertTrue(result.getBisqEasyOffer().isEmpty());
            assertEquals(futureVersion, result.getMinSupportedVersion());
        }
    }

    @Nested
    @DisplayName("T4: Try/catch safety net")
    class TryCatchSafetyNet {

        @Test
        @DisplayName("T4a: corrupted offer payload (empty Offer proto) caught by try/catch")
        void corrupted_offer_payload_caught() {
            bisq.offer.protobuf.Offer emptyOffer = bisq.offer.protobuf.Offer.newBuilder().build();
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(0)
                            .setBisqEasyOffer(emptyOffer)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertTrue(result.getBisqEasyOffer().isEmpty(),
                    "Corrupted offer should result in empty Optional");
            assertTrue(result.isUnsupportedOffer(),
                    "Should be flagged as unsupported");
        }

        @Test
        @DisplayName("T4b: offer with minimal garbage data caught by try/catch")
        void offer_with_garbage_caught() {
            bisq.offer.protobuf.Offer badOffer = bisq.offer.protobuf.Offer.newBuilder()
                    .setId("bad-offer")
                    .setDate(System.currentTimeMillis())
                    .build();
            bisq.chat.protobuf.BisqEasyOfferbookMessage offerbookMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(0)
                            .setBisqEasyOffer(badOffer)
                            .build();
            bisq.chat.protobuf.ChatMessage proto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(offerbookMsg)
                    .build();

            BisqEasyOfferbookMessage result = BisqEasyOfferbookMessage.fromProto(proto);

            assertTrue(result.getBisqEasyOffer().isEmpty(),
                    "Malformed offer should result in empty Optional");
            assertTrue(result.isUnsupportedOffer());
        }
    }

    @Nested
    @DisplayName("T6: Round-trip with version field")
    class RoundTripWithVersion {

        @Test
        @DisplayName("T6a: stablecoin offer message with version 1 roundtrips")
        void stablecoin_message_roundtrips_with_version() {
            BisqEasyOfferbookMessage original = new BisqEasyOfferbookMessage(
                    "channel-1",
                    "author-1",
                    java.util.Optional.empty(),
                    java.util.Optional.of("offer text"),
                    java.util.Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    1);

            bisq.chat.protobuf.ChatMessage proto = original.toProto(false);
            BisqEasyOfferbookMessage restored = BisqEasyOfferbookMessage.fromProto(proto);

            assertEquals(1, restored.getMinSupportedVersion());
            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getChannelId(), restored.getChannelId());
            assertFalse(restored.isUnsupportedOffer());
        }

        @Test
        @DisplayName("T6b: fiat offer message with version 0 roundtrips (backward compat)")
        void fiat_message_roundtrips_with_zero_version() {
            BisqEasyOfferbookMessage original = new BisqEasyOfferbookMessage(
                    "channel-2",
                    "author-2",
                    java.util.Optional.empty(),
                    java.util.Optional.of("fiat offer text"),
                    java.util.Optional.empty(),
                    System.currentTimeMillis(),
                    false,
                    0);

            bisq.chat.protobuf.ChatMessage proto = original.toProto(false);
            BisqEasyOfferbookMessage restored = BisqEasyOfferbookMessage.fromProto(proto);

            assertEquals(0, restored.getMinSupportedVersion());
            assertFalse(restored.isUnsupportedOffer());
        }
    }

    @Nested
    @DisplayName("T7: Upgrade popup flag")
    class UpgradePopupFlag {

        @Test
        @DisplayName("T7a: unsupported offer flag is only set on version mismatch or deserialization failure")
        void unsupported_flag_semantics() {
            bisq.chat.protobuf.BisqEasyOfferbookMessage normalMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(0)
                            .build();
            bisq.chat.protobuf.ChatMessage normalProto = baseMessageBuilder()
                    .setBisqEasyOfferbookMessage(normalMsg)
                    .build();

            bisq.chat.protobuf.BisqEasyOfferbookMessage futureMsg =
                    bisq.chat.protobuf.BisqEasyOfferbookMessage.newBuilder()
                            .setMinSupportedVersion(999)
                            .build();
            bisq.chat.protobuf.ChatMessage futureProto = baseMessageBuilder()
                    .setId("future-msg-id")
                    .setBisqEasyOfferbookMessage(futureMsg)
                    .build();

            BisqEasyOfferbookMessage normalResult = BisqEasyOfferbookMessage.fromProto(normalProto);
            BisqEasyOfferbookMessage futureResult = BisqEasyOfferbookMessage.fromProto(futureProto);

            assertFalse(normalResult.isUnsupportedOffer(), "Normal message should not be flagged");
            assertTrue(futureResult.isUnsupportedOffer(), "Future version message should be flagged");
        }
    }
}
