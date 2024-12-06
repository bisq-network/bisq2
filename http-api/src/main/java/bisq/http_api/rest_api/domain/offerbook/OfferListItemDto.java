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

package bisq.http_api.rest_api.domain.offerbook;

import bisq.offer.Direction;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@Schema(name = "OfferListItem", description = "Detailed information about an offer in the offerbook.")
public class OfferListItemDto {
    @Schema(description = "Unique identifier for the message.", example = "msg-123456")
    private final String messageId;
    @Schema(description = "Unique identifier for the offer.", example = "offer-987654")
    private final String offerId;
    @JsonProperty("isMyMessage")
    @Schema(description = "Indicates whether this message belongs to the current user.", example = "true")
    private final boolean isMyMessage;
    @Schema(description = "Direction of the offer (buy or sell).", implementation = Direction.class)
    private final Direction direction;
    @Schema(description = "Quote currency code of the offer.", example = "USD")
    private final String quoteCurrencyCode;
    @Schema(description = "Title of the offer.", example = "Buy 1 BTC at $30,000")
    private final String offerTitle;
    @Schema(description = "Timestamp of the offer in milliseconds since epoch.", example = "1672531200000")
    private final long date;
    @Schema(description = "Formatted date string for the offer.", example = "2023-01-01 12:00:00")
    private final String formattedDate;
    @Schema(description = "Anonymous pseudonym of the user.", example = "Nym123")
    private final String nym;
    @Schema(description = "Username of the offer's creator.", example = "Alice")
    private final String userName;
    @Schema(description = "Reputation score of the user who created the offer.", implementation = ReputationScoreDto.class)
    private final ReputationScoreDto reputationScore;
    @Schema(description = "Formatted amount for the quoted currency.", example = "30,000 USD")
    private final String formattedQuoteAmount;
    @Schema(description = "Formatted price of the offer.", example = "$30,000 per BTC")
    private final String formattedPrice;
    @Schema(description = "List of payment methods supported by the quote side.", example = "[\"Bank Transfer\", \"PayPal\"]")
    private final List<String> quoteSidePaymentMethods;
    @Schema(description = "List of payment methods supported by the base side.", example = "[\"Cash Deposit\"]")
    private final List<String> baseSidePaymentMethods;
    @Schema(description = "Supported language codes for the offer.", example = "en,es,fr")
    private final String supportedLanguageCodes;

    public OfferListItemDto(String messageId,
                            String offerId,
                            boolean isMyMessage,
                            Direction direction,
                            String quoteCurrencyCode,
                            String offerTitle,
                            long date,
                            String formattedDate,
                            String nym,
                            String userName,
                            ReputationScoreDto reputationScore,
                            String formattedQuoteAmount,
                            String formattedPrice,
                            List<String> quoteSidePaymentMethods,
                            List<String> baseSidePaymentMethods,
                            String supportedLanguageCodes) {
        this.messageId = messageId;
        this.offerId = offerId;
        this.isMyMessage = isMyMessage;
        this.direction = direction;
        this.quoteCurrencyCode = quoteCurrencyCode;
        this.offerTitle = offerTitle;
        this.date = date;
        this.formattedDate = formattedDate;
        this.nym = nym;
        this.userName = userName;
        this.reputationScore = reputationScore;
        this.formattedQuoteAmount = formattedQuoteAmount;
        this.formattedPrice = formattedPrice;
        this.quoteSidePaymentMethods = quoteSidePaymentMethods;
        this.baseSidePaymentMethods = baseSidePaymentMethods;
        this.supportedLanguageCodes = supportedLanguageCodes;
    }
}
