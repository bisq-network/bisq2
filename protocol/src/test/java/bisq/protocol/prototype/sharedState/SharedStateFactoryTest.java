package bisq.protocol.prototype.sharedState;

import bisq.protocol.prototype.MockBitcoind;
import bisq.protocol.prototype.multiSig.MultiSig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static bisq.protocol.prototype.SecurityProvider.Unit.UNIT;
import static bisq.protocol.prototype.sharedState.SharedStateFactory.fireEvent;
import static bisq.protocol.prototype.sharedState.SharedStateFactory.when;

public class SharedStateFactoryTest {
    @Test
    public void testNew() {
//        var factory = new SharedStateFactory<>(LightningEscrow.SharedState.class);
        var factory = new SharedStateFactory<>(MultiSig.SharedState.class);

        System.out.println("Parties:");
        System.out.println(factory.getParties());
        System.out.println("\nDependencyMultimap:");
        factory.getDependencyMultimap().asMap().forEach((id, list) -> System.out.println(id + " -> " + list));
        System.out.println("\nActorMap:");
        factory.getActorMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println("\nSupplierMap:");
        factory.getSupplierMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println("\nEventObserverMap:");
        factory.getEventObserverMap().forEach((id, party) -> System.out.println(id + " -> " + party));
        System.out.println("\nDeclaredAccessConditionMap:");
        factory.getDeclaredAccessConditionMap().forEach((id, cond) -> System.out.println(id + " -> " + cond));
        System.out.println("\nAccessConditionMap:");
        factory.getAccessConditionMap().forEach((id, cond) -> System.out.println(id + " -> " + cond));
        System.out.println("");

        var wallet = new MockBitcoind();
        var makerState = factory.create("maker", state -> {
            when(state.makerWallet()).thenReturn(wallet);
            when(state.makerEscrowPublicKey()).thenReturn("makerPubKey");
            when(state.makerPayoutAddress()).thenReturn("makerPayoutAddress");
        });
        var takerState = factory.create("taker", state -> {
            when(state.takerWallet()).thenReturn(wallet);
            when(state.takerEscrowPublicKey()).thenReturn("takerPubKey");
            when(state.takerPayoutAddress()).thenReturn("takerPayoutAddress");
        });

        Map<String, ?> message;

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(takerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM taker TO maker: " + toString(message = takerState.sendMessage("maker")));
        System.out.println();
        makerState.receiveMessage("taker", message);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(takerState);
        System.out.println();

        System.out.println("FIRING EVENTS: takerSeesDepositTxInMempool, makerSeesDepositTxInMempool, makerStartsCountercurrencyPayment");
        System.out.println();
        fireEvent(takerState::takerSeesDepositTxInMempool, "publishedDepositTx");
        fireEvent(makerState::makerSeesDepositTxInMempool, "publishedDepositTx");
        fireEvent(makerState::makerStartsCountercurrencyPayment, UNIT);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println("FIRING EVENTS: takerConfirmsCountercurrencyPayment, takerSeesPayoutTxInMempool, makerSeesPayoutTxInMempool");
        System.out.println();
        fireEvent(takerState::takerConfirmsCountercurrencyPayment, UNIT);
        fireEvent(takerState::takerSeesPayoutTxInMempool, "publishedPayoutTx");
        fireEvent(makerState::makerSeesPayoutTxInMempool, "publishedPayoutTx");

        System.out.println(takerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM taker TO maker: " + toString(message = takerState.sendMessage("maker")));
        System.out.println();
        makerState.receiveMessage("taker", message);

        System.out.println(makerState);
        System.out.println();

        System.out.println("GOT MESSAGE FROM maker TO taker: " + toString(message = makerState.sendMessage("taker")));
        System.out.println();
        takerState.receiveMessage("maker", message);

        System.out.println(makerState.makerDepositTxInputs());
        System.out.println();
//        System.out.println(makerState.depositTx());
    }

    private static String toString(Map<String, ?> message) {
        var sb = new StringBuilder("{");
        var len = sb.length();
        message.forEach((name, value) ->
                sb.append(len == sb.length() ? "" : ",").append("\n  ").append(name).append(" = ").append(value)
        );
        return sb.append("\r\n}").toString();
    }
}
