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

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.MathUtils;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.price_spec.FixPriceSpec;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.MarketPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class Offer implements Proto {
    protected final String id;
    protected final long date;
    protected final NetworkId makerNetworkId;
    protected final Direction direction;
    protected final Market market;
    protected final long baseSideAmount;
    protected final PriceSpec priceSpec;
    protected final List<ProtocolType> protocolTypes;
    protected final List<SettlementSpec> baseSideSettlementSpecs;
    protected final List<SettlementSpec> quoteSideSettlementSpecs;
    protected final List<OfferOption> offerOptions;

    public Offer(String id,
                 long date,
                 NetworkId makerNetworkId,
                 Direction direction,
                 Market market,
                 long baseSideAmount,
                 PriceSpec priceSpec,
                 List<ProtocolType> protocolTypes,
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
        this.protocolTypes = new ArrayList<>(protocolTypes);
        this.baseSideSettlementSpecs = new ArrayList<>(baseSideSettlementSpecs);
        this.quoteSideSettlementSpecs = new ArrayList<>(quoteSideSettlementSpecs);
        this.offerOptions = new ArrayList<>(offerOptions);

        // All lists need to sort deterministically as the data is used in the proof of work check
        this.protocolTypes.sort(Comparator.comparingInt(ProtocolType::hashCode));
        this.baseSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.quoteSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.offerOptions.sort(Comparator.comparingInt(OfferOption::hashCode));
    }

    public abstract bisq.offer.protobuf.Offer toProto();

    protected bisq.offer.protobuf.Offer.Builder getSwapOfferBuilder() {
        return bisq.offer.protobuf.Offer.newBuilder()
                .setId(id)
                .setDate(date)
                .setMakerNetworkId(makerNetworkId.toProto())
                .setDirection(direction.toProto())
                .setMarket(market.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setPriceSpec(priceSpec.toProto())
                .addAllProtocolTypes(protocolTypes.stream().map(ProtocolType::toProto).collect(Collectors.toList()))
                .addAllBaseSideSettlementSpecs(baseSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllQuoteSideSettlementSpecs(quoteSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllOfferOptions(offerOptions.stream().map(OfferOption::toProto).collect(Collectors.toList()));
    }


    public static Offer fromProto(bisq.offer.protobuf.Offer proto) {
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

    public Optional<Monetary> getQuoteAmountAsMonetary(MarketPriceService marketPriceService) {
        return getQuote(marketPriceService)
                .map(quote -> {
                    long quoteAmountValue = Quote.toQuoteMonetary(getBaseAmountAsMonetary(), quote).getValue();
                    return Monetary.from(quoteAmountValue, market.getQuoteCurrencyCode());
                });
    }

    public Optional<Quote> getQuote(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPriceSpec) {
            return Optional.of(getFixePriceQuote((FixPriceSpec) priceSpec));
        } else if (priceSpec instanceof MarketPriceSpec) {
            return findMarketPriceQuote(marketPriceService);
        } else if (priceSpec instanceof FloatPriceSpec) {
            return findFloatPriceQuote(marketPriceService, (FloatPriceSpec) priceSpec);
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public Optional<Double> findPercentFromMarketPrice(MarketPriceService marketPriceService) {
        Optional<Double> percentage;
        if (priceSpec instanceof FixPriceSpec) {
            Quote fixPrice = getFixePriceQuote((FixPriceSpec) priceSpec);
            percentage = findMarketPriceQuote(marketPriceService).map(marketPrice ->
                    1 - (double) fixPrice.getValue() / (double) marketPrice.getValue());
        } else if (priceSpec instanceof MarketPriceSpec) {
            percentage = Optional.of(0d);
        } else if (priceSpec instanceof FloatPriceSpec) {
            percentage = Optional.of(((FloatPriceSpec) priceSpec).getPercentage());
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
        return percentage;
    }

    public Quote getFixePriceQuote(FixPriceSpec fixPriceSpec) {
        return Quote.fromPrice(fixPriceSpec.getValue(), market);
    }

    public Optional<Quote> findFloatPriceQuote(MarketPriceService marketPriceService, FloatPriceSpec floatPriceSpec) {
        return findMarketPriceQuote(marketPriceService)
                .map(marketQuote ->
                        Quote.fromPrice(MathUtils.roundDoubleToLong((1 + floatPriceSpec.getPercentage()) * marketQuote.getValue()), market));
    }

    public Optional<Quote> findMarketPriceQuote(MarketPriceService marketPriceService) {
        return marketPriceService.getMarketPrice(market).map(MarketPrice::getQuote).stream().findAny();
    }

    public Direction getMakersDirection() {
        return direction;
    }

    public Direction getTakersDirection() {
        return direction.mirror();
    }

    public String getAuthorUserProfileId() {
        return makerNetworkId.getPubKey().getId();
    }

    public boolean isMyOffer(String myUserProfileId) {
        return myUserProfileId.equals(getAuthorUserProfileId());
    }

    public boolean isMyOffer(Set<String> myUserProfileIds) {
        return myUserProfileIds.contains(getAuthorUserProfileId());
    }

    public String getShortId() {
        return id.substring(0, 8);
    }
}
