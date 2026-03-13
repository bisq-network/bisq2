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

package bisq.contract.mu_sig;

import bisq.account.payment_method.PaymentMethodSpec;
import bisq.account.payment_method.PaymentMethodSpecUtil;
import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.market.Market;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.contract.TwoPartyContract;
import bisq.network.identity.NetworkId;
import bisq.offer.mu_sig.MuSigOffer;
import bisq.offer.options.AccountOption;
import bisq.offer.options.OfferOptionUtil;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public class MuSigContract extends TwoPartyContract<MuSigOffer> {

    private final long baseSideAmount;
    private final long quoteSideAmount;
    private final PaymentMethodSpec<?> baseSidePaymentMethodSpec;
    private final PaymentMethodSpec<?> quoteSidePaymentMethodSpec;
    private final Optional<UserProfile> mediator;
    private final PriceSpec priceSpec;
    private final long marketPrice;

    public MuSigContract(long takeOfferDate,
                         MuSigOffer offer,
                         NetworkId takerNetworkId,
                         long baseSideAmount,
                         long quoteSideAmount,
                         PaymentMethodSpec<?> paymentMethodSpec,
                         byte[] takerSaltedAccountPayloadHash,
                         Optional<UserProfile> mediator,
                         PriceSpec priceSpec,
                         long marketPrice) {
        this(takeOfferDate,
                offer,
                takerNetworkId,
                baseSideAmount,
                quoteSideAmount,
                takerSaltedAccountPayloadHash,
                mediator,
                priceSpec,
                marketPrice,
                getBaseSidePaymentMethodSpec(offer, paymentMethodSpec),
                getQuoteSidePaymentMethodSpec(offer, paymentMethodSpec));
    }

    private MuSigContract(long takeOfferDate,
                          MuSigOffer offer,
                          NetworkId takerNetworkId,
                          long baseSideAmount,
                          long quoteSideAmount,
                          byte[] takerSaltedAccountPayloadHash,
                          Optional<UserProfile> mediator,
                          PriceSpec priceSpec,
                          long marketPrice,
                          PaymentMethodSpec<?> baseSidePaymentMethodSpec,
                          PaymentMethodSpec<?> quoteSidePaymentMethodSpec) {
        this(takeOfferDate,
                offer,
                TradeProtocolType.MU_SIG,
                resolveMakerParty(offer, baseSidePaymentMethodSpec, quoteSidePaymentMethodSpec),
                new Party(Role.TAKER, takerNetworkId, Optional.of(takerSaltedAccountPayloadHash)),
                baseSideAmount,
                quoteSideAmount,
                baseSidePaymentMethodSpec,
                quoteSidePaymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice);
    }

    public MuSigContract(long takeOfferDate,
                         MuSigOffer offer,
                         TradeProtocolType protocolType,
                         Party maker,
                         Party taker,
                         long baseSideAmount,
                         long quoteSideAmount,
                         PaymentMethodSpec<?> baseSidePaymentMethodSpec,
                         PaymentMethodSpec<?> quoteSidePaymentMethodSpec,
                         Optional<UserProfile> mediator,
                         PriceSpec priceSpec,
                         long marketPrice) {
        super(takeOfferDate, offer, protocolType, maker, taker);
        this.baseSideAmount = baseSideAmount;
        this.quoteSideAmount = quoteSideAmount;
        this.baseSidePaymentMethodSpec = baseSidePaymentMethodSpec;
        this.quoteSidePaymentMethodSpec = quoteSidePaymentMethodSpec;
        this.mediator = mediator;
        this.priceSpec = priceSpec;
        this.marketPrice = marketPrice;

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
                .setMuSigContract(toMuSigContractProto(serializeForHash));
    }

    private bisq.contract.protobuf.MuSigContract toMuSigContractProto(boolean serializeForHash) {
        return resolveBuilder(getMuSigContractBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.contract.protobuf.MuSigContract.Builder getMuSigContractBuilder(boolean serializeForHash) {
        bisq.contract.protobuf.MuSigContract.Builder builder = bisq.contract.protobuf.MuSigContract.newBuilder()
                .setBaseSideAmount(baseSideAmount)
                .setQuoteSideAmount(quoteSideAmount)
                .setBaseSidePaymentMethodSpec(baseSidePaymentMethodSpec.toProto(serializeForHash))
                .setQuoteSidePaymentMethodSpec(quoteSidePaymentMethodSpec.toProto(serializeForHash))
                .setPriceSpec(priceSpec.toProto(serializeForHash))
                .setMarketPrice(marketPrice);
        mediator.ifPresent(mediator -> builder.setMediator(mediator.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.contract.protobuf.Contract toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigContract fromProto(bisq.contract.protobuf.Contract proto) {
        bisq.contract.protobuf.TwoPartyContract twoPartyContractProto = proto.getTwoPartyContract();
        bisq.contract.protobuf.MuSigContract muSigContractProto = twoPartyContractProto.getMuSigContract();
        MuSigOffer muSigOffer = MuSigOffer.fromProto(proto.getOffer());
        Market market = muSigOffer.getMarket();
        PaymentMethodSpec<?> baseSidePaymentMethodSpec = PaymentMethodSpec.fromProto(
                muSigContractProto.getBaseSidePaymentMethodSpec(),
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForBaseSide(market));
        PaymentMethodSpec<?> quoteSidePaymentMethodSpec = PaymentMethodSpec.fromProto(
                muSigContractProto.getQuoteSidePaymentMethodSpec(),
                PaymentMethodSpecUtil.getPaymentMethodSpecClassForQuoteSide(market));
        return new MuSigContract(proto.getTakeOfferDate(),
                muSigOffer,
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                resolveMakerParty(muSigOffer, baseSidePaymentMethodSpec, quoteSidePaymentMethodSpec),
                Party.fromProto(twoPartyContractProto.getTaker()),
                muSigContractProto.getBaseSideAmount(),
                muSigContractProto.getQuoteSideAmount(),
                baseSidePaymentMethodSpec,
                quoteSidePaymentMethodSpec,
                muSigContractProto.hasMediator() ?
                        Optional.of(UserProfile.fromProto(muSigContractProto.getMediator())) :
                        Optional.empty(),
                PriceSpec.fromProto(muSigContractProto.getPriceSpec()),
                muSigContractProto.getMarketPrice());
    }

    private static PaymentMethodSpec<?> getBaseSidePaymentMethodSpec(MuSigOffer offer,
                                                                     PaymentMethodSpec<?> paymentMethodSpec) {
        if (offer.getMarket().isBaseCurrencyBitcoin()) {
            checkArgument(offer.getBaseSidePaymentMethodSpecs().size() == 1,
                    "MuSigOffer's baseSidePaymentMethodSpecs must have exactly 1 item");
            return offer.getBaseSidePaymentMethodSpecs().get(0);
        } else {
            return paymentMethodSpec;
        }
    }

    private static PaymentMethodSpec<?> getQuoteSidePaymentMethodSpec(MuSigOffer offer,
                                                                      PaymentMethodSpec<?> paymentMethodSpec) {
        if (offer.getMarket().isBaseCurrencyBitcoin()) {
            return paymentMethodSpec;
        } else {
            checkArgument(offer.getQuoteSidePaymentMethodSpecs().size() == 1,
                    "MuSigOffer's quoteSidePaymentMethodSpecs must have exactly 1 item");
            return offer.getQuoteSidePaymentMethodSpecs().get(0);
        }
    }

    private static Party resolveMakerParty(MuSigOffer offer,
                                           PaymentMethodSpec<?> baseSidePaymentMethodSpec,
                                           PaymentMethodSpec<?> quoteSidePaymentMethodSpec) {
        PaymentMethodSpec<?> nonBtcSidePaymentMethodSpec = offer.getMarket().isBaseCurrencyBitcoin()
                ? quoteSidePaymentMethodSpec
                : baseSidePaymentMethodSpec;
        Optional<byte[]> saltedAccountPayloadHash = OfferOptionUtil.findAccountOptions(offer.getOfferOptions()).stream()
                .filter(accountOption -> accountOption.getPaymentMethod().equals(nonBtcSidePaymentMethodSpec.getPaymentMethod()))
                .findFirst()
                .map(AccountOption::getSaltedAccountPayloadHash);
        return new Party(Role.MAKER, offer.getMakerNetworkId(), saltedAccountPayloadHash);
    }

    public PaymentMethodSpec<?> getBtcSidePaymentMethodSpec() {
        return offer.getMarket().isBaseCurrencyBitcoin() ? baseSidePaymentMethodSpec : quoteSidePaymentMethodSpec;
    }

    public PaymentMethodSpec<?> getNonBtcSidePaymentMethodSpec() {
        return offer.getMarket().isBaseCurrencyBitcoin() ? quoteSidePaymentMethodSpec : baseSidePaymentMethodSpec;
    }

    public long getBtcSideAmount() {
        return offer.getMarket().isBaseCurrencyBitcoin() ? baseSideAmount : quoteSideAmount;
    }

    public long getNonBtcSideAmount() {
        return offer.getMarket().isBaseCurrencyBitcoin() ? quoteSideAmount : baseSideAmount;
    }
}
