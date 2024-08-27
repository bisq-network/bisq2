package bisq.contract;

import bisq.account.protocol_type.TradeProtocolType;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.offer.Offer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ToString(callSuper = true)
@Getter
@EqualsAndHashCode(callSuper = true)
public abstract class MultiPartyContract<T extends Offer<?, ?>> extends TwoPartyContract<T> {
    private static Party resolveTaker(List<Party> parties) {
        return parties.stream().filter(e -> e.getRole() == Role.TAKER).findAny().orElseThrow();
    }

    private final List<Party> parties;

    public MultiPartyContract(long takeOfferDate, T offer, TradeProtocolType protocolType, List<Party> parties) {
        super(takeOfferDate, offer, protocolType, resolveTaker(parties));
        this.parties = new ArrayList<>(parties);
        this.parties.sort(Comparator.comparingInt(Party::hashCode));
    }

    @Override
    public bisq.contract.protobuf.Contract.Builder getBuilder(boolean serializeForHash) {
        return getContractBuilder(serializeForHash).setMultiPartyContract(
                bisq.contract.protobuf.MultiPartyContract.newBuilder()
                        .addAllParties(parties.stream()
                                .map(e -> e.toProto(serializeForHash))
                                .collect(Collectors.toList())));
    }

    @Override
    public bisq.contract.protobuf.Contract toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static TwoPartyContract<?> fromProto(bisq.contract.protobuf.Contract proto) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (proto.getTwoPartyContract().getMessageCase()) {
            // no impl yet
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}
