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

package bisq.chat;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CitationTest {
    private static final String AUTHOR_PROFILE_ID = "0123456789012345678901234567890123456789";

    /**
     * The cited id is the only field of a citation that nothing bounded: {@code verify} covered the
     * author id and the text, and this one went into proto and out to the network at whatever length
     * the sender chose. The limit is the one every other id in the domain gets from
     * {@code NetworkDataValidation.validateId}, and 50 is spelled out rather than read from there so
     * that widening the shared limit does not silently widen what goes on the wire from here.
     */
    @Test
    void aChatMessageIdOverFiftyCharactersIsRejected() {
        assertThatThrownBy(() -> citationCiting("x".repeat(51)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theChatMessageIdLimitIsInclusive() {
        assertThatNoException().isThrownBy(() -> citationCiting("x".repeat(50)));
    }

    /** Citing a message is optional, and the check must not turn the absent case into a failure. */
    @Test
    void anAbsentChatMessageIdIsAccepted() {
        assertThatNoException().isThrownBy(
                () -> new Citation(AUTHOR_PROFILE_ID, "text", Optional.empty()));
    }

    /**
     * The other way in. A citation reaching this node from the network is built by {@code fromProto}
     * rather than by the endpoint, so the bound has to sit in {@code verify} to cover both; a check
     * placed in the API layer alone would leave this path open. The proto is assembled by hand here
     * because the constructor is exactly what a hostile peer does not go through.
     */
    @Test
    void aCitationArrivingFromAPeerWithAnOverlongChatMessageIdIsRejected() {
        bisq.chat.protobuf.Citation proto = bisq.chat.protobuf.Citation.newBuilder()
                .setAuthorUserProfileId(AUTHOR_PROFILE_ID)
                .setText("text")
                .setChatMessageId("x".repeat(51))
                .build();

        assertThatThrownBy(() -> Citation.fromProto(proto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * That the bound did not cost the field its delivery. {@code serializeForHash} is false because
     * the id is {@code @ExcludeForHash}: the hash form omits it by design, and the wire form is what
     * a peer actually reads.
     */
    @Test
    void aChatMessageIdWithinTheLimitStillReachesTheWire() {
        Citation citation = citationCiting("cited-message-id");

        Citation restored = Citation.fromProto(citation.toProto(false));

        assertThat(restored.getChatMessageId()).contains("cited-message-id");
    }

    private static Citation citationCiting(String chatMessageId) {
        return new Citation(AUTHOR_PROFILE_ID, "text", Optional.of(chatMessageId));
    }
}
