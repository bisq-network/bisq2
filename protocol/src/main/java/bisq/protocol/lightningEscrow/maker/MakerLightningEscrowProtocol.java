package bisq.protocol.lightningEscrow.maker;

import bisq.contract.AssetTransfer;
import bisq.contract.ManyPartyContract;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.protocol.lightningEscrow.LightningEscrow;
import bisq.protocol.lightningEscrow.LightningEscrowProtocol;

import java.util.concurrent.CompletableFuture;

public class MakerLightningEscrowProtocol extends LightningEscrowProtocol {
    public MakerLightningEscrowProtocol(ManyPartyContract contract, NetworkService networkService) {
        super(contract, networkService, new AssetTransfer.Manual(), new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message) {
    }

}
