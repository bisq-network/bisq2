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

package bisq.trade.bisq_easy.validation;

/**
 * The node has no market prices with which it can value a trade amount in USD: a price is missing, outdated, or the
 * two prices are not from about the same time. The condition is temporary and is not a fault of the trade peer, so
 * a client can ask the user to try again later.
 * <p>
 * It extends {@link IllegalArgumentException} so that callers which handle invalid input keep their behavior: the
 * REST API answers 400, and the maker reports the rejection to the taker as an unknown failure.
 */
public final class MarketPriceNotCurrentException extends IllegalArgumentException {
    public MarketPriceNotCurrentException(String message) {
        super(message);
    }
}
