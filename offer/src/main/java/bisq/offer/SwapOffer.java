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

package bisq.offer;

import bisq.common.monetary.Quote;
import bisq.common.util.MathUtils;
import bisq.network.NetworkId;
import bisq.offer.options.AmountOption;
import bisq.offer.options.ListingOption;
import bisq.offer.options.PriceOption;
import bisq.offer.protocol.SwapProtocolType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Offer for an 2 party asset exchange (swap).
 */
@Slf4j
@Getter
@EqualsAndHashCode(callSuper = true)
public class SwapOffer extends Listing {
    private final SwapSide askSwapSide;
    private final SwapSide bidSwapSide;
    private final boolean useAskLegForBaseCurrency;
    private transient final Quote quote;

    public SwapOffer(SwapSide askSwapSide,
                     SwapSide bidSwapSide,
                     String baseCurrencyCode,
                     SwapProtocolType protocolType,
                     NetworkId makerNetworkId) {
        this(askSwapSide, bidSwapSide, baseCurrencyCode, makerNetworkId, List.of(protocolType), new HashSet<>());
    }

    public SwapOffer(SwapSide askSwapSide,
                     SwapSide bidSwapSide,
                     String baseCurrencyCode,
                     List<SwapProtocolType> protocolTypes,
                     NetworkId makerNetworkId) {
        this(askSwapSide, bidSwapSide, baseCurrencyCode, makerNetworkId, protocolTypes, new HashSet<>());
    }

    /**
     * @param askSwapSide           The ask leg (what the maker asks for)
     * @param bidSwapSide           The bid leg (what the maker offers)
     * @param baseCurrencyCode If the base currency code for the price quote.
     * @param makerNetworkId   The networkId the maker used for that listing. It encapsulate the network addresses
     *                         of the supported networks and the pubKey used for data protection in the storage layer.
     * @param protocolTypes    The list of the supported swap protocol types. Order in the list can be used as priority.
     * @param listingOptions     Options for different aspects of an offer like min amount, market based price, fee options... Can be specific to protocol type.
     */
    public SwapOffer(SwapSide askSwapSide,
                     SwapSide bidSwapSide,
                     String baseCurrencyCode,
                     NetworkId makerNetworkId,
                     List<SwapProtocolType> protocolTypes,
                     Set<ListingOption> listingOptions) {
        super(makerNetworkId, protocolTypes, listingOptions);

        this.askSwapSide = askSwapSide;
        this.bidSwapSide = bidSwapSide;
        this.useAskLegForBaseCurrency = baseCurrencyCode.equals(askSwapSide.code());

        quote = Quote.of(getBaseLeg().monetary(), getQuoteLeg().monetary());
    }

    public SwapSide getBaseLeg() {
        return useAskLegForBaseCurrency ? askSwapSide : bidSwapSide;
    }

    public SwapSide getQuoteLeg() {
        return useAskLegForBaseCurrency ? bidSwapSide : askSwapSide;
    }

    public String getBaseCode() {
        return getBaseLeg().code();
    }

    public String getQuoteCode() {
        return getQuoteLeg().code();
    }

    public String getAskCode() {
        return askSwapSide.code();
    }

    public String getBidCode() {
        return bidSwapSide.code();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Amounts
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Long> findMinQuoteAmount(long quoteAmount) {
        return findMinAmount(quoteAmount);
    }

    public long getMinBaseAmountOrAmount() {
        return findMinBaseAmount().orElse(getBaseLeg().amount());
    }

    public Optional<Long> findMinBaseAmount() {
        return findMinAmount(getBaseLeg().amount());
    }

    private Optional<Long> findMinAmount(long amount) {
        return findMinAmountAsPercentage()
                .map(percentage -> MathUtils.roundDoubleToLong(amount * percentage));
    }

    private Optional<Double> findMinAmountAsPercentage() {
        return findAmountOption(listingOptions).map(AmountOption::minAmountAsPercentage);
    }

    private Optional<AmountOption> findAmountOption(Set<ListingOption> listingOptions) {
        return listingOptions.stream().filter(e -> e instanceof AmountOption).map(e -> (AmountOption) e).findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Price
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Double> findMarketPriceOffset() {
        return findPriceOption(listingOptions).map(PriceOption::marketPriceOffset);
    }

    private Optional<PriceOption> findPriceOption(Set<ListingOption> listingOptions) {
        return listingOptions.stream().filter(e -> e instanceof PriceOption).map(e -> (PriceOption) e).findAny();
    }

}
