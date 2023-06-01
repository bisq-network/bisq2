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

import bisq.account.protocol_type.SwapProtocolType;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.price_spec.FixPriceSpec;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class SwapOffer implements Proto {
    protected final String id;
    protected final long date;
    protected final NetworkId makerNetworkId;
    protected final Direction direction;
    protected final Market market;
    protected final long baseSideAmount;
    protected final PriceSpec priceSpec;
    protected final List<SwapProtocolType> swapProtocolTypes;
    protected final List<SettlementSpec> baseSideSettlementSpecs;
    protected final List<SettlementSpec> quoteSideSettlementSpecs;
    protected final List<OfferOption> offerOptions;

    public SwapOffer(String id,
                     long date,
                     NetworkId makerNetworkId,
                     Direction direction,
                     Market market,
                     long baseSideAmount,
                     PriceSpec priceSpec,
                     List<SwapProtocolType> swapProtocolTypes,
                     List<SettlementSpec> baseSideSettlementSpecs,
                     List<SettlementSpec> quoteSideSettlementSpecs,
                     List<OfferOption> offerOptions) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.direction = direction;
        this.market = market;
        this.baseSideAmount = baseSideAmount;
        this.priceSpec = priceSpec;
        // We might get an immutable list, but we need to sort it, so wrap it into an ArrayList
        this.swapProtocolTypes = new ArrayList<>(swapProtocolTypes);
        this.baseSideSettlementSpecs = new ArrayList<>(baseSideSettlementSpecs);
        this.quoteSideSettlementSpecs = new ArrayList<>(quoteSideSettlementSpecs);
        this.offerOptions = new ArrayList<>(offerOptions);

        // All lists need to sort deterministically as the data is used in the proof of work check
        this.swapProtocolTypes.sort(Comparator.comparingInt(SwapProtocolType::hashCode));
        this.baseSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.quoteSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.offerOptions.sort(Comparator.comparingInt(OfferOption::hashCode));
    }

    public abstract bisq.offer.protobuf.SwapOffer toProto();

    protected bisq.offer.protobuf.SwapOffer.Builder getSwapOfferBuilder() {
        return bisq.offer.protobuf.SwapOffer.newBuilder()
                .setId(id)
                .setDate(date)
                .setMakerNetworkId(makerNetworkId.toProto())
                .setDirection(direction.toProto())
                .setMarket(market.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setPriceSpec(priceSpec.toProto())
                .addAllSwapProtocolTypes(swapProtocolTypes.stream().map(SwapProtocolType::toProto).collect(Collectors.toList()))
                .addAllBaseSideSettlementSpecs(baseSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllQuoteSideSettlementSpecs(quoteSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllOfferOptions(offerOptions.stream().map(OfferOption::toProto).collect(Collectors.toList()));
    }


    public static SwapOffer fromProto(bisq.offer.protobuf.SwapOffer proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYOFFER: {
                return BisqEasyOffer.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public Monetary getBaseAmountAsMonetary() {
        return Monetary.from(baseSideAmount, market.getBaseCurrencyCode());
    }

    public Monetary getQuoteAmountAsMonetary(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPriceSpec) {
            Monetary base = getBaseAmountAsMonetary();
            Quote quote = Quote.fromPrice(((FixPriceSpec) priceSpec).getValue(), market);
            long quoteAmountValue = Quote.toQuoteMonetary(base, quote).getValue();
            return Monetary.from(quoteAmountValue, market.getQuoteCurrencyCode());
        } else if (priceSpec instanceof FloatPriceSpec) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public Quote getQuote(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPriceSpec) {
            return Quote.fromPrice(((FixPriceSpec) priceSpec).getValue(), market);
        } else if (priceSpec instanceof FloatPriceSpec) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }
}
