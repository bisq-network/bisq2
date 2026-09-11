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

package bisq.offer.mu_sig.use_case.take_offer;

import lombok.Getter;

/**
 * Thrown when an offer fails the take-offer trust-boundary validation
 * (take-offer.md, "Offer as root input").
 */
@Getter
public class TakeOfferValidationException extends RuntimeException {
    public enum Reason {
        OWN_OFFER,
        PROTOCOL_TYPE_NOT_SUPPORTED,
        NO_MARKET_PRICE,
        FLOAT_PRICE_OUT_OF_BOUNDS,
        FIXED_PRICE_MARKET_MISMATCH,
        INVALID_OFFER,
        INVALID_PAYMENT_METHOD_SPECS,
        INVALID_OFFER_OPTIONS,
        AMOUNT_OUTSIDE_LIMITS
    }

    private final Reason reason;

    public TakeOfferValidationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }
}
