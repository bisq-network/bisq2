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

package bisq.protocol.prototype.bsqBond;

import bisq.contract.TwoPartyContract;
import bisq.network.NetworkIdWithKeyPair;
import bisq.network.NetworkService;
import bisq.network.p2p.services.confidential.MessageListener;
import bisq.protocol.prototype.Protocol;
import bisq.protocol.prototype.SettlementExecution;
import bisq.protocol.prototype.TwoPartyProtocol;

/**
 * Mock protocol for simulating a BSQ bond based protocol.
 * <ol>
 *   <li value="1">Maker commits bond.
 *   <li value="2">Maker sends commitment to Taker.
 *   <li value="4">After Maker has received Taker's commitment she sends her funds.
 *   <li value="6">After Maker has received Taker's funds she has completed.
 * </ol>
 * <p>
 * Taker awaits Maker commitment.
 * <ol>
 *   <li value="3">After Taker has received Maker's commitment he sends his commitment.
 *   <li value="5">After Taker has received Maker's funds he sends his funds. He has completed now.
 * </ol>
 */
public abstract class BsqBondProtocol extends TwoPartyProtocol implements MessageListener {


    public enum State implements Protocol.State {
        START,
        COMMITMENT_SENT,
        COMMITMENT_RECEIVED,
        FUNDS_SENT,
        FUNDS_RECEIVED // Completed
    }

    protected final SettlementExecution settlementExecution;
    protected final BsqBond bsqBond;

    public BsqBondProtocol(NetworkService networkService,
                           NetworkIdWithKeyPair networkIdWithKeyPair,
                           TwoPartyContract contract,
                           SettlementExecution settlementExecution,
                           BsqBond bsqBond) {
        super(networkService, networkIdWithKeyPair, contract);
        this.settlementExecution = settlementExecution;
        this.bsqBond = bsqBond;
    }
}
