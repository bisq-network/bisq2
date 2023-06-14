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

package bisq.contract.bisq_easy;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.monetary.Monetary;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TwoPartyContract;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BisqEasyContract extends TwoPartyContract<BisqEasyOffer> {

    private final Monetary baseSideAmount;
    private final Monetary quoteSideAmount;
    protected final BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs;
    protected final FiatPaymentMethodSpec quoteSidePaymentMethodSpec;

    public BisqEasyContract(
            BisqEasyOffer offer,
            NetworkId takerNetworkId,
            Monetary baseSideAmount,
            Monetary quoteSideAmount,
            BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs,
            FiatPaymentMethodSpec quoteSidePaymentMethodSpec) {
        super(offer, TradeProtocolType.BISQ_EASY, new Party(Role.TAKER, takerNetworkId));

        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.baseSidePaymentMethodSpecs = baseSidePaymentMethodSpecs;
        this.quoteSidePaymentMethodSpec = quoteSidePaymentMethodSpec;
    }

    @Override
    public bisq.contract.protobuf.Contract toProto() {
        return null;
    }

   

  /*  public static BisqEasyContract<? extends Offer> fromProto(bisq.contract.protobuf.Contract proto) {
        return new BisqEasyContract<>(Offer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(proto.getTwoPartyContract().getTaker()));
    }*/
}