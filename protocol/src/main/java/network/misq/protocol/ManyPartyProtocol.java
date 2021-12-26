package network.misq.protocol;

import network.misq.contract.ManyPartyContract;
import network.misq.contract.Party;
import network.misq.contract.Role;
import network.misq.network.NetworkService;
import network.misq.network.p2p.node.Node;

import java.util.Map;

public abstract class ManyPartyProtocol extends Protocol implements Node.Listener {
    protected final Map<Role, Party> partyMap;

    public ManyPartyProtocol(ManyPartyContract contract, NetworkService networkService) {
        super(contract, networkService);
        partyMap = contract.getPartyMap();
    }
}
