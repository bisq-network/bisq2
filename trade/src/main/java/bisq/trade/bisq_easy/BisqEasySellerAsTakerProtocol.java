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

package bisq.trade.bisq_easy;

import bisq.trade.SellerProtocol;
import bisq.trade.TakerProtocol;
import bisq.trade.bisq_easy.events.BisqEasyAccountDataEvent;
import bisq.trade.bisq_easy.events.BisqEasyAccountDataEventHandler;
import bisq.trade.bisq_easy.events.BisqEasyTakeOfferEvent;
import bisq.trade.bisq_easy.events.BisqEasyTakeOfferEventHandler;

import static bisq.trade.bisq_easy.BisqEasyTradeState.*;

public class BisqEasySellerAsTakerProtocol extends BisqEasyProtocol implements SellerProtocol, TakerProtocol {

    public BisqEasySellerAsTakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    public void configTransitions() {
        buildTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .run(BisqEasyTakeOfferEventHandler.class)
                .to(TAKER_TAKE_OFFER_REQUEST_SENT);

        buildTransition()
                .from(TAKER_TAKE_OFFER_REQUEST_SENT)
                .on(BisqEasyAccountDataEvent.class)
                .run(BisqEasyAccountDataEventHandler.class)
                .to(SELLER_ACCOUNT_DATA_SENT);
    }
}