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

package bisq.offer.mu_sig;

import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.application.BuildVersion;
import bisq.common.market.Market;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.options.OfferOption;
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
public final class MuSigOffer extends Offer<PaymentMethodSpec<?>, PaymentMethodSpec<?>> {
    private static final int VERSION = 0;
    public static final double DEFAULT_BUYER_SECURITY_DEPOSIT = 0.25;
    public static final double DEFAULT_SELLER_SECURITY_DEPOSIT = 0.25;

    public MuSigOffer(String id,
                      NetworkId makerNetworkId,
                      Direction direction,
                      Market market,
                      AmountSpec amountSpec,
                      PriceSpec priceSpec,
                      List<PaymentMethod<?>> paymentMethods,
                      List<? extends OfferOption> offerOptions,
                      String tradeProtocolVersion
    ) {
        this(id,
                System.currentTimeMillis(),
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                List.of(TradeProtocolType.MU_SIG),
                market.isBtcFiatMarket()
                        ? createBitcoinMainChainPaymentMethodSpec()
                        : PaymentMethodSpecUtil.createPaymentMethodSpecs(paymentMethods, market.getBaseCurrencyCode()),
                market.isBtcFiatMarket()
                        ? PaymentMethodSpecUtil.createPaymentMethodSpecs(paymentMethods, market.getQuoteCurrencyCode())
                        : createBitcoinMainChainPaymentMethodSpec(),
                offerOptions,
                VERSION,
                tradeProtocolVersion,
                BuildVersion.VERSION
        );
    }

    private MuSigOffer(String id,
                       long date,
                       NetworkId makerNetworkId,
                       Direction direction,
                       Market market,
                       AmountSpec amountSpec,
                       PriceSpec priceSpec,
                       List<TradeProtocolType> protocolTypes,
                       List<PaymentMethodSpec<?>> baseSidePaymentMethodSpecs,
                       List<PaymentMethodSpec<?>> quoteSidePaymentMethodSpecs,
                       List<? extends OfferOption> offerOptions,
                       int version,
                       String tradeProtocolVersion,
                       String appVersion
    ) {
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
                offerOptions,
                version,
                tradeProtocolVersion,
                appVersion);

        verify();
    }

    private static List<PaymentMethodSpec<?>> createBitcoinMainChainPaymentMethodSpec() {
        return PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec().stream()
                        .map(spec -> (PaymentMethodSpec<?>) spec)
                        .collect(Collectors.toList());
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.offer.protobuf.Offer.Builder getBuilder(boolean serializeForHash) {
        return getOfferBuilder(serializeForHash).setMuSigOffer(
                bisq.offer.protobuf.MuSigOffer.newBuilder());
    }

    @Override
    public bisq.offer.protobuf.Offer toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigOffer fromProto(bisq.offer.protobuf.Offer proto) {
        List<TradeProtocolType> protocolTypes = proto.getProtocolTypesList().stream()
                .map(TradeProtocolType::fromProto)
                .collect(Collectors.toList());
        Market market = Market.fromProto(proto.getMarket());
        Class<? extends PaymentMethodSpec<?>> clazzForBaseSide = PaymentMethodSpecUtil.getPaymentMethodSpecClassForBaseSide(market);
        List<PaymentMethodSpec<?>> baseSidePaymentMethodSpecs = proto.getBaseSidePaymentSpecsList().stream()
                .map(pmProto -> PaymentMethodSpec.fromProto(pmProto, clazzForBaseSide))
                .collect(Collectors.toList());
        Class<? extends PaymentMethodSpec<?>> clazzForQuoteSide = PaymentMethodSpecUtil.getPaymentMethodSpecClassForQuoteSide(market);
        List<PaymentMethodSpec<?>> quoteSidePaymentMethodSpecs = proto.getQuoteSidePaymentSpecsList().stream()
                .map(pmProto -> PaymentMethodSpec.fromProto(pmProto, clazzForQuoteSide))
                .collect(Collectors.toList());
        List<OfferOption> offerOptions = proto.getOfferOptionsList().stream()
                .map(OfferOption::fromProto)
                .collect(Collectors.toList());
        return new MuSigOffer(proto.getId(),
                proto.getDate(),
                NetworkId.fromProto(proto.getMakerNetworkId()),
                Direction.fromProto(proto.getDirection()),
                market,
                AmountSpec.fromProto(proto.getAmountSpec()),
                PriceSpec.fromProto(proto.getPriceSpec()),
                protocolTypes,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpecs,
                offerOptions,
                proto.getVersion(),
                proto.getTradeProtocolVersion(),
                proto.getAppVersion());
    }
}
