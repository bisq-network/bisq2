package bisq.offer.bisq_easy;


import bisq.account.protocol_type.SwapProtocolType;
import bisq.account.settlement.BitcoinSettlement;
import bisq.common.currency.Market;
import bisq.common.monetary.Fiat;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.offer.Direction;
import bisq.offer.SettlementSpec;
import bisq.offer.SwapOffer;
import bisq.offer.offer_options.AmountOption;
import bisq.offer.offer_options.OfferOption;
import bisq.offer.offer_options.ReputationOption;
import bisq.offer.offer_options.TradeTermsOption;
import bisq.offer.price_spec.FloatPriceSpec;
import bisq.offer.price_spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Getter
public final class BisqEasyOffer extends SwapOffer {
    private static List<SwapProtocolType> createSwapProtocolTypes() {
        return List.of(SwapProtocolType.BISQ_EASY);
    }

    private static List<SettlementSpec> createBaseSideSettlementSpecs() {
        return List.of(new SettlementSpec(BitcoinSettlement.Method.MAINCHAIN.name()));
    }

    private static List<SettlementSpec> createQuoteSideSettlementSpecs(List<String> paymentMethodNames) {
        return paymentMethodNames.stream()
                .map(SettlementSpec::new)
                .collect(Collectors.toList());
    }

    private static List<OfferOption> createOfferOptions(String makersTradeTerms, long requiredTotalReputationScore, double minAmountAsPercentage) {
        return List.of(
                new TradeTermsOption(makersTradeTerms),
                new ReputationOption(requiredTotalReputationScore),
                new AmountOption(minAmountAsPercentage)
        );
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
                new FloatPriceSpec(pricePremiumAsPercentage),
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
                          List<SwapProtocolType> swapProtocolTypes,
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
                swapProtocolTypes,
                baseSideSettlementSpecs,
                quoteSideSettlementSpecs,
                offerOptions);
        this.quoteSideAmount = quoteSideAmount;

        chatMessageText = Res.get("createOffer.bisqEasyOffer.chatMessage",
                Res.get(direction.name().toLowerCase()).toUpperCase(),
                AmountFormatter.formatAmountWithCode(Fiat.of(quoteSideAmount, market.getQuoteCurrencyCode()), true),
                Joiner.on(", ").join(this.getPaymentMethodNames()));
    }

    @Override
    public bisq.offer.protobuf.SwapOffer toProto() {
        return getSwapOfferBuilder().setBisqEasyOffer(
                        bisq.offer.protobuf.BisqEasyOffer.newBuilder()
                                .setQuoteSideAmount(quoteSideAmount))
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.offer.protobuf.SwapOffer proto) {
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

    public List<String> getPaymentMethodNames() {
        return quoteSideSettlementSpecs.stream().map(SettlementSpec::getSettlementMethodName)
                .map(methodName -> {
                    if (Res.has(methodName)) {
                        return Res.get(methodName);
                    } else {
                        return methodName;
                    }
                })
                .collect(Collectors.toList());
    }
}
