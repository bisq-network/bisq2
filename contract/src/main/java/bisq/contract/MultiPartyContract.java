package bisq.contract;

import bisq.account.protocol_type.SwapProtocolType;
import bisq.offer.SwapOffer;
import com.google.protobuf.Message;
import lombok.Getter;

import java.util.Set;

@Getter
public class MultiPartyContract<T extends SwapOffer> extends SwapContract<T> {
    private final Set<Party> parties;

    public MultiPartyContract(T listing, SwapProtocolType protocolType, Set<Party> parties) {
        super(listing, protocolType);
        this.parties = parties;
    }

    @Override
    public Message toProto() {
        return null;
    }
}
