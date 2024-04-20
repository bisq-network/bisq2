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
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public final class BisqEasyContract extends TwoPartyContract<BisqEasyOffer> {
    private final long baseSideAmount;
    private final long quoteSideAmount;
    private final BitcoinPaymentMethodSpec baseSidePaymentMethodSpec;
    private final FiatPaymentMethodSpec quoteSidePaymentMethodSpec;
    private final Optional<UserProfile> mediator;
    private final PriceSpec agreedPriceSpec;
    private final long marketPrice;
    private final long takeOfferDate;

    public BisqEasyContract(long takeOfferDate,
                            BisqEasyOffer offer,
                            NetworkId takerNetworkId,
                            long baseSideAmount,
                            long quoteSideAmount,
                            BitcoinPaymentMethodSpec baseSidePaymentMethodSpec,
                            FiatPaymentMethodSpec quoteSidePaymentMethodSpec,
                            Optional<UserProfile> mediator,
                            PriceSpec agreedPriceSpec,
                            long marketPrice) {
        this(takeOfferDate,
                offer,
                TradeProtocolType.BISQ_EASY,
                new Party(Role.TAKER, takerNetworkId),
                baseSideAmount,
                quoteSideAmount,
                baseSidePaymentMethodSpec,
                quoteSidePaymentMethodSpec,
                mediator,
                agreedPriceSpec,
                marketPrice);
    }

    private BisqEasyContract(long takeOfferDate,
                             BisqEasyOffer offer,
                             TradeProtocolType protocolType,
                             Party taker,
                             long baseSideAmount,
                             long quoteSideAmount,
                             BitcoinPaymentMethodSpec baseSidePaymentMethodSpec,
                             FiatPaymentMethodSpec quoteSidePaymentMethodSpec,
                             Optional<UserProfile> mediator,
                             PriceSpec agreedPriceSpec,
                             long marketPrice) {
        super(takeOfferDate, offer, protocolType, taker);
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.baseSidePaymentMethodSpec = baseSidePaymentMethodSpec;
        this.quoteSidePaymentMethodSpec = quoteSidePaymentMethodSpec;
        this.mediator = mediator;
        this.agreedPriceSpec = agreedPriceSpec;
        this.marketPrice = marketPrice;
        this.takeOfferDate = takeOfferDate;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
    }

    @Override
    public bisq.contract.protobuf.Contract.Builder getBuilder(boolean serializeForHash) {
        return getContractBuilder(serializeForHash).setTwoPartyContract(toTwoPartyContract(serializeForHash));
    }

    @Override
    protected bisq.contract.protobuf.TwoPartyContract.Builder getTwoPartyContractBuilder(boolean serializeForHash) {
        return super.getTwoPartyContractBuilder(serializeForHash)
                .setBisqEasyContract(toBisqEasyContractProto(serializeForHash));
    }

    private bisq.contract.protobuf.BisqEasyContract toBisqEasyContractProto(boolean serializeForHash) {
        return resolveBuilder(getBisqEasyContractBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.contract.protobuf.BisqEasyContract.Builder getBisqEasyContractBuilder(boolean serializeForHash) {
        bisq.contract.protobuf.BisqEasyContract.Builder builder = bisq.contract.protobuf.BisqEasyContract.newBuilder()
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .setBaseSidePaymentMethodSpec(baseSidePaymentMethodSpec.toProto(serializeForHash))
                .setQuoteSidePaymentMethodSpec(quoteSidePaymentMethodSpec.toProto(serializeForHash))
                .setAgreedPriceSpec(agreedPriceSpec.toProto(serializeForHash))
                .setMarketPrice(marketPrice);
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.contract.protobuf.Contract toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static BisqEasyContract fromProto(bisq.contract.protobuf.Contract proto) {
        bisq.contract.protobuf.TwoPartyContract twoPartyContractProto = proto.getTwoPartyContract();
        bisq.contract.protobuf.BisqEasyContract bisqEasyContractProto = twoPartyContractProto.getBisqEasyContract();
        return new BisqEasyContract(proto.getTakeOfferDate(),
                BisqEasyOffer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                Party.fromProto(twoPartyContractProto.getTaker()),
                bisqEasyContractProto.getBaseSideAmount(),
                bisqEasyContractProto.getQuoteSideAmount(),
                PaymentMethodSpec.protoToBitcoinPaymentMethodSpec(bisqEasyContractProto.getBaseSidePaymentMethodSpec()),
                PaymentMethodSpec.protoToFiatPaymentMethodSpec(bisqEasyContractProto.getQuoteSidePaymentMethodSpec()),
                bisqEasyContractProto.hasMediator() ?
                        Optional.of(UserProfile.fromProto(bisqEasyContractProto.getMediator())) :
                        Optional.empty(),
                PriceSpec.fromProto(bisqEasyContractProto.getAgreedPriceSpec()),
                bisqEasyContractProto.getMarketPrice());
    }
}
