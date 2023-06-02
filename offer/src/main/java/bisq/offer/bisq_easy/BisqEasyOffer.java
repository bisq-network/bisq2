package bisq.offer.bisq_easy;


import bisq.account.protocol_type.ProtocolType;
import bisq.account.settlement.BitcoinSettlement;
import bisq.common.currency.Market;
import bisq.common.monetary.Fiat;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.SettlementSpec;
import bisq.offer.offer_options.AmountOption;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.offer_options.ReputationOption;
import bisq.offer.offer_options.TradeTermsOption;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.MarketPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@ToString
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Getter
public final class BisqEasyOffer extends Offer {
    private static List<ProtocolType> createSwapProtocolTypes() {
        return List.of(ProtocolType.BISQ_EASY);
    }

    private static PriceSpec createPriceSpec(double percentage) {
        return percentage >= 0 ?
                new FloatPriceSpec(percentage) :
                new MarketPriceSpec();
    }

    private static List<SettlementSpec> createBaseSideSettlementSpecs() {
        return List.of(new SettlementSpec(BitcoinSettlement.Method.MAINCHAIN.name()));
    }

    private static List<SettlementSpec> createQuoteSideSettlementSpecs(List<String> paymentMethodNames) {
        checkArgument(!paymentMethodNames.isEmpty());
        return paymentMethodNames.stream()
                .map(SettlementSpec::new)
                .collect(Collectors.toList());
    }

    private static List<OfferOption> createOfferOptions(String makersTradeTerms, long requiredTotalReputationScore, double minAmountAsPercentage) {
        List<OfferOption> offerOptions = new ArrayList<>();
        if (makersTradeTerms != null && !makersTradeTerms.isEmpty()) {
            offerOptions.add(new TradeTermsOption(makersTradeTerms));
        }
        if (requiredTotalReputationScore > 0) {
            offerOptions.add(new ReputationOption(requiredTotalReputationScore));
        }
        if (minAmountAsPercentage < 1 && minAmountAsPercentage > 0) {
            offerOptions.add(new AmountOption(minAmountAsPercentage));
        }
        return offerOptions;
    }


    private final long quoteSideAmount;

    private transient final String chatMessageText;

    public BisqEasyOffer(String id,
                         long date,
                         NetworkId makerNetworkId,
                         Direction direction,
                         Market market,
                         long baseSideAmount,
                         long quoteSideAmount,
                         List<String> paymentMethodNames,
                         String makersTradeTerms,
                         long requiredTotalReputationScore,
                         double minAmountAsPercentage,
                         double pricePremiumAsPercentage) {
        this(id,
                date,
                makerNetworkId,
                direction,
                market,
                baseSideAmount,
                createPriceSpec(pricePremiumAsPercentage),
                createSwapProtocolTypes(),
                createBaseSideSettlementSpecs(),
                createQuoteSideSettlementSpecs(paymentMethodNames),
                createOfferOptions(makersTradeTerms, requiredTotalReputationScore, minAmountAsPercentage),
                quoteSideAmount);
    }


    private BisqEasyOffer(String id,
                          long date,
                          NetworkId makerNetworkId,
                          Direction direction,
                          Market market,
                          long baseSideAmount,
                          PriceSpec priceSpec,
                          List<ProtocolType> protocolTypes,
                          List<SettlementSpec> baseSideSettlementSpecs,
                          List<SettlementSpec> quoteSideSettlementSpecs,
                          List<OfferOption> offerOptions,
                          long quoteSideAmount) {
        super(id,
                date,
                makerNetworkId,
                direction,
                market,
                baseSideAmount,
                priceSpec,
                protocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions);
        this.quoteSideAmount = quoteSideAmount;

        chatMessageText = Res.get("createOffer.bisqEasyOffer.chatMessage",
                getDirectionAsDisplayString(),
                getQuoteSideAmountAsDisplayString(),
                getSettlementMethodsAsDisplayString());
    }

    @Override
    public bisq.offer.protobuf.Offer toProto() {
        return getSwapOfferBuilder().setBisqEasyOffer(
                        bisq.offer.protobuf.BisqEasyOffer.newBuilder()
                                .setQuoteSideAmount(quoteSideAmount))
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<ProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(ProtocolType::fromProto)
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
        return new BisqEasyOffer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                proto.getBaseSideAmount(),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions,
                proto.getBisqEasyOffer().getQuoteSideAmount());
    }

    public List<String> getSettlementMethodNames() {
        return SettlementSpec.getSettlementMethodNames(quoteSideSettlementSpecs);
    }

    public String getDirectionAsDisplayString() {
        return Res.get(direction.name().toLowerCase()).toUpperCase();
    }

    public String getMirroredDirectionAsDisplayString() {
        return Res.get(direction.mirror().name().toLowerCase()).toUpperCase();
    }

    public String getSettlementMethodsAsDisplayString() {
        return Joiner.on(", ").join(SettlementSpec.getSettlementMethodNamesAsDisplayString(quoteSideSettlementSpecs));
    }

    public String getBaseSideAmountAsDisplayString() {
        return AmountFormatter.formatAmountWithCode(Fiat.of(baseSideAmount, market.getBaseCurrencyCode()), true);
    }

    public String getQuoteSideAmountAsDisplayString() {
        return AmountFormatter.formatAmountWithCode(Fiat.of(quoteSideAmount, market.getQuoteCurrencyCode()), true);
    }

    public Optional<String> getPricePremiumAsPercentage() {
        return getFloatPriceAsPercentage()
                .map(PercentageFormatter::formatToPercentWithSymbol);
    }

    public Optional<Double> getFloatPriceAsPercentage() {
        return PriceSpec.findFloatPriceSpec(priceSpec).map(FloatPriceSpec::getPercentage);
    }

    public Optional<String> getMakersTradeTerms() {
        return OfferOption.findTradeTermsOption(offerOptions).stream().findAny()
                .map(TradeTermsOption::getMakersTradeTerms);
    }

    public Optional<Long> getRequiredTotalReputationScore() {
        return OfferOption.findReputationOption(offerOptions).stream().findAny()
                .map(ReputationOption::getRequiredTotalReputationScore);
    }

    public Optional<Double> getMinAmountAsPercentage() {
        return OfferOption.findAmountOption(offerOptions).stream().findAny()
                .map(AmountOption::getMinAmountAsPercentage);
    }
}
