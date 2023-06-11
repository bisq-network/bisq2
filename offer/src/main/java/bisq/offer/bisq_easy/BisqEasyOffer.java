package bisq.offer.bisq_easy;


import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.network.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodUtil;
import bisq.offer.price.spec.PriceSpec;
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
public final class BisqEasyOffer extends Offer {
    private final String chatMessageText;

    public BisqEasyOffer(NetworkId makerNetworkId,
                         Direction direction,
                         Market market,
                         AmountSpec amountSpec,
                         PriceSpec priceSpec,
                         List<FiatPaymentMethod> fiatPaymentMethods,
                         String makersTradeTerms,
                         long requiredTotalReputationScore,
                         String chatMessageText) {
        this(StringUtils.createUid(),
                System.currentTimeMillis(),
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                List.of(TradeProtocolType.BISQ_EASY),
                PaymentMethodUtil.createBitcoinMainChainPaymentMethodSpec(),
                PaymentMethodUtil.createFiatPaymentMethodSpecs(fiatPaymentMethods),
                OfferOptionUtil.fromTradeTermsAndReputationScore(makersTradeTerms, requiredTotalReputationScore),
                chatMessageText
        );
    }

    private BisqEasyOffer(String id,
                          long date,
                          NetworkId makerNetworkId,
                          Direction direction,
                          Market market,
                          AmountSpec amountSpec,
                          PriceSpec priceSpec,
                          List<TradeProtocolType> protocolTypes,
                          List<PaymentMethodSpec> baseSidePaymentMethodSpecs,
                          List<PaymentMethodSpec> quoteSidePaymentMethodSpecs,
                          List<OfferOption> offerOptions,
                          String chatMessageText) {
        super(id,
                date,
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                protocolTypes,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpecs,
                offerOptions);
        this.chatMessageText = chatMessageText;
    }

    @Override
    public bisq.offer.protobuf.Offer toProto() {
        return getOfferBuilder().setBisqEasyOffer(
                        bisq.offer.protobuf.BisqEasyOffer.newBuilder()
                                .setChatMessageText(chatMessageText))
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<TradeProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(TradeProtocolType::fromProto)
                .collect(Collectors.toList());
        List<PaymentMethodSpec> baseSidePaymentMethodSpecs = proto.getBaseSidePaymentSpecsList().stream()
                .map(PaymentMethodSpec::fromProto)
                .collect(Collectors.toList());
        List<PaymentMethodSpec> quoteSidePaymentMethodSpecs = proto.getQuoteSidePaymentSpecsList().stream()
                .map(PaymentMethodSpec::fromProto)
                .collect(Collectors.toList());
        List<OfferOption> offerOptions = proto.getOfferOptionsList().stream()
                .map(OfferOption::fromProto)
                .collect(Collectors.toList());
        return new BisqEasyOffer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                AmountSpec.fromProto(proto.getAmountSpec()),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpecs,
                offerOptions,
                proto.getBisqEasyOffer().getChatMessageText());
    }
}
