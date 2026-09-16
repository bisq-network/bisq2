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
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Input checks shared by the chat endpoints: each returns the message for a 400, or empty when the
 * input is fine.
 * <p>
 * Client input must never surface as a 500: {@code Reaction.values()[reactionId]} throws on an
 * out-of-range id, and {@code Citation} throws NPE rather than IllegalArgumentException on a null
 * field.
 * <p>
 * The size checks on the citation are not here because the domain would miss them, but because of what
 * the domain says when it catches them: {@code NetworkDataValidation.validateText},
 * {@code validateProfileId} and {@code validateId} all append the offending input to their message, so
 * leaving the citation to the {@code Citation} constructor answers a 400 that hands the client back
 * what it just sent — the thousand characters of quoted text, the author id, or the id of the cited
 * message. Emptiness is a different case — no message class rejects it,
 * {@code ChatMessage.verify} only bounds the length — so that check is the only one there is. The
 * text's maximum is likewise the only one for private chat, whose message class does not verify on
 * construction; {@code CommonPublicChatMessage} does, and checking it here anyway keeps the two
 * endpoints answering the same way.
 */
public final class ChatRequestValidation {
    private ChatRequestValidation() {
    }

    public static Optional<String> textError(@Nullable String text) {
        if (StringUtils.isEmpty(text)) {
            return Optional.of("text must not be empty.");
        }
        if (text.length() > ChatMessage.MAX_TEXT_LENGTH) {
            return Optional.of("text must not be longer than " + ChatMessage.MAX_TEXT_LENGTH + " characters.");
        }
        return Optional.empty();
    }

    public static Optional<String> citationError(@Nullable CitationDto citation) {
        if (citation == null) {
            return Optional.empty();
        }
        if (citation.authorUserProfileId() == null || citation.text() == null) {
            return Optional.of("citation requires authorUserProfileId and text.");
        }
        if (citation.authorUserProfileId().length() != NetworkDataValidation.PROFILE_ID_LENGTH) {
            return Optional.of("citation authorUserProfileId must be "
                    + NetworkDataValidation.PROFILE_ID_LENGTH + " characters.");
        }
        if (citation.text().length() > Citation.MAX_TEXT_LENGTH) {
            return Optional.of("citation text must not be longer than " + Citation.MAX_TEXT_LENGTH + " characters.");
        }
        if (citation.chatMessageId().isPresent()
                && citation.chatMessageId().get().length() > NetworkDataValidation.MAX_ID_LENGTH) {
            return Optional.of("citation chatMessageId must not be longer than "
                    + NetworkDataValidation.MAX_ID_LENGTH + " characters.");
        }
        return Optional.empty();
    }

    /** Empty when the id is out of range. */
    public static Optional<Reaction> parseReaction(int reactionId) {
        Reaction[] reactions = Reaction.values();
        if (reactionId < 0 || reactionId >= reactions.length) {
            return Optional.empty();
        }
        return Optional.of(reactions[reactionId]);
    }
}
