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

package bisq.http_api.rest_api.domain.market_price;

import bisq.dto.common.monetary.PriceQuoteDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Response DTO for market price quotes.
 */
public record MarketPriceResponse(
        @Schema(description = "Map of currency codes to market price quotes") Map<String, PriceQuoteDto> quotes) {
}
