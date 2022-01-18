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


import bisq.MockBitcoind;
import bisq.wallets.Chain;
import bisq.wallets.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class MultiSigWithBitcoindIntegrationTest extends MultiSigTest {

    private Chain chain;

    @BeforeEach
    public void setup() {
        super.setup();
        // A shared chain data provider is used to mock broadcasts and mocked block events
        // (e.g. bob broadcasts and both get the confirm event)
        chain = new MockBitcoind();
    }

    protected Chain getChain() {
        return chain; // mock shared between both nodes
    }

    protected Wallet getTakerWallet() {
        return new MockBitcoind();
    }

    protected Wallet getMakerWallet() {
        return new MockBitcoind();
    }

    @Test
    public void testMultiSig() {
        super.run();
    }
}
