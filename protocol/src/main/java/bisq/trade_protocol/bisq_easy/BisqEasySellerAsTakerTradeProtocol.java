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

package bisq.trade_protocol.bisq_easy;

import bisq.trade_protocol.TradeModel;
import bisq.trade_protocol.TradeProtocol;
import bisq.trade_protocol.api.SellerTradeProtocol;
import bisq.trade_protocol.api.TakerTradeProtocol;
import bisq.trade_protocol.bisq_easy.events.BisqEasyTakeOfferEvent;
import bisq.trade_protocol.bisq_easy.events.BisqEasyTakeOfferEventHandler;

import static bisq.trade_protocol.bisq_easy.BisqEasyState.INIT;
import static bisq.trade_protocol.bisq_easy.BisqEasyState.TAKE_OFFER_REQUEST_SENT;

public class BisqEasySellerAsTakerTradeProtocol<M extends TradeModel<?, ?>> extends TradeProtocol<M> implements SellerTradeProtocol, TakerTradeProtocol {

    public BisqEasySellerAsTakerTradeProtocol(ServiceProvider serviceProvider, M model) {
        super(serviceProvider, model);
    }

    public void configTransition() {
        transitionBuilder()
                .from(INIT)
                .on(BisqEasyTakeOfferEvent.class)
                .handle(BisqEasyTakeOfferEventHandler.class)
                .to(TAKE_OFFER_REQUEST_SENT);
    }
}