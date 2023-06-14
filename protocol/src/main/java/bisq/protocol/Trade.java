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

package bisq.protocol;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.offer.Offer;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.protocol.bisq_easy.BisqEasyTrade;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public abstract class Trade<B extends PaymentMethodSpec<?>, Q extends PaymentMethodSpec<?>> implements Proto {
    private final Offer<B, Q> offer;
    protected final NetworkId takerNetworkId;
    private final long baseSideAmount;
    private final long quoteSideAmount;
    protected final TradeProtocolType protocolType;
    protected final B baseSidePaymentMethodSpecs;
    protected final Q quoteSidePaymentMethodSpec;

    public Trade(Offer<B, Q> offer,
                 NetworkId takerNetworkId,
                 long baseSideAmount,
                 long quoteSideAmount,
                 TradeProtocolType protocolType,
                 B baseSidePaymentMethodSpecs,
                 Q quoteSidePaymentMethodSpec) {
        this.offer = offer;
        this.takerNetworkId = takerNetworkId;
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.protocolType = protocolType;
        this.baseSidePaymentMethodSpecs = baseSidePaymentMethodSpecs;
        this.quoteSidePaymentMethodSpec = quoteSidePaymentMethodSpec;
    }

    @Override
    public abstract bisq.protocol.protobuf.Trade toProto();

    protected bisq.protocol.protobuf.Trade.Builder getTradeBuilder() {
        return bisq.protocol.protobuf.Trade.newBuilder()
                .setOffer(offer.toProto())
                .setTakerNetworkId(takerNetworkId.toProto())
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .setProtocolTypeName(protocolType.name())
                .setBaseSidePaymentMethodSpec(baseSidePaymentMethodSpecs.toProto())
                .setQuoteSidePaymentMethodSpec(quoteSidePaymentMethodSpec.toProto());
    }

    static BisqEasyTrade protoToBisqEasyTrade(bisq.protocol.protobuf.Trade proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYTRADE: {
                return BisqEasyTrade.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }

    public String getId() {
        return offer.getId();
    }
}