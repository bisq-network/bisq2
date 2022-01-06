/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.protocol.multiSig;

import bisq.protocol.SecurityProvider;
import bisq.protocol.multiSig.maker.FundsSentMessage;
import bisq.protocol.multiSig.maker.TxInputsMessage;
import bisq.protocol.multiSig.taker.DepositTxBroadcastMessage;
import bisq.protocol.multiSig.taker.PayoutTxBroadcastMessage;
import bisq.protocol.sharedState.*;
import bisq.wallets.Chain;
import bisq.wallets.Wallet;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MultiSig implements SecurityProvider, Chain.Listener {
    @State(parties = {"maker", "taker"})
    public interface SharedState extends SecurityProvider.SharedState {
        // STARTING SECRETS...

        @Supplied(by = "maker")
        Wallet makerWallet();

        @Supplied(by = "taker")
        Wallet takerWallet();

        @Access("ALL")
        @Supplied(by = "maker")
        String makerEscrowPublicKey();

        @Access("ALL")
        @Supplied(by = "taker")
        String takerEscrowPublicKey();

        @Access("ALL")
        @Supplied(by = "maker")
        String makerPayoutAddress();

        @Access("ALL")
        @Supplied(by = "taker")
        String takerPayoutAddress();

        // DERIVED PROPERTIES...

        @Access("ALL")
        @DependsOn("makerWallet")
        default String makerDepositTxInputs() {
            return makerWallet().getUtxos().join();
        }

        @Access("ALL")
        @DependsOn("takerWallet")
        default String takerDepositTxInputs() {
            return takerWallet().getUtxos().join();
        }

        @DependsOn({"makerEscrowPublicKey", "takerEscrowPublicKey"})
        default String escrowAddress() {
            return "multisigAddress(" + makerEscrowPublicKey() + ", " + takerEscrowPublicKey() + ")";
        }

        @DependsOn({"makerDepositTxInputs", "takerDepositTxInputs", "escrowAddress"})
        @DependsOn("makerSignedDepositTx")
        @DependsOn("takerSignedDepositTx")
        @DependsOn("depositTx")
        default String unsignedDepositTx() {
            return SharedStateFactory.oneOf(
                    () -> "mergeTxs(" + makerDepositTxInputs() + ", " + takerDepositTxInputs() + ", " + escrowAddress() + ")",
                    () -> "stripSignature(" + makerSignedDepositTx() + ")",
                    () -> "stripSignature(" + takerSignedDepositTx() + ")",
                    () -> "stripSignature(" + depositTx() + ")"
            );
        }

        @Access("ALL")
        @DependsOn({"makerWallet", "unsignedDepositTx"})
        default String makerSignedDepositTx() {
            return makerWallet().sign(unsignedDepositTx()).join();
        }

        @Access("ALL")
        @DependsOn({"takerWallet", "unsignedDepositTx"})
        default String takerSignedDepositTx() {
            return takerWallet().sign(unsignedDepositTx()).join();
        }

        @DependsOn({"makerWallet", "takerSignedDepositTx"})
        @DependsOn({"takerWallet", "makerSignedDepositTx"})
        @DependsOn("makerSeesDepositTxInMempool")
        @DependsOn("takerSeesDepositTxInMempool")
        default String depositTx() {
            return SharedStateFactory.oneOf(
                    () -> makerWallet().sign(takerSignedDepositTx()).join(),
                    () -> takerWallet().sign(makerSignedDepositTx()).join(),
                    this::makerSeesDepositTxInMempool,
                    this::takerSeesDepositTxInMempool
            );
        }

        @DependsOn({"unsignedDepositTx", "makerPayoutAddress", "takerPayoutAddress"})
        @DependsOn("makerSignedPayoutTx")
        @DependsOn("takerSignedPayoutTx")
        @DependsOn("payoutTx")
        default String unsignedPayoutTx() {
            return SharedStateFactory.oneOf(
                    () -> "mergeTxs(outpoint(" + unsignedDepositTx() + "), " + makerPayoutAddress() + ", " + takerPayoutAddress() + ")",
                    () -> "stripSignature(" + makerSignedPayoutTx() + ")",
                    () -> "stripSignature(" + takerSignedPayoutTx() + ")",
                    () -> "stripSignature(" + payoutTx() + ")"
            );
        }

        @Access("makerStartsCountercurrencyPayment")
        @DependsOn({"makerWallet", "unsignedPayoutTx"})
        default String makerSignedPayoutTx() {
            return makerWallet().sign(unsignedPayoutTx()).join();
        }

        @Access("takerConfirmsCountercurrencyPayment")
        @DependsOn({"takerWallet", "unsignedPayoutTx"})
        default String takerSignedPayoutTx() {
            return takerWallet().sign(unsignedPayoutTx()).join();
        }

        @DependsOn({"makerWallet", "takerSignedPayoutTx"})
        @DependsOn({"takerWallet", "makerSignedPayoutTx"})
        @DependsOn("makerSeesPayoutTxInMempool")
        @DependsOn("takerSeesPayoutTxInMempool")
        default String payoutTx() {
            return SharedStateFactory.oneOf(
                    () -> makerWallet().sign(takerSignedPayoutTx()).join(),
                    () -> takerWallet().sign(makerSignedPayoutTx()).join(),
                    this::makerSeesPayoutTxInMempool,
                    this::takerSeesPayoutTxInMempool
            );
        }

        // ACTIONS AND EVENTS...

        @Action(by = "taker")
        @DependsOn("depositTx")
        default void takerBroadcastsDepositTx() {
        }

        @Event(seenBy = "taker")
        String takerSeesDepositTxInMempool();

        @Event(seenBy = "maker")
        String makerSeesDepositTxInMempool();

        @Event(seenBy = "maker")
        Unit makerStartsCountercurrencyPayment();

        @Event(seenBy = "taker")
        Unit takerConfirmsCountercurrencyPayment();

        @Action(by = "taker")
        @DependsOn({"takerConfirmsCountercurrencyPayment", "payoutTx"})
        default void takerBroadcastsPayoutTx() {
        }

        @Event(seenBy = "taker")
        String takerSeesPayoutTxInMempool();

        @Event(seenBy = "maker")
        String makerSeesPayoutTxInMempool();
    }

    public interface Listener {
        void onDepositTxConfirmed();
    }

    private final Wallet wallet;
    private final Chain chain;
    @Setter
    private String depositTx;
    @Setter
    private String payoutSignature;

    protected final Set<MultiSig.Listener> listeners = ConcurrentHashMap.newKeySet();

    public MultiSig(Wallet wallet, Chain chain) {
        this.wallet = wallet;
        this.chain = chain;

        chain.addListener(this);
    }

    @Override
    public Type getType() {
        return Type.ESCROW;
    }

    @Override
    public void onTxConfirmed(String tx) {
        if (tx.equals(depositTx)) {
            listeners.forEach(Listener::onDepositTxConfirmed);
        }
    }

    public MultiSig addListener(MultiSig.Listener listener) {
        listeners.add(listener);
        return this;
    }

    public CompletableFuture<String> getTxInputs() {
        return wallet.getUtxos()
                .thenCompose(this::createPartialDepositTx);
    }

    private CompletableFuture<String> createPartialDepositTx(String utxos) {
        return CompletableFuture.completedFuture("partial deposit tx");
    }

    public CompletableFuture<String> broadcastDepositTx(String txInput) {
        return wallet.getUtxos()
                .thenCompose(utxos -> createDepositTx(txInput, utxos))
                .thenCompose(wallet::sign)
                .whenComplete((depositTx, t) -> this.depositTx = depositTx)
                .thenCompose(chain::broadcast);
    }

    private CompletableFuture<String> createDepositTx(String txInput, String utxos) {
        return CompletableFuture.completedFuture("depositTx");
    }

    public CompletableFuture<String> createPartialPayoutTx() {
        return CompletableFuture.completedFuture("payoutTx");
    }

    public CompletableFuture<String> createPayoutTx(String signature) {
        return CompletableFuture.completedFuture("payoutTx");
    }

    public CompletableFuture<String> getPayoutTxSignature(String payoutTx) {
        return wallet.sign(payoutTx);
    }

    public CompletableFuture<String> broadcastPayoutTx() {
        return createPayoutTx(payoutSignature)
                .thenCompose(wallet::sign)
                .thenCompose(chain::broadcast);
    }

    public CompletableFuture<Boolean> isPayoutTxInMemPool(String payoutTx) {
        return chain.isInMemPool(payoutTx);
    }


    public CompletableFuture<String> verifyTxInputsMessage(TxInputsMessage msg) {
        return CompletableFuture.completedFuture(msg.getTxInput());
    }

    public CompletableFuture<String> verifyDepositTxBroadcastMessage(DepositTxBroadcastMessage msg) {
        return CompletableFuture.completedFuture(msg.getTx());
    }

    public CompletableFuture<String> verifyPayoutTxBroadcastMessage(PayoutTxBroadcastMessage msg) {
        return CompletableFuture.completedFuture(msg.getTx());
    }

    public CompletableFuture<String> verifyFundsSentMessage(FundsSentMessage fundsSentMessage) {
        return CompletableFuture.completedFuture(fundsSentMessage.getSig());
    }
}
