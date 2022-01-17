package bisq.contract;

import lombok.Getter;

import java.util.Set;

@Getter
public class MultiPartyContract extends Contract {
    private final Set<Party> parties;

    public MultiPartyContract(ProtocolType protocolType, Party maker, Set<Party> parties, SettlementExecution settlementExecution) {
        super(protocolType, maker, settlementExecution);

        this.parties = Set.copyOf(parties);
    }
}
