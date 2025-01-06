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

package bisq.dto.chat.bisq_easy.offerbook;

import bisq.dto.chat.ChatMessageTypeDto;
import bisq.dto.chat.CitationDto;
import bisq.dto.offer.bisq_easy.BisqEasyOfferDto;

import java.util.Optional;

public record BisqEasyOfferbookMessageDto(String id,
                                          String channelId,
                                          String authorUserProfileId,
                                          Optional<BisqEasyOfferDto> bisqEasyOffer,
                                          Optional<String> text,
                                          Optional<CitationDto> citation,
                                          long date,
                                          boolean wasEdited,
                                          ChatMessageTypeDto chatMessageType) {
}
