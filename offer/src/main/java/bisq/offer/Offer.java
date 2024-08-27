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

package bisq.offer;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.currency.Market;
import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.submarine.SubmarineOffer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class Offer<B extends PaymentMethodSpec<?>, Q extends PaymentMethodSpec<?>> implements NetworkProto {
    protected final String id;
    protected final long date;
    protected final NetworkId makerNetworkId;
    protected final Direction direction;
    protected final Market market;
    private final AmountSpec amountSpec;
    protected final PriceSpec priceSpec;
    protected final List<TradeProtocolType> protocolTypes;
    protected final List<B> baseSidePaymentMethodSpecs;
    protected final List<Q> quoteSidePaymentMethodSpecs;
    protected final List<OfferOption> offerOptions;

    public Offer(String id,
                 long date,
                 NetworkId makerNetworkId,
                 Direction direction,
                 Market market,
                 AmountSpec amountSpec,
                 PriceSpec priceSpec,
                 List<TradeProtocolType> protocolTypes,
                 List<B> baseSidePaymentMethodSpecs,
                 List<Q> quoteSidePaymentMethodSpecs,
                 List<OfferOption> offerOptions) {
        this.id = id;
        this.date = date;
        this.makerNetworkId = makerNetworkId;
        this.direction = direction;
        this.market = market;
        this.amountSpec = amountSpec;
        this.priceSpec = priceSpec;
        // We might get an immutable list, but we need to sort it, so wrap it into an ArrayList
        this.protocolTypes = new ArrayList<>(protocolTypes);
        this.baseSidePaymentMethodSpecs = new ArrayList<>(baseSidePaymentMethodSpecs);
        this.quoteSidePaymentMethodSpecs = new ArrayList<>(quoteSidePaymentMethodSpecs);
        this.offerOptions = new ArrayList<>(offerOptions);

        // All lists need to sort deterministically as the data is used in the proof of work check
        Collections.sort(this.protocolTypes);
        this.baseSidePaymentMethodSpecs.sort(Comparator.comparingInt(PaymentMethodSpec::hashCode));
        this.quoteSidePaymentMethodSpecs.sort(Comparator.comparingInt(PaymentMethodSpec::hashCode));
        this.offerOptions.sort(Comparator.comparingInt(OfferOption::hashCode));
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateDate(date);

        checkArgument(protocolTypes.size() < 10);
        checkArgument(baseSidePaymentMethodSpecs.size() < 10);
        checkArgument(quoteSidePaymentMethodSpecs.size() < 10);
        checkArgument(offerOptions.size() < 10);
    }

    @Override
    public abstract bisq.offer.protobuf.Offer toProto(boolean serializeForHash);

    protected bisq.offer.protobuf.Offer.Builder getOfferBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.Offer.newBuilder()
                .setId(id)
                .setDate(date)
                .setMakerNetworkId(makerNetworkId.toProto(serializeForHash))
                .setDirection(direction.toProtoEnum())
                .setMarket(market.toProto(serializeForHash))
                .setAmountSpec(amountSpec.toProto(serializeForHash))
                .setPriceSpec(priceSpec.toProto(serializeForHash))
                .addAllProtocolTypes(protocolTypes.stream()
                        .map(TradeProtocolType::toProtoEnum)
                        .collect(Collectors.toList()))
                .addAllBaseSidePaymentSpecs(baseSidePaymentMethodSpecs.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllQuoteSidePaymentSpecs(quoteSidePaymentMethodSpecs.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()))
                .addAllOfferOptions(offerOptions.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }


    public static Offer<? extends PaymentMethodSpec<?>, ? extends PaymentMethodSpec<?>> fromProto(bisq.offer.protobuf.Offer proto) {
        return switch (proto.getMessageCase()) {
            case BISQEASYOFFER -> BisqEasyOffer.fromProto(proto);
            case BISQMUSIGOFFER -> BisqMuSigOffer.fromProto(proto);
            case SUBMARINEOFFER -> SubmarineOffer.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public String getMakersUserProfileId() {
        return makerNetworkId.getPubKey().getId();
    }

    public boolean isMyOffer(String myUserProfileId) {
        return myUserProfileId.equals(getMakersUserProfileId());
    }

    public boolean isMyOffer(Set<String> myUserProfileIds) {
        return myUserProfileIds.contains(getMakersUserProfileId());
    }

    public String getShortId() {
        return id.substring(0, 8);
    }

    public Direction getMakersDirection() {
        return direction;
    }

    public Direction getTakersDirection() {
        return direction.mirror();
    }

    public boolean hasAmountRange() {
        return amountSpec instanceof RangeAmountSpec;
    }
}
