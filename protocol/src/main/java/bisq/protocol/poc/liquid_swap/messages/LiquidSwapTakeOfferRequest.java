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

package bisq.protocol.poc.liquid_swap.messages;

import bisq.contract.poc.PocContract;
import bisq.network.protobuf.NetworkMessage;
import bisq.protocol.poc.messages.TakeOfferRequest;
import lombok.Getter;

@Getter
public class LiquidSwapTakeOfferRequest extends TakeOfferRequest {

    private final PocContract contract;

    public LiquidSwapTakeOfferRequest(PocContract contract) {
        super(contract.getOffer().getId());
        this.contract = contract;
    }

    @Override
    public NetworkMessage toProto() {
        //todo
        return null;
    }
}