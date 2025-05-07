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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.application.BuildVersion;
import bisq.common.currency.Market;
import bisq.common.util.StringUtils;
import bisq.network.identity.NetworkId;
import bisq.offer.Direction;
import bisq.offer.Offer;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.options.OfferOption;
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
public final class MuSigOffer extends Offer<BitcoinPaymentMethodSpec, FiatPaymentMethodSpec> {
    private static final int VERSION = 0;

    public MuSigOffer(NetworkId makerNetworkId,
                      Direction direction,
                      Market market,
                      AmountSpec amountSpec,
                      PriceSpec priceSpec,
                      List<FiatPaymentMethod> fiatPaymentMethods,
                      List<OfferOption> offerOptions,
                      String tradeProtocolVersion
    ) {
        this(StringUtils.createUid(),
                System.currentTimeMillis(),
                makerNetworkId,
                direction,
                market,
                amountSpec,
                priceSpec,
                List.of(TradeProtocolType.MU_SIG),
                PaymentMethodSpecUtil.createBitcoinMainChainPaymentMethodSpec(),
                PaymentMethodSpecUtil.createFiatPaymentMethodSpecs(fiatPaymentMethods),
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
                       List<BitcoinPaymentMethodSpec> baseSidePaymentMethodSpecs,
                       List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs,
                       List<OfferOption> offerOptions,
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
        return resolveProto(serializeForHash);
    }

    public static MuSigOffer fromProto(bisq.offer.protobuf.Offer proto) {
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
        return new MuSigOffer(proto.getId(),
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
                proto.getVersion(),
                proto.getTradeProtocolVersion(),
                proto.getAppVersion());
    }
}
