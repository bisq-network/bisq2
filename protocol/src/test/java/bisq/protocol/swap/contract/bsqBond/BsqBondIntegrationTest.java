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

package bisq.protocol.swap.contract.bsqBond;


import bisq.account.settlement.FiatSettlement;
import bisq.common.monetary.Fiat;
import bisq.contract.SwapContract;
import bisq.identity.Identity;
import bisq.network.NetworkId;
import bisq.offer.SwapOffer;
import bisq.offer.SwapSide;
import bisq.offer.protocol.ProtocolType;
import bisq.offer.protocol.SwapProtocolType;
import bisq.protocol.BaseProtocolTest;
import bisq.protocol.ContractMaker;
import bisq.protocol.ProtocolExecution;
import bisq.protocol.SettlementExecution;
import bisq.protocol.bsqBond.BsqBond;
import bisq.protocol.bsqBond.BsqBondProtocol;
import bisq.protocol.bsqBond.maker.MakerBsqBondProtocol;
import bisq.protocol.bsqBond.taker.TakerBsqBondProtocol;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class BsqBondIntegrationTest extends BaseProtocolTest {
    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    public void testBsqBond() {
        Identity makerIdentity = identityService.getOrCreateIdentity("maker").join();
        NetworkId makerNetworkId = makerIdentity.networkId();
        Identity takerIdentity = identityService.getOrCreateIdentity("taker").join();
        NetworkId takerNetworkId = takerIdentity.networkId();

        SwapSide askSwapSide = new SwapSide(Fiat.of(100, "USD"), List.of(FiatSettlement.ZELLE));
        SwapSide bidSwapSide = new SwapSide(Fiat.of(90, "EUR"), List.of(FiatSettlement.REVOLUT, FiatSettlement.SEPA));
        SwapOffer offer = new SwapOffer(askSwapSide,
                bidSwapSide,
                "USD",
                SwapProtocolType.BSQ_BOND,
                makerNetworkId);

        // taker takes offer and selects first ProtocolType
        ProtocolType protocolType = offer.getProtocolTypes().get(0);
        val askSideSettlement = offer.getAskSwapSide().settlementMethods().get(0);
        val bidSideSettlement = offer.getBidSwapSide().settlementMethods().get(0);
        // Not sure if SettlementExecution stays... if we need more work as it can be for both ask and bid
        SettlementExecution settlementExecution = SettlementExecution.from(askSideSettlement.getType());
        
        SwapContract takerContract = ContractMaker.takerCreatesSwapContract(offer,
                protocolType,
                takerNetworkId,
                askSideSettlement, 
                bidSideSettlement);
        TakerBsqBondProtocol takerBsqBondProtocol = new TakerBsqBondProtocol(networkService,
                makerIdentity.getNodeIdAndKeyPair(),
                takerContract,
                settlementExecution,
                new BsqBond());
        ProtocolExecution takerProtocolExecution = new ProtocolExecution(takerBsqBondProtocol);

        // simulated take offer protocol: Taker sends to maker the protocolType
        SwapContract makerContract = ContractMaker.makerCreatesSwapContract(offer, 
                protocolType, 
                takerNetworkId,
                askSideSettlement,
                bidSideSettlement);
        MakerBsqBondProtocol makerBsqBondProtocol = new MakerBsqBondProtocol(networkService,
                takerIdentity.getNodeIdAndKeyPair(),
                makerContract,
                settlementExecution,
                new BsqBond());
        ProtocolExecution makerProtocolExecution = new ProtocolExecution(makerBsqBondProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerProtocolExecution.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State bsqBondProtocolState) {
                if (bsqBondProtocolState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });
        takerProtocolExecution.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State bsqBondProtocolState) {
                if (bsqBondProtocolState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });

        makerProtocolExecution.start();
        takerProtocolExecution.start();

        try {
            boolean completed = completedLatch.await(30, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
