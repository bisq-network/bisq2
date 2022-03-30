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

package bisq.network.p2p.services.relay;

import bisq.network.p2p.message.NetworkMessage;
import bisq.network.p2p.node.Address;
import bisq.network.p2p.services.confidential.ConfidentialMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;

//todo proto support not impl yet as class itself and use case is not completed
@EqualsAndHashCode(callSuper = true)
@Getter
public class RelayMessage extends ConfidentialMessage {
    private final Address targetAddress;

    public RelayMessage(NetworkMessage networkMessage, Address targetAddress) {
        super(null, null);
        // super(proto, peersPublicKey);
        this.targetAddress = targetAddress;
    }
}
