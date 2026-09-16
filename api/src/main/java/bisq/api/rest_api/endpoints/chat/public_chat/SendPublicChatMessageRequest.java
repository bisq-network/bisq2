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


package bisq.api.rest_api.endpoints.chat.public_chat;

import bisq.api.dto.chat.CitationDto;

import javax.annotation.Nullable;

/**
 * @param senderUserProfileId which of my identities sends; the node's selected identity when absent.
 *                            Unlike private chat, a public channel has no identity of its own.
 */
public record SendPublicChatMessageRequest(String text,
                                           @Nullable CitationDto citation,
                                           @Nullable String senderUserProfileId) {
}
