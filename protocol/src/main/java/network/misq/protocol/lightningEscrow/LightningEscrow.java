package network.misq.protocol.lightningEscrow;

import network.misq.protocol.SecurityProvider;
import network.misq.protocol.sharedState.*;

import static network.misq.protocol.sharedState.SharedStateFactory.oneOf;

public class LightningEscrow implements SecurityProvider {
    @Override
    public Type getType() {
        return Type.ESCROW;
    }

    @State(parties = {"maker", "taker", "escrowAgent"})
    public interface SharedState extends SecurityProvider.SharedState {
        // STARTING SECRETS...

        @Access("escrowAgentReceivesMakerPendingPayment & escrowAgentReceivesTakerPendingPayment")
        @Supplied(by = "escrowAgent")
        @DependsOn("makerReceivesMakerPaymentReceipt")
        @DependsOn("takerReceivesTakerPaymentReceipt")
        default String escrowAgentNonce1() {
            return oneOf(
                    this::makerReceivesMakerPaymentReceipt,
                    this::takerReceivesTakerPaymentReceipt
            );
        }

        @Access("maker | makerStartsCountercurrencyPayment")
        @Supplied(by = "escrowAgent")
        String escrowAgentNonce2();

        @Access("takerConfirmsCountercurrencyPayment")
        @Supplied(by = "taker")
        String takerNonce();

        @Supplied(by = "taker")
        String obliviousTransferKey();

        // DERIVED PROPERTIES...

        @DependsOn({"escrowAgentNonce1", "escrowAgentNonce2"})
        default String garbledCircuitGenerator() {
            return "generator(" + escrowAgentNonce1() + ", " + escrowAgentNonce2() + ")";
        }

        @Access("taker")
        @DependsOn("garbledCircuitGenerator")
        default String garbledCircuit() {
            return "garbledCircuit(" + garbledCircuitGenerator() + ")";
        }

        @Access("escrowAgent")
        @DependsOn({"obliviousTransferKey", "takerNonce"})
        default String obliviousRequest() {
            return "encrypt(" + obliviousTransferKey() + ", " + takerNonce() + ")";
        }

        @Access("taker")
        @DependsOn({"garbledCircuitGenerator", "obliviousRequest"})
        default String obliviousResponse() {
            return "garbleObliviously(" + garbledCircuitGenerator() + ", " + obliviousRequest() + ")";
        }

        @DependsOn({"obliviousTransferKey", "obliviousResponse"})
        default String garbledTakerNonce() {
            return "decrypt(" + obliviousTransferKey() + ", " + obliviousResponse() + ")";
        }

        @Access("ALL")
        @DependsOn("garbledCircuit")
        @DependsOn("escrowAgentNonce1")
        @DependsOn("escrowAgentReceivesMakerPendingPayment")
        @DependsOn("escrowAgentReceivesTakerPendingPayment")
        default String escrowAgentNonce1Hash() {
            return oneOf(
                    () -> "lazyEvaluate(" + garbledCircuit() + ", null)(0)",
                    () -> "SHA256(" + escrowAgentNonce1() + ")",
                    this::escrowAgentReceivesMakerPendingPayment,
                    this::escrowAgentReceivesTakerPendingPayment
            );
        }

        @DependsOn("garbledCircuit")
        @DependsOn("escrowAgentNonce2")
        default String escrowAgentNonce2Hash() {
            return oneOf(
                    () -> "lazyEvaluate(" + garbledCircuit() + ", null)(1)",
                    () -> "SHA256(" + escrowAgentNonce2() + ")"
            );
        }

        @Access("takerConfirmsCountercurrencyPayment")
        @DependsOn({"escrowAgentNonce1", "escrowAgentNonce2", "takerNonce"})
        @DependsOn("escrowAgentReceivesEscrowAgentPaymentReceipt")
        default String escrowAgentPaymentPreimage() {
            return oneOf(
                    () -> escrowAgentNonce1() + " XOR " + escrowAgentNonce2() + " XOR " + takerNonce(),
                    this::escrowAgentReceivesEscrowAgentPaymentReceipt
            );
        }

        @DependsOn({"garbledCircuit", "garbledTakerNonce"})
        @DependsOn({"garbledCircuitGenerator", "garbledEscrowAgentPaymentHash"})
        @DependsOn("escrowAgentPaymentPreimage")
        @DependsOn("makerReceivesEscrowAgentPendingPayment")
        @DependsOn("takerReceivesEscrowAgentPendingPayment")
        default String escrowAgentPaymentHash() {
            return oneOf(
                    () -> "evaluate(" + garbledCircuit() + ", " + garbledTakerNonce() + ")(2)",
                    () -> "ungarble(" + garbledCircuitGenerator() + ", " + garbledEscrowAgentPaymentHash() + ")",
                    () -> "SHA256(" + escrowAgentPaymentPreimage() + ")",
                    this::makerReceivesEscrowAgentPendingPayment,
                    this::takerReceivesEscrowAgentPendingPayment
            );
        }

        @Access("escrowAgent")
        @DependsOn({"garbledCircuit", "garbledTakerNonce"})
        default String garbledEscrowAgentPaymentHash() {
            return "evaluate(" + garbledCircuit() + ", " + garbledTakerNonce() + ")(3)";
        }

        // ACTIONS AND EVENTS...

        @Action(by = "escrowAgent")
        @DependsOn("escrowAgentPaymentHash")
        default void escrowAgentMakesPendingPayments() {
        }

        @Event(seenBy = "maker")
        String makerReceivesEscrowAgentPendingPayment();

        @Event(seenBy = "taker")
        String takerReceivesEscrowAgentPendingPayment();

        @Action(by = "maker")
        @DependsOn({"escrowAgentNonce1Hash", "escrowAgentNonce2Hash", "escrowAgentNonce2", "makerReceivesEscrowAgentPendingPayment"})
        default void makerMakesPendingPayment() {
        }

        @Action(by = "taker")
        @DependsOn({"escrowAgentNonce1Hash", "escrowAgentNonce2Hash", "takerReceivesEscrowAgentPendingPayment"})
        default void takerMakesPendingPayment() {
        }

        @Event(seenBy = "escrowAgent")
        String escrowAgentReceivesMakerPendingPayment();

        @Event(seenBy = "escrowAgent")
        String escrowAgentReceivesTakerPendingPayment();

        @Action(by = "escrowAgent")
        @DependsOn({"escrowAgentReceivesMakerPendingPayment", "escrowAgentReceivesTakerPendingPayment"})
        default void escrowAgentFinalizesMakerAndTakerPayments() {
        }

        @Event(seenBy = "maker")
        String makerReceivesMakerPaymentReceipt();

        @Event(seenBy = "taker")
        String takerReceivesTakerPaymentReceipt();

        @Event(seenBy = "maker")
        Unit makerStartsCountercurrencyPayment();

        @Event(seenBy = "taker")
        Unit takerConfirmsCountercurrencyPayment();

        @Action(by = "taker")
        @DependsOn({"takerConfirmsCountercurrencyPayment", "escrowAgentPaymentPreimage"})
        default void takerFinalizesEscrowAgentPayment() {
        }

        @Event(seenBy = "escrowAgent")
        String escrowAgentReceivesEscrowAgentPaymentReceipt();
    }
}
