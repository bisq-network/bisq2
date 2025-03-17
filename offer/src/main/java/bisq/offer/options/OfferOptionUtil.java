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

package bisq.offer.options;

import bisq.offer.Offer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class OfferOptionUtil {
    public static List<OfferOption> fromTradeTermsAndReputationScore(String makersTradeTerms, long requiredTotalReputationScore) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        if (requiredTotalReputationScore > 0) {
            offerOptions.add(new ReputationOption(requiredTotalReputationScore));
        }
        return offerOptions;
    }

    public static List<OfferOption> fromTradeTerms(String makersTradeTerms) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        return offerOptions;
    }

    public static Optional<TradeTermsOption> findTradeTermsOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof TradeTermsOption)
                .map(option -> (TradeTermsOption) option)
                .findAny();
    }

    public static Optional<ReputationOption> findReputationOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof ReputationOption)
                .map(option -> (ReputationOption) option)
                .findAny();
    }

    public static Optional<CollateralOption> findCollateralOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof CollateralOption)
                .map(option -> (CollateralOption) option)
                .findAny();
    }

    public static Optional<FiatPaymentOption> findFiatPaymentOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FiatPaymentOption)
                .map(option -> (FiatPaymentOption) option)
                .findAny();
    }

    public static Optional<FeeOption> findFeeOption(Collection<OfferOption> offerOptions) {
        return offerOptions.stream()
                .filter(option -> option instanceof FeeOption)
                .map(option -> (FeeOption) option)
                .findAny();
    }

    public static Optional<String> findMakersTradeTerms(Offer<?, ?> offer) {
        return OfferOptionUtil.findTradeTermsOption(offer.getOfferOptions()).stream().findAny()
                .map(TradeTermsOption::getMakersTradeTerms);
    }
}