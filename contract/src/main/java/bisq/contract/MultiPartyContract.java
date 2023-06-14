package bisq.contract;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class MultiPartyContract<T extends Offer<?, ?>> extends TwoPartyContract<T> {
    private static Party resolveTaker(List<Party> parties) {
        return parties.stream().filter(e -> e.getRole() == Role.TAKER).findAny().orElseThrow();
    }

    private final List<Party> parties;

    public MultiPartyContract(T listing, TradeProtocolType protocolType, List<Party> parties) {
        super(listing, protocolType, resolveTaker(parties));
        this.parties = new ArrayList<>(parties);
        this.parties.sort(Comparator.comparingInt(Party::hashCode));
    }

    @Override
    public bisq.contract.protobuf.Contract toProto() {
        return getContractBuilder().setMultiPartyContract(
                        bisq.contract.protobuf.MultiPartyContract.newBuilder()
                                .addAllParties(parties.stream().map(Party::toProto).collect(Collectors.toList())))
                .build();
    }

    public static TwoPartyContract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        switch (proto.getTwoPartyContract().getMessageCase()) {
            // no impl yet
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
    
    /*public static MultiPartyContract<? extends Offer<?, ?>> fromProto(bisq.contract.protobuf.Contract proto) {
        return new MultiPartyContract<>(Offer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                proto.getMultiPartyContract().getPartiesList().stream().map(Party::fromProto).collect(Collectors.toList()));
    }*/

}
