package bisq.protocol.lightningEscrow.taker;

import bisq.contract.MultiPartyContract;
import bisq.protocol.SettlementExecution;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.protocol.lightningEscrow.LightningEscrow;
import bisq.protocol.lightningEscrow.LightningEscrowProtocol;

import java.util.concurrent.CompletableFuture;

public class TakerLightningEscrowProtocol extends LightningEscrowProtocol {
    public TakerLightningEscrowProtocol(NetworkService networkService,
                                        NetworkIdWithKeyPair networkIdWithKeyPair,
                                        MultiPartyContract contract) {
        super(networkService, networkIdWithKeyPair, contract, new SettlementExecution.Automatic(), new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message) {
    }
}
