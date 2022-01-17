package bisq.protocol.lightningEscrow;

import bisq.contract.SettlementExecution;
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

    private final SettlementExecution transport;
    private final LightningEscrow security;

    public LightningEscrowProtocol(ManyPartyContract contract, 
                                   NetworkService networkService, 
                                   SettlementExecution settlementExecution, 
                                   SecurityProvider securityProvider) {
        super(contract, networkService);
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
