package bisq.protocol.prototype;

import bisq.contract.MultiPartyContract;
import bisq.contract.Party;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;

import java.util.Set;

public abstract class ManyPartyProtocol extends Protocol implements MessageListener {
    protected final Set<Party> parties;

    public ManyPartyProtocol(NetworkService networkService,
                             NetworkIdWithKeyPair networkIdWithKeyPair,
                             MultiPartyContract contract) {
        super(networkService, networkIdWithKeyPair, contract);
        parties = contract.getParties();
    }
}
