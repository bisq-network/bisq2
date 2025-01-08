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

package bisq.dto.presentation.offerbook;

import bisq.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.dto.user.profile.UserProfileDto;
import bisq.dto.user.reputation.ReputationScoreDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// As some of the data are non-trivial to create, we pass the presentation data to the client.
// Similar to bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list.OfferListItem;
public record OfferItemPresentationDto(BisqEasyOfferDto bisqEasyOffer,
                                       @JsonProperty("isMyOffer") boolean isMyOffer,
                                       UserProfileDto makersUserProfile,
                                       String formattedDate,
                                       String formattedQuoteAmount,
                                       String formattedBaseAmount,
                                       String formattedPrice,
                                       String formattedPriceSpec,
                                       List<String> quoteSidePaymentMethods,
                                       List<String> baseSidePaymentMethods,
                                       ReputationScoreDto makersReputationScore) {
}