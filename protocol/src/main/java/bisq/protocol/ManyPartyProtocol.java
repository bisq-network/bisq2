package bisq.protocol;

import bisq.contract.ManyPartyContract;
import bisq.contract.Party;
import bisq.contract.Role;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Node;

import java.util.Map;

public abstract class ManyPartyProtocol extends Protocol implements Node.Listener {
    protected final Map<Role, Party> partyMap;

    public ManyPartyProtocol(ManyPartyContract contract, NetworkService networkService) {
        super(contract, networkService);
        partyMap = contract.getPartyMap();
    }
}
