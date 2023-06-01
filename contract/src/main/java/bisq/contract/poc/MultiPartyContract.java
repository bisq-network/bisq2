package bisq.contract.poc;

import bisq.account.protocol_type.SwapProtocolType;
import bisq.contract.Party;
import bisq.contract.SwapContract;
import bisq.offer.SwapOffer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MultiPartyContract<T extends SwapOffer> extends SwapContract<T> {
    private final List<Party> parties;

    public MultiPartyContract(T listing, SwapProtocolType protocolType, List<Party> parties) {
        super(listing, protocolType);
        this.parties = new ArrayList<>(parties);
        this.parties.sort(Comparator.comparingInt(Party::hashCode));
    }

    @Override
    public bisq.contract.protobuf.SwapContract toProto() {
        return getSwapContractBuilder().setMultiPartyContract(
                        bisq.contract.protobuf.MultiPartyContract.newBuilder()
                                .addAllParties(parties.stream().map(Party::toProto).collect(Collectors.toList())))
                .build();
    }

    public static MultiPartyContract<? extends SwapOffer> fromProto(bisq.contract.protobuf.SwapContract proto) {
        return new MultiPartyContract<>(SwapOffer.fromProto(proto.getSwapOffer()),
                SwapProtocolType.fromProto(proto.getProtocolType()),
                proto.getMultiPartyContract().getPartiesList().stream().map(Party::fromProto).collect(Collectors.toList()));
    }
}
