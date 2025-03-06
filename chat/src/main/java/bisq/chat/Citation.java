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

import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode
public final class Citation implements NetworkProto {
    public static final int MAX_TEXT_LENGTH = 1000;

    private final String authorUserProfileId;
    private final String text;

    // Added with v2.1.7
    @EqualsAndHashCode.Exclude
    @ExcludeForHash
    private final String chatMessageId;

    public Citation(String authorUserProfileId, String text, Optional<String> chatMessageId) {
        this.authorUserProfileId = authorUserProfileId;
        this.text = text;
        this.chatMessageId = chatMessageId.orElse("");

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(authorUserProfileId);
        NetworkDataValidation.validateText(text, MAX_TEXT_LENGTH);
    }

    @Override
    public bisq.chat.protobuf.Citation.Builder getBuilder(boolean serializeForHash) {
        return bisq.chat.protobuf.Citation.newBuilder()
                .setAuthorUserProfileId(authorUserProfileId)
                .setText(text)
                .setChatMessageId(chatMessageId);
    }

    @Override
    public bisq.chat.protobuf.Citation toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static Citation fromProto(bisq.chat.protobuf.Citation proto) {
        return new Citation(proto.getAuthorUserProfileId(),
                proto.getText(), Optional.of(proto.getChatMessageId()));
    }

    public boolean isValid() {
        return authorUserProfileId != null && !authorUserProfileId.isEmpty()
                && text != null && !text.isEmpty()
                && chatMessageId != null;
    }
}
