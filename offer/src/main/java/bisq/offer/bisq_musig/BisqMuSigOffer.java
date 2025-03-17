package bisq.offer.bisq_musig;


import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.options.OfferOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.spec.PriceSpec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Getter
public final class BisqMuSigOffer extends Offer<BitcoinPaymentMethodSpec, FiatPaymentMethodSpec> {
    public BisqMuSigOffer(NetworkId makerNetworkId,
                          Direction direction,
                          Market market,
                          AmountSpec amountSpec,
                          PriceSpec priceSpec,
                          List<FiatPaymentMethod> fiatPaymentMethods,
                          String makersTradeTerms) {
        this(StringUtils.createUid(),
                System.currentTimeMillis(),
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                List.of(TradeProtocolType.BISQ_MU_SIG),
                PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec(),
                PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(fiatPaymentMethods),
                OfferOptionUtil.fromTradeTerms(makersTradeTerms)
        );
    }

    private BisqMuSigOffer(String id,
                           long date,
                           NetworkId makerNetworkId,
                           Direction direction,
                           Market market,
                           AmountSpec amountSpec,
                           PriceSpec priceSpec,
                           List<TradeProtocolType> protocolTypes,
                           List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs,
                           List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs,
                           List<OfferOption> offerOptions) {
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

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.Offer.Builder getBuilder(boolean serializeForHash) {
        return getOfferBuilder(serializeForHash).setBisqMuSigOffer(
                bisq.offer.protobuf.BisqMuSigOffer.newBuilder());
    }

    @Override
    public bisq.offer.protobuf.Offer toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BisqMuSigOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<TradeProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(TradeProtocolType::fromProto)
                .collect(Collectors.toList());
        List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs = proto.getBaseSidePaymentSpecsList().stream()
                .map(PaymentMethodSpec::protoToBitcoinPaymentMethodSpec)
                .collect(Collectors.toList());
        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = proto.getQuoteSidePaymentSpecsList().stream()
                .map(PaymentMethodSpec::protoToFiatPaymentMethodSpec)
                .collect(Collectors.toList());
        List<OfferOption> offerOptions = proto.getOfferOptionsList().stream()
                .map(OfferOption::fromProto)
                .collect(Collectors.toList());
        return new BisqMuSigOffer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Direction.fromProto(proto.getDirection()),
                Market.fromProto(proto.getMarket()),
                AmountSpec.fromProto(proto.getAmountSpec()),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpecs,
                offerOptions);
    }

}
