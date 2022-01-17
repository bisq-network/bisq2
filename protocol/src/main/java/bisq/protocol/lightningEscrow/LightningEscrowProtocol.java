package bisq.protocol.lightningEscrow;

import bisq.contract.MultiPartyContract;
import bisq.contract.SettlementExecution;
import bisq.network.NetworkIdWithKeyPair;
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

    private final SettlementExecution transport;
    private final LightningEscrow security;

    public LightningEscrowProtocol(NetworkService networkService,
                                   NetworkIdWithKeyPair networkIdWithKeyPair,
                                   MultiPartyContract contract,
                                   SettlementExecution settlementExecution,
                                   SecurityProvider securityProvider) {
        super(networkService, networkIdWithKeyPair, contract);
        transport = settlementExecution;
        security = (LightningEscrow) securityProvider;

        if (settlementExecution instanceof SettlementExecution.Manual manualSettlementExecution) {
            manualSettlementExecution.addListener(this::onStartManualPayment);
            addListener(state -> {
                if (state == State.MANUAL_PAYMENT_STARTED) {
                    manualSettlementExecution.onManualPaymentStarted();
                }
            });
        }
    }

    private void onStartManualPayment() {
    }

    public void onManualPaymentStarted() {
    }
}
