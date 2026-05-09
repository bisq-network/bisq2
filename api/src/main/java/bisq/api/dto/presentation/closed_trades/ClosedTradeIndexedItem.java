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

package bisq.api.dto.presentation.closed_trades;

// Server-only carrier wrapping the wire DTO with denormalized fields used purely for
// in-memory filtering and sorting in the closed trades endpoint. Not serialized to clients;
// transient strings stay on the server while only the slim dto is broadcast.
//
// Formatted strings (price/amounts) are cached here because BisqEasyTradeFormatter
// allocates Monetary objects and applies locale-aware formatting on every call; doing that
// per search keystroke across the full closed-trade history would be wasteful.
//
// Caveat: cached formatted strings are frozen at creation time. If the user changes the app
// language at runtime, existing items keep the old locale's formatting until the trade is
// re-added (e.g. on restart). Acceptable for now since these strings drive server-side
// filter/sort only and are not sent to the client.
public record ClosedTradeIndexedItem(
        ClosedTradeListItemDto dto,
        String market,
        String directionalTitle,
        String formattedMyRole,
        String formattedPrice,
        String formattedBaseAmount,
        String formattedQuoteAmount,
        String bitcoinSettlementMethodDisplayString,
        String fiatPaymentMethodDisplayString
) {
}
