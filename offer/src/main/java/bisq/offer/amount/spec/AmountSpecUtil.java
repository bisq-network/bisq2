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
    // BaseAmount: Monetary from BaseAmountSpec otherwise empty Optional
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixBaseAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findFixBaseAmountSpec(amountSpec)
                .map(FixBaseAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findMinBaseAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findMinMaxBaseAmountSpec(amountSpec)
                .map(MinMaxBaseAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findMaxBaseAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findMinMaxBaseAmountSpec(amountSpec)
                .map(MinMaxBaseAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, baseCurrencyCode));
    }

    public static Optional<Monetary> findFixOrMaxBaseAmountFromSpec(AmountSpec amountSpec, String baseCurrencyCode) {
        return findFixBaseAmountFromSpec(amountSpec, baseCurrencyCode)
                .or(() -> findMaxBaseAmountFromSpec(amountSpec, baseCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // QuoteAmount: Monetary from QuoteAmountSpec otherwise empty Optional
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<Monetary> findFixQuoteAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findFixQuoteAmountSpec(amountSpec)
                .map(FixQuoteAmountSpec::getAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findMinQuoteAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findMinMaxQuoteAmountSpec(amountSpec)
                .map(MinMaxQuoteAmountSpec::getMinAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findMaxQuoteAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findMinMaxQuoteAmountSpec(amountSpec)
                .map(MinMaxQuoteAmountSpec::getMaxAmount)
                .map(amount -> Monetary.from(amount, quoteCurrencyCode));
    }

    public static Optional<Monetary> findFixOrMaxQuoteAmountFromSpec(AmountSpec amountSpec, String quoteCurrencyCode) {
        return findFixQuoteAmountFromSpec(amountSpec, quoteCurrencyCode)
                .or(() -> findMaxQuoteAmountFromSpec(amountSpec, quoteCurrencyCode));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Find AmountSpec implementations
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static Optional<FixBaseAmountSpec> findFixBaseAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof FixBaseAmountSpec ?
                Optional.of((FixBaseAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<MinMaxBaseAmountSpec> findMinMaxBaseAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof MinMaxBaseAmountSpec ?
                Optional.of((MinMaxBaseAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<FixQuoteAmountSpec> findFixQuoteAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof FixQuoteAmountSpec ?
                Optional.of((FixQuoteAmountSpec) amountSpec) :
                Optional.empty();
    }

    public static Optional<MinMaxQuoteAmountSpec> findMinMaxQuoteAmountSpec(AmountSpec amountSpec) {
        return amountSpec instanceof MinMaxQuoteAmountSpec ?
                Optional.of((MinMaxQuoteAmountSpec) amountSpec) :
                Optional.empty();
    }
}