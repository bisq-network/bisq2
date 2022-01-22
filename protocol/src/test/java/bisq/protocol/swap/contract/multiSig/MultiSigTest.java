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


import bisq.account.settlement.CryptoSettlement;
import bisq.account.settlement.FiatSettlement;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.timer.Scheduler;
import bisq.contract.SwapContract;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.offer.SwapOffer;
import bisq.offer.SwapSide;
import bisq.offer.protocol.SwapProtocolType;
import bisq.protocol.BaseProtocolTest;
import bisq.protocol.ContractMaker;
import bisq.protocol.ProtocolExecution;
import bisq.protocol.SettlementExecution;
import bisq.protocol.multiSig.MultiSig;
import bisq.protocol.multiSig.MultiSigProtocol;
import bisq.protocol.multiSig.maker.MakerMultiSigProtocol;
import bisq.protocol.multiSig.taker.TakerMultiSigProtocol;
import bisq.wallets.Chain;
import bisq.wallets.Wallet;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
        Identity makerIdentity = identityService.getOrCreateIdentity("maker").join();
        NetworkId makerNetworkId = makerIdentity.networkId();
        Identity takerIdentity = identityService.getOrCreateIdentity("taker").join();
        NetworkId takerNetworkId = takerIdentity.networkId();
        // create offer (buy USD)
        SwapSide askSwapSide = new SwapSide(Fiat.of(5000, "USD"), List.of(FiatSettlement.ZELLE));
        SwapSide bidSwapSide = new SwapSide(Coin.asBtc(100000), List.of(CryptoSettlement.MAINNET, CryptoSettlement.L2));
        SwapOffer offer = new SwapOffer(bidSwapSide,
                askSwapSide,
                "USD",
                SwapProtocolType.MULTISIG,
                makerNetworkId);

        // taker takes offer and selects first ProtocolType
        SwapProtocolType protocolType = offer.getProtocolTypes().get(0);
        val askSideSettlement = offer.getAskSwapSide().settlementMethods().get(0);
        val bidSideSettlement = offer.getBidSwapSide().settlementMethods().get(0);
        // Not sure if SettlementExecution stays... if we need more work as it can be for both ask and bid
        SettlementExecution settlementExecution = SettlementExecution.from(askSideSettlement.getType());
        SwapContract takerTrade = ContractMaker.takerCreatesSwapContract(offer,
                protocolType,
                takerNetworkId,
                askSideSettlement,
                bidSideSettlement);
        MultiSig takerMultiSig = new MultiSig(getTakerWallet(), getChain());
        TakerMultiSigProtocol takerMultiSigProtocol = new TakerMultiSigProtocol(networkService,
                makerIdentity.getNodeIdAndKeyPair(),
                takerTrade,
                settlementExecution,
                takerMultiSig);
        ProtocolExecution takerSwapTradeProtocolExecution = new ProtocolExecution(takerMultiSigProtocol);

        // simulated take offer protocol: Taker sends to maker the protocolType
        SwapContract makerTrade = ContractMaker.makerCreatesSwapContract(offer,
                protocolType,
                takerNetworkId,
                askSideSettlement,
                bidSideSettlement);
        MultiSig makerMultiSig = new MultiSig(getMakerWallet(), getChain());
        MakerMultiSigProtocol makerMultiSigProtocol = new MakerMultiSigProtocol(networkService,
                takerIdentity.getNodeIdAndKeyPair(),
                makerTrade,
                settlementExecution,
                makerMultiSig);
        ProtocolExecution makerSwapTradeProtocolExecution = new ProtocolExecution(makerMultiSigProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerSwapTradeProtocolExecution.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.DEPOSIT_TX_BROADCAST_MSG_RECEIVED) {
                    // Simulate deposit confirmation
                    Scheduler.run(makerMultiSigProtocol::onDepositTxConfirmed).after(100);
                } else if (state == MultiSigProtocol.State.START_MANUAL_PAYMENT) {
                    // Simulate user action
                    Scheduler.run(makerMultiSigProtocol::onManualPaymentStarted).after(100);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_VISIBLE_IN_MEM_POOL) {
                    completedLatch.countDown();
                }
            }
        });
        takerSwapTradeProtocolExecution.getProtocol().addListener(state -> {
            if (state instanceof MultiSigProtocol.State) {
                if (state == MultiSigProtocol.State.FUNDS_SENT_MSG_RECEIVED) {
                    // Simulate user action
                    Scheduler.run(takerMultiSigProtocol::onFundsReceived).after(100);
                } else if (state == MultiSigProtocol.State.PAYOUT_TX_BROADCAST_MSG_SENT) {
                    completedLatch.countDown();
                }
            }
        });

        makerSwapTradeProtocolExecution.start();
        takerSwapTradeProtocolExecution.start();


        try {
            boolean completed = completedLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
