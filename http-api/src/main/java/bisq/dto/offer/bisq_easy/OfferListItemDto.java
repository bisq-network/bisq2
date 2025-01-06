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

package bisq.dto.offer.bisq_easy;

import bisq.dto.user.profile.UserProfileItemDto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@EqualsAndHashCode
public class OfferListItemDto {
    private final BisqEasyOfferDto bisqEasyOffer;
    @JsonProperty("isMyOffer")
    private final boolean isMyOffer;
    private final UserProfileItemDto userProfileItem;
    private final String formattedDate;
    private final String formattedQuoteAmount;
    private final String formattedBaseAmount;
    private final String formattedPrice;
    private final String formattedPriceSpec;
    private final List<String> quoteSidePaymentMethods;
    private final List<String> baseSidePaymentMethods;

    @JsonCreator
    public OfferListItemDto(BisqEasyOfferDto bisqEasyOffer,
                            boolean isMyOffer,
                            UserProfileItemDto userProfileItem,
                            String formattedDate,
                            String formattedQuoteAmount,
                            String formattedBaseAmount,
                            String formattedPrice,
                            String formattedPriceSpec,
                            List<String> quoteSidePaymentMethods,
                            List<String> baseSidePaymentMethods) {
        this.bisqEasyOffer = bisqEasyOffer;
        this.isMyOffer = isMyOffer;
        this.userProfileItem = userProfileItem;
        this.formattedDate = formattedDate;
        this.formattedQuoteAmount = formattedQuoteAmount;
        this.formattedBaseAmount = formattedBaseAmount;
        this.formattedPrice = formattedPrice;
        this.formattedPriceSpec = formattedPriceSpec;
        this.quoteSidePaymentMethods = quoteSidePaymentMethods;
        this.baseSidePaymentMethods = baseSidePaymentMethods;
    }
}
