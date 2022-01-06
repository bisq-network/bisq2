package bisq.protocol.lightningEscrow.taker;

import bisq.contract.AssetTransfer;
import bisq.contract.ManyPartyContract;
import bisq.network.NetworkService;
import bisq.network.p2p.message.Message;
import bisq.network.p2p.node.CloseReason;
import bisq.network.p2p.node.Connection;
import bisq.protocol.lightningEscrow.LightningEscrow;
import bisq.protocol.lightningEscrow.LightningEscrowProtocol;

import java.util.concurrent.CompletableFuture;

public class TakerLightningEscrowProtocol extends LightningEscrowProtocol {
    public TakerLightningEscrowProtocol(ManyPartyContract contract, NetworkService networkService) {
        super(contract, networkService, new AssetTransfer.Automatic(), new LightningEscrow());
    }

    @Override
    public CompletableFuture<Boolean> start() {
        return null;
    }

    @Override
    public void onMessage(Message message, Connection connection, String nodeId) {
    }

    @Override
    public void onConnection(Connection connection) {
    }

    @Override
    public void onDisconnect(Connection connection, CloseReason closeReason) {
    }
}
