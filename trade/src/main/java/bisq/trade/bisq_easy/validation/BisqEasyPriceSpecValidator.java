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

import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;

import static com.google.common.base.Preconditions.checkArgument;

public final class BisqEasyPriceSpecValidator {
    private BisqEasyPriceSpecValidator() {
    }

    public static void validatePriceSpec(BisqEasyOffer offer, PriceSpec contractPriceSpec) {
        validatePriceSpec(offer.getPriceSpec(), contractPriceSpec);
    }

    // The taker creates the contract, and the maker derives the expected amounts from the contract price spec.
    // Another price spec than the offer's would let the taker choose the price.
    static void validatePriceSpec(PriceSpec offerPriceSpec, PriceSpec contractPriceSpec) {
        checkArgument(offerPriceSpec.equals(contractPriceSpec),
                "Contract price spec must equal the offer price spec. offerPriceSpec=%s; contractPriceSpec=%s",
                offerPriceSpec, contractPriceSpec);
    }
}
