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

package bisq.desktop.main.content.mu_sig.offer.offer_details;

import bisq.common.monetary.Monetary;

import java.util.Optional;

class MuSigOfferDetailsRecords {

    record SecurityDepositInfo(double percentValue, Optional<Amount> amount, String percentText) {
    }

    //
    // Price details records
    //

    interface PriceDetails {
    }

    record MarketPriceDetails() implements PriceDetails {
    }

    record FixAtMarketPriceDetails(
            String marketPriceAsString,
            String marketCodes) implements PriceDetails {
    }

    record FloatPriceDetails(
            String marketPriceAsString,
            String marketCodes,
            String percentAsString,
            String aboveOrBelow
    ) implements PriceDetails {
    }

    record FixPriceDetails(
            String marketPriceAsString,
            String marketCodes,
            String percentAsString,
            String aboveOrBelow) implements PriceDetails {
    }

    //
    // Offer Amounts
    //

    interface Amount {
    }

    record FixedAmount(Monetary amount) implements Amount {
    }

    record RangeAmount(Monetary minAmount, Monetary maxAmount) implements Amount {
    }

    interface OfferAmounts {
        Amount baseSide();

        Amount quoteSide();
    }

    record RangeOfferAmounts(
            RangeAmount baseSide,
            RangeAmount quoteSide) implements OfferAmounts {
    }

    record FixedOfferAmounts(
            FixedAmount baseSide,
            FixedAmount quoteSide) implements OfferAmounts {
    }

    interface FormattedOfferAmounts {
    }

    record FormattedRangeOfferAmounts(
            String formattedMinBaseSideAmount,
            String formattedMaxBaseSideAmount,
            String formattedMinQuoteSideAmount,
            String formattedMaxQuoteSideAmount) implements FormattedOfferAmounts {
    }

    record FormattedFixedOfferAmounts(
            String formattedBaseSideAmount,
            String formattedQuoteSideAmount) implements FormattedOfferAmounts {
    }
}
