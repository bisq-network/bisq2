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

package bisq.api.dto.presentation.offerbook;

import bisq.api.dto.offer.bisq_easy.BisqEasyOfferDto;
import bisq.api.dto.user.profile.UserProfileDto;
import bisq.api.dto.user.reputation.ReputationScoreDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// As some of the data are non-trivial to create, we pass the presentation data to the client.
// Similar to bisq.desktop.main.content.bisq_easy.offerbook.offerbook_list.OfferListItem;
// We provide initial values for data which are mutable. Those data need to be provided by websocket events.
public record OfferItemPresentationDto(BisqEasyOfferDto bisqEasyOffer,
                                       @JsonProperty("isMyOffer") boolean isMyOffer,
                                       UserProfileDto userProfile, // The userName inside userProfile can change when multiple nicknames are in the network
                                       String formattedDate,
                                       String formattedQuoteAmount,
                                       String formattedBaseAmount, // Can change by market price changes if float or market price is used
                                       String formattedPrice, // Can change by market price changes if float or market price is used
                                       String formattedPriceSpec,
                                       List<String> quoteSidePaymentMethods,
                                       List<String> baseSidePaymentMethods,
                                       ReputationScoreDto reputationScore, // Can change by reputation changes
                                       /**
                                        * Whether the maker's reputation covers the offer's minimum or fixed amount, with the
                                        * tolerance of the desktop offerbook. Buy offers are always true, and the user's own
                                        * offers are judged like all others. True when a market price is missing, because the
                                        * required score is then unknown; the node still refuses to take such an offer. False
                                        * when the amount overflows in USD. Can change by market price and reputation changes.
                                        */
                                       boolean hasSellerSufficientReputation
) {
}
