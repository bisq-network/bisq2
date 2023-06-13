package bisq.contract.poc;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.contract.Contract;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.offer.Offer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MultiPartyContract<T extends Offer> extends Contract<T> {
    private final List<Party> parties;

    public MultiPartyContract(T listing, TradeProtocolType protocolType, List<Party> parties) {
        super(listing, protocolType);
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

    public static MultiPartyContract<? extends Offer> fromProto(bisq.contract.protobuf.Contract proto) {
        return new MultiPartyContract<>(Offer.fromProto(proto.getOffer()),
                TradeProtocolType.fromProto(proto.getTradeProtocolType()),
                proto.getMultiPartyContract().getPartiesList().stream().map(Party::fromProto).collect(Collectors.toList()));
    }

    @Override
    public Party getTaker() {
        return parties.stream().filter(e -> e.getRole() == Role.TAKER).findAny().orElseThrow();
    }
}
