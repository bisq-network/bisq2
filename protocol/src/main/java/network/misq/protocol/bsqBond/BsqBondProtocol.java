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

package network.misq.protocol.bsqBond;

import network.misq.contract.AssetTransfer;
import network.misq.contract.TwoPartyContract;
import network.misq.network.NetworkService;
import network.misq.network.p2p.node.Node;
import network.misq.protocol.Protocol;
import network.misq.protocol.SecurityProvider;
import network.misq.protocol.TwoPartyProtocol;

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
public abstract class BsqBondProtocol extends TwoPartyProtocol implements Node.Listener {

    public enum State implements Protocol.State {
        START,
        COMMITMENT_SENT,
        COMMITMENT_RECEIVED,
        FUNDS_SENT,
        FUNDS_RECEIVED // Completed
    }

    protected final AssetTransfer transport;
    protected final BsqBond security;

    public BsqBondProtocol(TwoPartyContract contract, NetworkService networkService, AssetTransfer transfer, SecurityProvider securityProvider) {
        super(contract, networkService);
        this.transport = transfer;
        this.security = (BsqBond) securityProvider;
    }
}
