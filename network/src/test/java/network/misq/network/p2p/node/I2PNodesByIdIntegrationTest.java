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

package network.misq.network.p2p.node;

import lombok.extern.slf4j.Slf4j;
import network.misq.network.p2p.node.transport.Transport;

// FIXME tests fail for unknown reasons (many changes in the code since test was written)
@Slf4j
public class I2PNodesByIdIntegrationTest extends BaseNodesByIdTest {

    // @Test
    void test_messageRoundTrip() throws InterruptedException {
        super.test_messageRoundTrip(getConfig(Transport.Type.I2P));
    }

    // @Test
    void test_initializeServer() throws InterruptedException {
        super.test_initializeServer(getConfig(Transport.Type.I2P));
    }

    @Override
    protected long getTimeout() {
        return numNodes * 30;
    }
}