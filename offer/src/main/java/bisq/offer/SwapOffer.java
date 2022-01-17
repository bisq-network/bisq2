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
import bisq.contract.SwapProtocolType;
import bisq.network.NetworkId;
import bisq.offer.options.AmountOption;
import bisq.offer.options.OfferOption;
import bisq.offer.options.PriceOption;
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
    private final Leg askLeg;
    private final Leg bidLeg;
    private final boolean useAskLegForBaseCurrency;
    private transient final Quote quote;

    public SwapOffer(List<SwapProtocolType> protocolTypes,
                     NetworkId makerNetworkId,
                     Leg askLeg,
                     Leg bidLeg,
                     String baseCurrencyCode) {
        this(askLeg, bidLeg, baseCurrencyCode, protocolTypes, makerNetworkId, new HashSet<>());
    }

    /**
     * @param askLeg           The ask leg (what the maker asks for)
     * @param bidLeg           The bid leg (what the maker offers)
     * @param baseCurrencyCode If the base currency code for the price quote.
     * @param protocolTypes    The list of the supported swap protocol types. Order in the list can be used as priority.
     * @param makerNetworkId   The networkId the maker used for that listing. It encapsulate the network addresses
     *                         of the supported networks and the pubKey used for data protection in the storage layer.
     * @param offerOptions     Options for different aspects of an offer like min amount, market based price, fee options... Can be specific to protocol type.
     */
    public SwapOffer(Leg askLeg,
                     Leg bidLeg,
                     String baseCurrencyCode,
                     List<SwapProtocolType> protocolTypes,
                     NetworkId makerNetworkId,
                     Set<OfferOption> offerOptions) {
        super(protocolTypes, makerNetworkId, offerOptions);

        this.askLeg = askLeg;
        this.bidLeg = bidLeg;
        this.useAskLegForBaseCurrency = baseCurrencyCode.equals(askLeg.code());

        quote = Quote.of(getBaseLeg().monetary(), getQuoteLeg().monetary());
       
        
    }

    public Leg getBaseLeg() {
        return useAskLegForBaseCurrency ? askLeg : bidLeg;
    }

    public Leg getQuoteLeg() {
        return useAskLegForBaseCurrency ? bidLeg : askLeg;
    }

    public String getBaseCode() {
        return getBaseLeg().code();
    }

    public String getQuoteCode() {
        return getQuoteLeg().code();
    }

    public String getAskCode() {
        return askLeg.code();
    }

    public String getBidCode() {
        return bidLeg.code();
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
        return findAmountOption(offerOptions).map(AmountOption::minAmountAsPercentage);
    }

    private Optional<AmountOption> findAmountOption(Set<OfferOption> offerOptions) {
        return offerOptions.stream().filter(e -> e instanceof AmountOption).map(e -> (AmountOption) e).findAny();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Price
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public Optional<Double> findMarketPriceOffset() {
        return findPriceOption(offerOptions).map(PriceOption::marketPriceOffset);
    }

    private Optional<PriceOption> findPriceOption(Set<OfferOption> offerOptions) {
        return offerOptions.stream().filter(e -> e instanceof PriceOption).map(e -> (PriceOption) e).findAny();
    }

}
