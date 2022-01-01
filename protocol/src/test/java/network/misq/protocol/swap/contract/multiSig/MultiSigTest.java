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

package network.misq.protocol.swap.contract.multiSig;


import lombok.extern.slf4j.Slf4j;
import network.misq.account.FiatTransfer;
import network.misq.common.monetary.Coin;
import network.misq.common.monetary.Fiat;
import network.misq.contract.AssetTransfer;
import network.misq.contract.ProtocolType;
import network.misq.contract.SwapProtocolType;
import network.misq.contract.TwoPartyContract;
import network.misq.network.NetworkService;
import network.misq.network.p2p.NetworkId;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.transport.Transport;
import network.misq.offer.Asset;
import network.misq.offer.Offer;
import network.misq.protocol.ContractMaker;
import network.misq.protocol.MockServiceTransport;
import network.misq.protocol.ProtocolExecutor;
import network.misq.protocol.multiSig.MultiSig;
import network.misq.protocol.multiSig.MultiSigProtocol;
import network.misq.protocol.multiSig.maker.MakerMultiSigProtocol;
import network.misq.protocol.multiSig.taker.TakerMultiSigProtocol;
import network.misq.security.PubKey;
import network.misq.wallets.Chain;
import network.misq.wallets.Wallet;
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
        networkService = new MockServiceTransport();
    }

    protected abstract Chain getChain();

    protected abstract Wallet getTakerWallet();

    protected abstract Wallet getMakerWallet();

    protected void run() {
        NetworkService networkService = new MockServiceTransport();
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
