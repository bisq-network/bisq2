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

package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.protocol.events.TradeEventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BisqMuSigT1ExpiredEventHandler extends TradeEventHandler<BisqMuSigTrade> {

    public BisqMuSigT1ExpiredEventHandler(ServiceProvider serviceProvider, BisqMuSigTrade trade) {
        super(serviceProvider, trade);
    }

    @Override
    public void handle(Event event) {
        if (!(event instanceof BisqMuSigT1ExpiredEvent expiredEvent)) {
            return;
        }

        log.info("T1 timelock expired for trade {}: timestamp={}",
                trade.getId(), expiredEvent.getExpiryTimestamp());

        //...
    }
}