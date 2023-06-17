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

import bisq.trade.BuyerProtocol;
import bisq.trade.TakerProtocol;
import bisq.trade.bisq_easy.events.BisqEasyTakeOfferEvent;
import bisq.trade.bisq_easy.events.BisqEasyTakeOfferEventHandler;
import bisq.trade.bisq_easy.messages.BisqEasyAccountDataMessage;
import bisq.trade.bisq_easy.messages.BisqEasyAccountDataMessageHandler;

import static bisq.trade.bisq_easy.BisqEasyTradeState.*;

public class BisqEasyBuyerAsTakerProtocol extends BisqEasyProtocol implements BuyerProtocol, TakerProtocol {

    public BisqEasyBuyerAsTakerProtocol(ServiceProvider serviceProvider, BisqEasyTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void configTransitions() {
        buildTransition()
                .from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .run(BisqEasyTakeOfferEventHandler.class)
                .to(TAKER_TAKE_OFFER_REQUEST_SENT);

        buildTransition()
                .from(TAKER_TAKE_OFFER_REQUEST_SENT)
                .on(BisqEasyAccountDataMessage.class)
                .run(BisqEasyAccountDataMessageHandler.class)
                .to(BUYER_ACCOUNT_DATA_RECEIVED);
    }
}