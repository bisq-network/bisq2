package bisq.protocol;

import bisq.contract.ManyPartyContract;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;

import java.util.Map;

public abstract class ManyPartyProtocol extends Protocol implements MessageListener {
    protected final Map<Role, Party> partyMap;

    public ManyPartyProtocol(ManyPartyContract contract, NetworkService networkService) {
        super(contract, networkService);
        partyMap = contract.getPartyMap();
    }
}
