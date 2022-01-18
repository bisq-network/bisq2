package bisq.contract;

import bisq.offer.Listing;
import bisq.offer.protocol.ProtocolType;
import lombok.Getter;

import java.util.Set;

@Getter
public class MultiPartyContract<T extends Listing> extends Contract<T> {
    private final Set<Party> parties;

    public MultiPartyContract(T listing, ProtocolType protocolType, Set<Party> parties) {
        super(listing, protocolType);
        this.parties = parties;
    }
}
