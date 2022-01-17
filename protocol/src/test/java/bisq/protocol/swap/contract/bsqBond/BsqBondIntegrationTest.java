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


import bisq.account.FiatSettlement;
import bisq.common.monetary.Fiat;
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
import bisq.protocol.bsqBond.BsqBond;
import bisq.protocol.bsqBond.BsqBondProtocol;
import bisq.protocol.bsqBond.maker.MakerBsqBondProtocol;
import bisq.protocol.bsqBond.taker.TakerBsqBondProtocol;
import lombok.extern.slf4j.Slf4j;
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
        SettlementExecution settlementExecution = new SettlementExecution.Automatic();
        Identity makerIdentity = identityService.getOrCreateIdentity("maker").join();
        NetworkId makerNetworkId = makerIdentity.networkId();

        Leg askLeg = new Leg(Fiat.of(100, "USD"), List.of(FiatSettlement.ZELLE));
        Leg bidLeg = new Leg(Fiat.of(90, "EUR"), List.of(FiatSettlement.REVOLUT, FiatSettlement.SEPA));
        SwapOffer offer = new SwapOffer(askLeg,
                bidLeg,
                "USD",
                List.of(SwapProtocolType.REPUTATION, SwapProtocolType.MULTISIG),
                makerNetworkId);

        // taker takes offer and selects first ProtocolType
        ProtocolType protocolType = offer.getProtocolTypes().get(0);
        TwoPartyContract takerContract = ContractMaker.takerCreatesTwoPartyContract(offer, protocolType, settlementExecution);
        TakerBsqBondProtocol takerBsqBondProtocol = new TakerBsqBondProtocol(networkService,
                makerIdentity.getNodeIdAndKeyPair(),
                takerContract,
                new BsqBond());
        ProtocolExecutor takerProtocolExecutor = new ProtocolExecutor(takerBsqBondProtocol);

        // simulated take offer protocol: Taker sends to maker the protocolType
        Identity takerIdentity = identityService.getOrCreateIdentity("taker").join();
        NetworkId takerNetworkId = takerIdentity.networkId();
        TwoPartyContract makerContract = ContractMaker.makerCreatesTwoPartyContract(takerNetworkId, protocolType, settlementExecution);
        MakerBsqBondProtocol makerBsqBondProtocol = new MakerBsqBondProtocol(networkService,
                takerIdentity.getNodeIdAndKeyPair(),
                makerContract,
                new BsqBond());
        ProtocolExecutor makerProtocolExecutor = new ProtocolExecutor(makerBsqBondProtocol);

        CountDownLatch completedLatch = new CountDownLatch(2);
        makerProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State bsqBondProtocolState) {
                if (bsqBondProtocolState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });
        takerProtocolExecutor.getProtocol().addListener(state -> {
            if (state instanceof BsqBondProtocol.State bsqBondProtocolState) {
                if (bsqBondProtocolState == BsqBondProtocol.State.FUNDS_RECEIVED) {
                    completedLatch.countDown();
                }
            }
        });

        makerProtocolExecutor.start();
        takerProtocolExecutor.start();

        try {
            boolean completed = completedLatch.await(30, TimeUnit.SECONDS);
            assertTrue(completed);
        } catch (Throwable e) {
            fail(e.toString());
        }
    }
}
