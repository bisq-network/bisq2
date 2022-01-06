package bisq.protocol.lightningEscrow;

import bisq.contract.AssetTransfer;
import bisq.contract.ManyPartyContract;
import bisq.network.NetworkService;
import bisq.protocol.ManyPartyProtocol;
import bisq.protocol.Protocol;
import bisq.protocol.SecurityProvider;

public abstract class LightningEscrowProtocol extends ManyPartyProtocol {
    public enum State implements Protocol.State {
        START,
        START_MANUAL_PAYMENT,
        MANUAL_PAYMENT_STARTED
    }

    private final AssetTransfer transport;
    private final LightningEscrow security;

    public LightningEscrowProtocol(ManyPartyContract contract, NetworkService networkService, AssetTransfer assetTransfer, SecurityProvider securityProvider) {
        super(contract, networkService);
        transport = assetTransfer;
        security = (LightningEscrow) securityProvider;

        if (assetTransfer instanceof AssetTransfer.Manual) {
            ((AssetTransfer.Manual) assetTransfer).addListener(this::onStartManualPayment);
            addListener(state -> {
                if (state == State.MANUAL_PAYMENT_STARTED) {
                    ((AssetTransfer.Manual) assetTransfer).onManualPaymentStarted();
                }
            });
        }
    }

    private void onStartManualPayment() {
    }

    public void onManualPaymentStarted() {
    }
}
