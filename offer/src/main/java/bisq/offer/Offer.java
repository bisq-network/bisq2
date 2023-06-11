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

import bisq.account.protocol_type.ProtocolType;
import bisq.common.currency.Market;
import bisq.common.proto.Proto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.NetworkId;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.MinMaxAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.options.OfferOption;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.settlement.SettlementSpec;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public abstract class Offer implements Proto {
    protected final String id;
    protected final long date;
    protected final NetworkId makerNetworkId;
    protected final Direction direction;
    protected final Market market;
    private final AmountSpec amountSpec;
    protected final PriceSpec priceSpec;
    protected final List<ProtocolType> protocolTypes;
    protected final List<SettlementSpec> baseSideSettlementSpecs;
    protected final List<SettlementSpec> quoteSideSettlementSpecs;
    protected final List<OfferOption> offerOptions;

    public Offer(String id,
                 long date,
                 NetworkId makerNetworkId,
                 Direction direction,
                 Market market,
                 AmountSpec amountSpec,
                 PriceSpec priceSpec,
                 List<ProtocolType> protocolTypes,
                 List<SettlementSpec> baseSideSettlementSpecs,
                 List<SettlementSpec> quoteSideSettlementSpecs,
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
        this.baseSideSettlementSpecs = new ArrayList<>(baseSideSettlementSpecs);
        this.quoteSideSettlementSpecs = new ArrayList<>(quoteSideSettlementSpecs);
        this.offerOptions = new ArrayList<>(offerOptions);

        // All lists need to sort deterministically as the data is used in the proof of work check
        this.protocolTypes.sort(Comparator.comparingInt(ProtocolType::hashCode));
        this.baseSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.quoteSideSettlementSpecs.sort(Comparator.comparingInt(SettlementSpec::hashCode));
        this.offerOptions.sort(Comparator.comparingInt(OfferOption::hashCode));
    }

    public abstract bisq.offer.protobuf.Offer toProto();

    protected bisq.offer.protobuf.Offer.Builder getSwapOfferBuilder() {
        return bisq.offer.protobuf.Offer.newBuilder()
                .setId(id)
                .setDate(date)
                .setMakerNetworkId(makerNetworkId.toProto())
                .setDirection(direction.toProto())
                .setMarket(market.toProto())
                .setAmountSpec(amountSpec.toProto())
                .setPriceSpec(priceSpec.toProto())
                .addAllProtocolTypes(protocolTypes.stream().map(ProtocolType::toProto).collect(Collectors.toList()))
                .addAllBaseSideSettlementSpecs(baseSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllQuoteSideSettlementSpecs(quoteSideSettlementSpecs.stream().map(SettlementSpec::toProto).collect(Collectors.toList()))
                .addAllOfferOptions(offerOptions.stream().map(OfferOption::toProto).collect(Collectors.toList()));
    }


    public static Offer fromProto(bisq.offer.protobuf.Offer proto) {
        switch (proto.getMessageCase()) {
            case BISQEASYOFFER: {
                return BisqEasyOffer.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
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
        return amountSpec instanceof MinMaxAmountSpec;
    }
}
