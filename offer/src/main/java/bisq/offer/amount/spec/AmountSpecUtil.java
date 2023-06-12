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

package bisq.offer.amount.spec;

import bisq.common.monetary.Monetary;

import java.util.Optional;

/**
 * Util for getting the AmountSpec implementation and amounts from the AmountSpec.
 * Should not be used by client code directly but rather an util for amount package internal use.
 * AmountUtil and OfferAmountFormatter exposes public APIs.
 */
public class AmountSpecUtil {

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // BaseAmount: Monetary from BaseSideAmountSpec otherwise empty Optional
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findBaseSideFixedAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findBaseSideFixedAmountSpec(amountSpec)
                .map(BaseSideFixedAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findBaseSideMinAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findBaseSideRangeAmountSpec(amountSpec)
                .map(BaseSideRangeAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findBaseSideMaxAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findBaseSideRangeAmountSpec(amountSpec)
                .map(BaseSideRangeAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findBaseSideMinOrFixedAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findBaseSideMinAmountFromSpec(amountSpec, baseCurrencyCode)
                .or(() -> findBaseSideFixedAmountFromSpec(amountSpec, baseCurrencyCode));
    }

    public static Optional<Monetary> findBaseSideMaxOrFixedAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findBaseSideMaxAmountFromSpec(amountSpec, baseCurrencyCode)
                .or(() -> findBaseSideFixedAmountFromSpec(amountSpec, baseCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount: Monetary from QuoteAmountSpec otherwise empty Optional
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findQuoteSideFixedAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findQuoteSideFixedAmountSpec(amountSpec)
                .map(QuoteSideFixedAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findQuoteSideMinAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findQuoteSideRangeAmountSpec(amountSpec)
                .map(QuoteSideRangeAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findQuoteSideMaxAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findQuoteSideRangeAmountSpec(amountSpec)
                .map(QuoteSideRangeAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findQuoteSideMinOrFixedAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findQuoteSideMinAmountFromSpec(amountSpec, quoteCurrencyCode)
                .or(() -> findQuoteSideFixedAmountFromSpec(amountSpec, quoteCurrencyCode));
    }

    public static Optional<Monetary> findQuoteSideMaxOrFixedAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findQuoteSideMaxAmountFromSpec(amountSpec, quoteCurrencyCode)
                .or(() -> findQuoteSideFixedAmountFromSpec(amountSpec, quoteCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Find AmountSpec implementations
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<BaseSideFixedAmountSpec> findBaseSideFixedAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof BaseSideFixedAmountSpec ?
                Optional.of((BaseSideFixedAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<BaseSideRangeAmountSpec> findBaseSideRangeAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof BaseSideRangeAmountSpec ?
                Optional.of((BaseSideRangeAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<QuoteSideFixedAmountSpec> findQuoteSideFixedAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof QuoteSideFixedAmountSpec ?
                Optional.of((QuoteSideFixedAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<QuoteSideRangeAmountSpec> findQuoteSideRangeAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof QuoteSideRangeAmountSpec ?
                Optional.of((QuoteSideRangeAmountSpec) amountSpec) :
                Optional.empty();
    }
}