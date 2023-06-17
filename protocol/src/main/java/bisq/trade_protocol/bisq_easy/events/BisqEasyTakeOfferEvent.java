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

package bisq.trade_protocol.bisq_easy.events;

import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.identity.Identity;
import bisq.trade_protocol.TradeProtocolEvent;
import lombok.Getter;

@Getter
public class BisqEasyTakeOfferEvent extends TradeProtocolEvent {
    private final Identity takerIdentity;
    private final BisqEasyContract bisqEasyContract;

    public BisqEasyTakeOfferEvent(Identity takerIdentity, BisqEasyContract bisqEasyContract) {
        super();
        this.takerIdentity = takerIdentity;
        this.bisqEasyContract = bisqEasyContract;
    }
}