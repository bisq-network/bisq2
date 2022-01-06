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

package bisq.protocol.swap.contract.multiSig;


import bisq.account.FiatTransfer;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.contract.AssetTransfer;
import bisq.contract.ProtocolType;
import bisq.contract.SwapProtocolType;
import bisq.contract.TwoPartyContract;
import bisq.network.NetworkService;
import bisq.network.p2p.NetworkId;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.node.transport.Transport;
import bisq.offer.Asset;
import bisq.offer.Offer;
import bisq.protocol.ContractMaker;
import bisq.protocol.MockNetworkService;
import bisq.protocol.ProtocolExecutor;
import bisq.protocol.multiSig.MultiSig;
import bisq.protocol.multiSig.MultiSigProtocol;
import bisq.protocol.multiSig.maker.MakerMultiSigProtocol;
import bisq.protocol.multiSig.taker.TakerMultiSigProtocol;
import bisq.security.PubKey;
import bisq.wallets.Chain;
import bisq.wallets.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@Slf4j
public abstract class MultiSigTest {
    private NetworkService networkService;

    @BeforeEach
    public void setup() {
        // We share a network mock to call MessageListeners when sending a msg (e.g. alice send a msg and
        // bob receives the event)
        networkService = new MockNetworkService();
    }

    protected abstract Chain getChain();

    protected abstract Wallet getTakerWallet();

    protected abstract Wallet getMakerWallet();

    protected void run() {
        NetworkService networkService = new MockNetworkService();
        // create offer
        NetworkId makerNetworkId = new NetworkId(Map.of(Transport.Type.CLEAR, Address.localHost(3333)), new PubKey(null, "default"), "default");
        Asset askAsset = new Asset(Fiat.of(5000, "USD"), List.of(FiatTransfer.ZELLE), AssetTransfer.Type.MANUAL);
        Asset bidAsset = new Asset(Coin.asBtc(100000), List.of(), AssetTransfer.Type.MANUAL);
        Offer offer = new Offer(List.of(SwapProtocolType.MULTISIG),
                makerNetworkId, bidAsset, askAsset);

        // taker takes offer and selects first ProtocolType
        ProtocolType selectedProtocolType = offer.getProtocolTypes().get(0);
        TwoPartyContract takerTrade = ContractMaker.createTakerTrade(offer, selectedProtocolType);
        MultiSig takerMultiSig = new MultiSig(getTakerWallet(), getChain());
        TakerMultiSigProtocol takerMultiSigProtocol = new TakerMultiSigProtocol(takerTrade, networkService, takerMultiSig);
        ProtocolExecutor takerSwapTradeProtocolExecutor = new ProtocolExecutor(takerMultiSigProtocol);

        // simulated take offer protocol: Taker sends to maker the selectedProtocolType
        NetworkId takerNetworkId = new NetworkId(Map.of(Transport.Type.CLEAR, Address.localHost(2222)), new PubKey(null, "default"), "default");
        TwoPartyContract makerTrade = ContractMaker.createMakerTrade(takerNetworkId, selectedProtocolType);
        MultiSig makerMultiSig = new MultiSig(getMakerWallet(), getChain());
        MakerMultiSigProtocol makerMultiSigProtocol = new MakerMultiSigProtocol(makerTrade, networkService, makerMultiSig);
        ProtocolExecutor makerSwapTradeProtocolExecutor = new ProtocolExecutor(makerMultiSigProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.START_MANUAL_PAYMENT) {
                    // Simulate user action
                    new Timer("Simulate Bob user action").schedule(new TimerTask() {
                        public void run() {
                            ((MakerMultiSigProtocol) makerSwapTradeProtocolExecutor.getProtocol()).onManualPaymentStarted();
                        }
                    }, 40);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_VISIBLE_IN_MEM_POOL) {
                    completedLatch.countDown();
                }
            }
        });
        takerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.FUNDS_SENT_MSG_RECEIVED) {
                    // Simulate user action
                    new Timer("Simulate Alice user action").schedule(new TimerTask() {
                        public void run() {
                            ((TakerMultiSigProtocol) takerSwapTradeProtocolExecutor.getProtocol()).onFundsReceived();
                        }
                    }, 40);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_BROADCAST_MSG_SENT) {
                    completedLatch.countDown();
                }
            }
        });

        makerSwapTradeProtocolExecutor.start();
        takerSwapTradeProtocolExecutor.start();

        try {
            boolean completed = completedLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
