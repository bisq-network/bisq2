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


import bisq.account.FiatSettlement;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.timer.Scheduler;
import bisq.contract.ProtocolType;
import bisq.contract.SettlementExecution;
import bisq.contract.SwapProtocolType;
import bisq.contract.TwoPartyContract;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.offer.Leg;
import bisq.offer.SwapOffer;
import bisq.protocol.BaseProtocolTest;
import bisq.protocol.ContractMaker;
import bisq.protocol.ProtocolExecutor;
import bisq.protocol.multiSig.MultiSig;
import bisq.protocol.multiSig.MultiSigProtocol;
import bisq.protocol.multiSig.maker.MakerMultiSigProtocol;
import bisq.protocol.multiSig.taker.TakerMultiSigProtocol;
import bisq.wallets.Chain;
import bisq.wallets.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


@Slf4j
public abstract class MultiSigTest extends BaseProtocolTest {

    @BeforeEach
    public void setup() {
        super.setup();
    }

    protected abstract Chain getChain();

    protected abstract Wallet getTakerWallet();

    protected abstract Wallet getMakerWallet();

    protected void run() {
        //todo works yet with Automatic but for manual its missing some steps
        SettlementExecution settlementExecution = new SettlementExecution.Automatic();
        
        Identity makerIdentity = identityService.getOrCreateIdentity("maker").join();
        NetworkId makerNetworkId = makerIdentity.networkId();
        // create offer
        Leg askLeg = new Leg(Fiat.of(5000, "USD"), List.of(FiatSettlement.ZELLE));
        Leg bidLeg = new Leg(Coin.asBtc(100000), List.of());
        SwapOffer offer = new SwapOffer(bidLeg,
                askLeg,
                "USD",
                SwapProtocolType.MULTISIG,
                makerNetworkId);

        // taker takes offer and selects first ProtocolType
        ProtocolType protocolType = offer.getProtocolTypes().get(0);
        TwoPartyContract takerTrade = ContractMaker.takerCreatesTwoPartyContract(offer, protocolType, settlementExecution);
        MultiSig takerMultiSig = new MultiSig(getTakerWallet(), getChain());
        TakerMultiSigProtocol takerMultiSigProtocol = new TakerMultiSigProtocol(networkService,
                makerIdentity.getNodeIdAndKeyPair(),
                takerTrade,
                settlementExecution,
                takerMultiSig);
        ProtocolExecutor takerSwapTradeProtocolExecutor = new ProtocolExecutor(takerMultiSigProtocol);

        // simulated take offer protocol: Taker sends to maker the protocolType
        Identity takerIdentity = identityService.getOrCreateIdentity("taker").join();
        NetworkId takerNetworkId = takerIdentity.networkId();
        TwoPartyContract makerTrade = ContractMaker.makerCreatesTwoPartyContract(takerNetworkId, protocolType, settlementExecution);
        MultiSig makerMultiSig = new MultiSig(getMakerWallet(), getChain());
        MakerMultiSigProtocol makerMultiSigProtocol = new MakerMultiSigProtocol(networkService,
                takerIdentity.getNodeIdAndKeyPair(),
                makerTrade,
                settlementExecution,
                makerMultiSig);
        ProtocolExecutor makerSwapTradeProtocolExecutor = new ProtocolExecutor(makerMultiSigProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.DEPOSIT_TX_BROADCAST_MSG_RECEIVED) {
                    // Simulate deposit confirmation
                    Scheduler.run(makerMultiSigProtocol::onDepositTxConfirmed).after(100);
                } else  if (state == MultiSigProtocol.State.START_MANUAL_PAYMENT) {
                    // Simulate user action
                    Scheduler.run(makerMultiSigProtocol::onManualPaymentStarted).after(100);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_VISIBLE_IN_MEM_POOL) {
                    completedLatch.countDown();
                }
            }
        });
        takerSwapTradeProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.FUNDS_SENT_MSG_RECEIVED) {
                    // Simulate user action
                    Scheduler.run(takerMultiSigProtocol::onFundsReceived).after(100);
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
