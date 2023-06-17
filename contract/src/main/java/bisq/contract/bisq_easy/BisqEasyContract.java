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
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TwoPartyContract;
import bisq.network.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class BisqEasyContract extends TwoPartyContract<BisqEasyOffer> {
    private final long baseSideAmount;
    private final long quoteSideAmount;
    protected final BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs;
    protected final FiatPaymentMethodSpec quoteSidePaymentMethodSpec;
    private final Optional<UserProfile> mediator;

    public BisqEasyContract(BisqEasyOffer offer,
                            NetworkId takerNetworkId,
                            long baseSideAmount,
                            long quoteSideAmount,
                            BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs,
                            FiatPaymentMethodSpec quoteSidePaymentMethodSpec,
                            Optional<UserProfile> mediator) {
        this(offer,
                TradeProtocolType.BISQ_EASY,
                new Party(Role.TAKER, takerNetworkId),
                baseSideAmount,
                quoteSideAmount,
                baseSidePaymentMethodSpecs,
                quoteSidePaymentMethodSpec,
                mediator);
    }

    private BisqEasyContract(BisqEasyOffer offer,
                             TradeProtocolType protocolType,
                             Party taker,
                             long baseSideAmount,
                             long quoteSideAmount,
                             BitcoinPaymentMethodSpec baseSidePaymentMethodSpecs,
                             FiatPaymentMethodSpec quoteSidePaymentMethodSpec,
                             Optional<UserProfile> mediator) {
        super(offer, protocolType, taker);
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.baseSidePaymentMethodSpecs = baseSidePaymentMethodSpecs;
        this.quoteSidePaymentMethodSpec = quoteSidePaymentMethodSpec;
        this.mediator = mediator;
    }

    @Override
    public bisq.contract.protobuf.Contract toProto() {
        var bisqEasyContract = bisq.contract.protobuf.BisqEasyContract.newBuilder()
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .setBaseSidePaymentMethodSpecs(baseSidePaymentMethodSpecs.toProto())
                .setQuoteSidePaymentMethodSpec(quoteSidePaymentMethodSpec.toProto());
        mediator.ifPresent(mediator -> bisqEasyContract.setMediator(mediator.toProto()));
        var twoPartyContract = getTwoPartyContractBuilder().setBisqEasyContract(bisqEasyContract);
        return getContractBuilder().setTwoPartyContract(twoPartyContract).build();
    }

    public static BisqEasyContract fromProto(bisq.contract.protobuf.Contract proto) {
        bisq.contract.protobuf.TwoPartyContract twoPartyContractProto = proto.getTwoPartyContract();
        bisq.contract.protobuf.BisqEasyContract bisqEasyContractProto = twoPartyContractProto.getBisqEasyContract();
        return new BisqEasyContract(BisqEasyOffer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(twoPartyContractProto.getTaker()),
                bisqEasyContractProto.getBaseSideAmount(),
                bisqEasyContractProto.getQuoteSideAmount(),
                PaymentMethodSpec.protoToBitcoinPaymentMethodSpec(bisqEasyContractProto.getBaseSidePaymentMethodSpecs()),
                PaymentMethodSpec.protoToFiatPaymentMethodSpec(bisqEasyContractProto.getQuoteSidePaymentMethodSpec()),
                bisqEasyContractProto.hasMediator() ?
                        Optional.of(UserProfile.fromProto(bisqEasyContractProto.getMediator())) :
                        Optional.empty());
    }
}