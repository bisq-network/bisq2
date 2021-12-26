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

package network.misq.offer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.monetary.Quote;
import network.misq.common.util.MathUtils;
import network.misq.contract.SwapProtocolType;
import network.misq.network.p2p.NetworkId;
import network.misq.offer.options.AmountOption;
import network.misq.offer.options.OfferOption;
import network.misq.offer.options.PriceOption;

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
public class Offer extends Listing {
    private final Asset askAsset;
    private final Asset bidAsset;
    private final boolean isBaseCurrencyAskSide;
    private final Set<OfferOption> offerOptions;

    private transient final Quote quote;
    private transient final Optional<Long> optionalMinBaseAmount;
    private transient final long minBaseAmountOrAmount; //todo remove

    public Offer(List<SwapProtocolType> protocolTypes,
                 NetworkId makerNetworkId,
                 Asset askAsset,
                 Asset bidAsset) {
        this(askAsset, bidAsset, true, protocolTypes, makerNetworkId, new HashSet<>());
    }

    /**
     * @param askAsset              The asset on the ask side (what the maker asks for)
     * @param bidAsset              The asset on the bid side (what the maker offers)
     * @param isBaseCurrencyAskSide If the base currency for the price quote is the currency of the ask asset.
     * @param protocolTypes         The list of the supported swap protocol types. Order in the list can be used as priority.
     * @param makerNetworkId        The networkId the maker used for that listing. It encapsulate the network addresses
     *                              of the supported networks and the pubKey used for data protection in the storage layer.
     * @param offerOptions          Options for different aspects of an offer like min amount, market based price, fee options... Can be specific to protocol type.
     */
    public Offer(Asset askAsset,
                 Asset bidAsset,
                 boolean isBaseCurrencyAskSide,
                 List<SwapProtocolType> protocolTypes,
                 NetworkId makerNetworkId,
                 Set<OfferOption> offerOptions) {
        super(protocolTypes, makerNetworkId, offerOptions);

        this.askAsset = askAsset;
        this.bidAsset = bidAsset;
        this.isBaseCurrencyAskSide = isBaseCurrencyAskSide;
        this.offerOptions = offerOptions;

        quote = Quote.of(getBaseAsset().monetary(), getQuoteAsset().monetary());
        optionalMinBaseAmount = getOptionalMinAmount(getBaseAsset().amount());
        minBaseAmountOrAmount = optionalMinBaseAmount.orElse(getBaseAsset().amount());
    }

    public Asset getBaseAsset() {
        return isBaseCurrencyAskSide ? askAsset : bidAsset;
    }

    public Asset getQuoteAsset() {
        return isBaseCurrencyAskSide ? bidAsset : askAsset;
    }

    public String getBaseCurrencyCode() {
        return getBaseAsset().currencyCode();
    }

    public String getQuoteCurrencyCode() {
        return getQuoteAsset().currencyCode();
    }

    public String getAskCurrencyCode() {
        return askAsset.currencyCode();
    }

    public String getBidCurrencyCode() {
        return bidAsset.currencyCode();
    }

    public Optional<Double> getMinAmountAsPercentage() {
        return findAmountOption(offerOptions).map(AmountOption::minAmountAsPercentage);
    }

    public Optional<Long> getOptionalMinQuoteAmount(long quoteAmount) {
        return getOptionalMinAmount(quoteAmount);
    }

    public Optional<Double> getMarketPriceOffset() {
        return findPriceOption(offerOptions).map(PriceOption::marketPriceOffset);
    }


    private Optional<Long> getOptionalMinAmount(long amount) {
        return getMinAmountAsPercentage()
                .map(percentage -> MathUtils.roundDoubleToLong(amount * percentage));
    }

    private Optional<AmountOption> findAmountOption(Set<OfferOption> offerOptions) {
        return offerOptions.stream().filter(e -> e instanceof AmountOption).map(e -> (AmountOption) e).findAny();
    }

    private Optional<PriceOption> findPriceOption(Set<OfferOption> offerOptions) {
        return offerOptions.stream().filter(e -> e instanceof PriceOption).map(e -> (PriceOption) e).findAny();
    }
}
