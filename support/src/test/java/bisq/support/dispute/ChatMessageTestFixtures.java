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

package bisq.support.dispute;

public final class ChatMessageTestFixtures {
    private ChatMessageTestFixtures() {
    }

    /**
     * Returns the chat message with a citation whose chat message ID has 51 characters. A node of version 2.1.13
     * accepts such a message from its trade peer and puts it into a mediation request. This version rejects the
     * message at parsing, because an ID has at most 50 characters.
     */
    public static bisq.chat.protobuf.ChatMessage withTooLongCitationId(bisq.chat.protobuf.ChatMessage chatMessage) {
        return chatMessage.toBuilder()
                .setCitation(bisq.chat.protobuf.Citation.newBuilder()
                        .setAuthorUserProfileId(chatMessage.getAuthorUserProfileId())
                        .setText("cited text")
                        .setChatMessageId("x".repeat(51)))
                .build();
    }
}
