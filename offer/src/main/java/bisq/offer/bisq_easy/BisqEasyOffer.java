package bisq.offer.bisq_easy;


import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.network.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment.PaymentSpec;
import bisq.offer.payment.PaymentUtil;
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
                         List<String> paymentMethodNames,
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
                List.of(ProtocolType.BISQ_EASY),
                PaymentUtil.createBaseSideSpecsForBitcoinMainChain(),
                PaymentUtil.createQuoteSideSpecsFromMethodNames(paymentMethodNames),
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
                          List<ProtocolType> protocolTypes,
                          List<PaymentSpec> baseSidePaymentSpecs,
                          List<PaymentSpec> quoteSidePaymentSpecs,
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
                baseSidePaymentSpecs,
                quoteSidePaymentSpecs,
                offerOptions);
        this.chatMessageText = chatMessageText;
    }

    @Override
    public bisq.offer.protobuf.Offer toProto() {
        return getSwapOfferBuilder().setBisqEasyOffer(
                        bisq.offer.protobuf.BisqEasyOffer.newBuilder()
                                .setChatMessageText(chatMessageText))
                .build();
    }

    public static BisqEasyOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<ProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(ProtocolType::fromProto)
                .collect(Collectors.toList());
        List<PaymentSpec> baseSidePaymentSpecs = proto.getBaseSidePaymentSpecsList().stream()
                .map(PaymentSpec::fromProto)
                .collect(Collectors.toList());
        List<PaymentSpec> quoteSidePaymentSpecs = proto.getQuoteSidePaymentSpecsList().stream()
                .map(PaymentSpec::fromProto)
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
                baseSidePaymentSpecs,
                quoteSidePaymentSpecs,
                offerOptions,
                proto.getBisqEasyOffer().getChatMessageText());
    }
}
