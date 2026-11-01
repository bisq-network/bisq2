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

package bisq.api.rest_api.endpoints.trades;

import bisq.api.dto.trade.bisq_easy.protocol.BisqEasyTradeStateDto;

/**
 * Minimal state-only response for an authenticated local Bisq 2 client.
 * Never place payment details, profile keys or chat content in this DTO.
 */
public record TradeStatusDto(String tradeId,
                             String offerId,
                             BisqEasyTradeStateDto tradeState,
                             TradeStore store) {
}
