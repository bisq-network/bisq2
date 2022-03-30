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

import bisq.account.protocol.SwapProtocolType;
import bisq.common.monetary.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.offer.options.OfferOption;
import bisq.offer.spec.*;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class Offer implements DistributedData {
    public static final String ACCOUNT_AGE_WITNESS_HASH = "accountAgeWitnessHash";
    public static final String REFERRAL_ID = "referralId";
    // Only used in payment method F2F
    public static final String F2F_CITY = "f2fCity";
    public static final String F2F_EXTRA_INFO = "f2fExtraInfo";
    public static final String CASH_BY_MAIL_EXTRA_INFO = "cashByMailExtraInfo";

    // Comma separated list of ordinal of a bisq.common.app.Capability. E.g. ordinal of
    // Capability.SIGNED_ACCOUNT_AGE_WITNESS is 11 and Capability.MEDIATION is 12 so if we want to signal that maker
    // of the offer supports both capabilities we add "11, 12" to capabilities.
    public static final String CAPABILITIES = "capabilities";
    // If maker is seller and has xmrAutoConf enabled it is set to "1" otherwise it is not set
    public static final String XMR_AUTO_CONF = "xmrAutoConf";
    public static final String XMR_AUTO_CONF_ENABLED_VALUE = "1";

    private final String id;
    private final long date;
    private final NetworkId makerNetworkId;
    private final Market market;
    private final Direction direction;
    private final long baseAmount;
    private final PriceSpec priceSpec;
    private final List<SwapProtocolType> swapProtocolTypes;
    private final List<SettlementSpec> baseSideSettlementSpecs;
    private final List<SettlementSpec> quoteSideSettlementSpecs;
    private final List<OfferOption> offerOptions;
    private final MetaData metaData;

    public Offer(String id,
                 long date,
                 NetworkId makerNetworkId,
                 Market market,
                 Direction direction,
                 long baseAmount,
                 PriceSpec priceSpec,
                 List<SwapProtocolType> swapProtocolTypes,
                 List<SettlementSpec> baseSideSettlementSpecs,
                 List<SettlementSpec> quoteSideSettlementSpecs,
                 List<OfferOption> offerOptions) {
        this(id,
                date,
                makerNetworkId,
                market,
                direction,
                baseAmount,
                priceSpec,
                swapProtocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions,
                new MetaData(TimeUnit.MINUTES.toMillis(5), 100000, Offer.class.getSimpleName()));
    }

    private Offer(String id,
                  long date,
                  NetworkId makerNetworkId,
                  Market market,
                  Direction direction,
                  long baseAmount,
                  PriceSpec priceSpec,
                  List<SwapProtocolType> swapProtocolTypes,
                  List<SettlementSpec> baseSideSettlementSpecs,
                  List<SettlementSpec> quoteSideSettlementSpecs,
                  List<OfferOption> offerOptions,
                  MetaData metaData) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.market = market;
        this.direction = direction;
        this.baseAmount = baseAmount;
        this.priceSpec = priceSpec;
        this.swapProtocolTypes = swapProtocolTypes;
        this.baseSideSettlementSpecs = baseSideSettlementSpecs;
        this.quoteSideSettlementSpecs = quoteSideSettlementSpecs;
        this.offerOptions = offerOptions;
        this.metaData = metaData;
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    public bisq.offer.protobuf.Offer toProto() {
        return bisq.offer.protobuf.Offer.newBuilder()
                .setId(id)
                .setDate(date)
                .setMakerNetworkId(makerNetworkId.toProto())
                .setMarket(market.toProto())
                .setDirection(direction.toProto())
                .setBaseAmount(baseAmount)
                .setPriceSpec(priceSpec.toProto())
                .addAllSwapProtocolTypes(swapProtocolTypes.stream().map(Enum::name).collect(Collectors.toList()))
                .addAllBaseSideSettlementSpecs(baseSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllQuoteSideSettlementSpecs(quoteSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllOfferOptions(offerOptions.stream().map(OfferOption::toProto).collect(Collectors.toList()))
                .setMetaData(metaData.toProto())
                .build();
    }

    public static Offer fromProto(bisq.offer.protobuf.Offer proto) {
        List<SwapProtocolType> protocolTypes = proto.getSwapProtocolTypesList().stream()
                .map(SwapProtocolType::fromProto)
                .collect(Collectors.toList());
        List<SettlementSpec> baseSideSettlementSpecs = proto.getBaseSideSettlementSpecsList().stream()
                .map(SettlementSpec::fromProto)
                .collect(Collectors.toList());
        List<SettlementSpec> quoteSideSettlementSpecs = proto.getQuoteSideSettlementSpecsList().stream()
                .map(SettlementSpec::fromProto)
                .collect(Collectors.toList());
        List<OfferOption> offerOptions = proto.getOfferOptionsList().stream()
                .map(OfferOption::fromProto)
                .collect(Collectors.toList());
        return new Offer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Market.fromProto(proto.getMarket()),
                Direction.fromProto(proto.getDirection()),
                proto.getBaseAmount(),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions,
                MetaData.fromProto(proto.getMetaData()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.Offer.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public Optional<SwapProtocolType> findProtocolType() {
        if (swapProtocolTypes.isEmpty()) {
            return Optional.empty();
        } else if (swapProtocolTypes.size() == 1) {
            return Optional.of(swapProtocolTypes.get(0));
        } else {
            throw new IllegalStateException("Multiple protocolTypes are not supported yet. protocolTypes=" + swapProtocolTypes);
        }
    }

    public Monetary getBaseAmountAsMonetary() {
        return Monetary.from(baseAmount, market.baseCurrencyCode());
    }

    public Monetary getQuoteAmountAsMonetary(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPrice fixPriceSpec) {
            Monetary base = getBaseAmountAsMonetary();
            Quote quote = Quote.fromPrice(fixPriceSpec.value(), market);
            long quoteAmountValue = Quote.toQuoteMonetary(base, quote).getValue();
            return Monetary.from(quoteAmountValue, market.quoteCurrencyCode());
        } else if (priceSpec instanceof FloatPrice floatPrice) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }

    public Quote getQuote(MarketPriceService marketPriceService) {
        if (priceSpec instanceof FixPrice fixPriceSpec) {
            return Quote.fromPrice(fixPriceSpec.value(), market);
        } else if (priceSpec instanceof FloatPrice floatPrice) {
            Optional<MarketPrice> marketPrice = marketPriceService.getMarketPrice(market);
            //todo
            throw new RuntimeException("floatPrice not impl yet");
        } else {
            throw new IllegalStateException("Not supported priceSpec. priceSpec=" + priceSpec);
        }
    }
}
