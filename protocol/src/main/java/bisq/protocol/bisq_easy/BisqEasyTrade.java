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

package bisq.protocol.bisq_easy;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.util.ProtobufUtils;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.protocol.Trade;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BisqEasyTrade extends Trade<BitcoinPaymentMethodSpec, FiatPaymentMethodSpec> {
    public BisqEasyTrade(Offer<BitcoinPaymentMethodSpec, FiatPaymentMethodSpec> offer,
                         NetworkId takerNetworkId,
                         long baseSideAmount,
                         long quoteSideAmount,
                         TradeProtocolType protocolType,
                         BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs,
                         FiatPaymentMethodSpec quoteSidePaymentMethodSpec) {
        super(offer, takerNetworkId, baseSideAmount, quoteSideAmount, protocolType, baseSidePaymentMethodSpecs, quoteSidePaymentMethodSpec);
    }

    @Override
    public bisq.protocol.protobuf.Trade toProto() {
        return getTradeBuilder().setBisqEasyTrade(bisq.protocol.protobuf.BisqEasyTrade.newBuilder()).build();
    }

    public static BisqEasyTrade fromProto(bisq.protocol.protobuf.Trade proto) {
        BitcoinPaymentMethodSpec baseSidePaymentMethodSpec = PaymentMethodSpec.protoToBitcoinPaymentMethodSpec(proto.getBaseSidePaymentMethodSpec());
        FiatPaymentMethodSpec quoteSidePaymentMethodSpec = PaymentMethodSpec.protoToFiatPaymentMethodSpec(proto.getQuoteSidePaymentMethodSpec());

        return new BisqEasyTrade(BisqEasyOffer.fromProto(proto.getOffer()),
                NetworkId.fromProto(proto.getTakerNetworkId()),
                proto.getBaseSideAmount(),
                proto.getQuoteSideAmount(),
                ProtobufUtils.enumFromProto(TradeProtocolType.class, proto.getProtocolTypeName()),
                baseSidePaymentMethodSpec,
                quoteSidePaymentMethodSpec);
    }
}