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


package bisq.api.rest_api.endpoints.chat;

import bisq.api.dto.chat.CitationDto;
import bisq.chat.ChatMessage;
import bisq.chat.Citation;
import bisq.chat.reactions.Reaction;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRequestValidationTest {
    private static final String AUTHOR_PROFILE_ID = "0123456789012345678901234567890123456789";

    @Test
    void anEmptyOrAbsentTextIsAnError() {
        assertThat(ChatRequestValidation.textError("")).isPresent();
        assertThat(ChatRequestValidation.textError(null)).isPresent();
    }

    /** The domain limit is inclusive. */
    @Test
    void theTextLimitIsInclusive() {
        assertThat(ChatRequestValidation.textError("x".repeat(ChatMessage.MAX_TEXT_LENGTH))).isEmpty();
        assertThat(ChatRequestValidation.textError("x".repeat(ChatMessage.MAX_TEXT_LENGTH + 1))).isPresent();
    }

    @Test
    void anAbsentCitationIsFine() {
        assertThat(ChatRequestValidation.citationError(null)).isEmpty();
    }

    @Test
    void aCitationMissingAFieldIsAnError() {
        assertThat(ChatRequestValidation.citationError(new CitationDto(null, "text", Optional.empty()))).isPresent();
        assertThat(ChatRequestValidation.citationError(new CitationDto(AUTHOR_PROFILE_ID, null, Optional.empty()))).isPresent();
    }

    /**
     * Not a limit but an exact width, and the reason it is checked here is the same as for the text:
     * the one who would otherwise catch it is {@code NetworkDataValidation.validateProfileId}, whose
     * message appends the id it rejected. Leaving it to the {@code Citation} constructor answers a 400
     * that hands the client back what it just sent.
     */
    @Test
    void aCitationWithAMalformedAuthorIdIsAnErrorThatDoesNotEchoIt() {
        String malformed = "x".repeat(200);

        Optional<String> error = ChatRequestValidation.citationError(
                new CitationDto(malformed, "text", Optional.empty()));

        assertThat(error).isPresent();
        assertThat(error.get()).doesNotContain(malformed);
    }

    /** Inclusive like the text limit, and the same limit the Citation constructor enforces. */
    @Test
    void theCitationLimitIsInclusive() {
        assertThat(ChatRequestValidation.citationError(citationOf("x".repeat(Citation.MAX_TEXT_LENGTH)))).isEmpty();
        assertThat(ChatRequestValidation.citationError(citationOf("x".repeat(Citation.MAX_TEXT_LENGTH + 1)))).isPresent();
    }

    private static CitationDto citationOf(String text) {
        return new CitationDto(AUTHOR_PROFILE_ID, text, Optional.empty());
    }

    @Test
    void aReactionIdOutsideTheRangeIsNotParsed() {
        assertThat(ChatRequestValidation.parseReaction(-1)).isEmpty();
        assertThat(ChatRequestValidation.parseReaction(Reaction.values().length)).isEmpty();
    }

    /**
     * The wire contract rather than the parser. {@code ChatMessageReaction.reactionId} is the ordinal
     * of {@code Reaction} — it is what goes into proto and into
     * {@code CommonPublicChatMessageReactionDto} — so reordering the enum changes the meaning of every
     * reaction already stored and in flight, which is what {@code docs/dev/backward-compatibility.md}
     * rules out.
     * <p>
     * Spelled out with literals because the obvious form, {@code contains(Reaction.values()[0])}, is
     * the same expression {@code parseReaction} evaluates: it holds true for whatever the order
     * happens to be. The enum is declared in the chat module, but the API is what publishes these ids,
     * so this is where a reorder has to be caught.
     */
    @Test
    void aReactionIdMeansTheSameReactionItAlwaysDid() {
        assertThat(ChatRequestValidation.parseReaction(0)).contains(Reaction.THUMBS_UP);
        assertThat(ChatRequestValidation.parseReaction(1)).contains(Reaction.THUMBS_DOWN);
        assertThat(ChatRequestValidation.parseReaction(2)).contains(Reaction.HAPPY);
        assertThat(ChatRequestValidation.parseReaction(3)).contains(Reaction.LAUGH);
        assertThat(ChatRequestValidation.parseReaction(4)).contains(Reaction.HEART);
        assertThat(ChatRequestValidation.parseReaction(5)).contains(Reaction.PARTY);
        // Appending a reaction is fine and is meant to fail here first, so the id above it is chosen
        // deliberately rather than inherited from wherever the new constant was pasted.
        assertThat(ChatRequestValidation.parseReaction(6)).isEmpty();
    }
}
