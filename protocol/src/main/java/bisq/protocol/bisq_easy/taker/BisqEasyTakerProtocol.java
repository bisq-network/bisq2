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

package bisq.protocol.bisq_easy.taker;

import bisq.common.monetary.Monetary;
import bisq.identity.Identity;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.protocol.bisq_easy.BisqEasyEvent;
import bisq.protocol.bisq_easy.BisqEasyProtocolModel;
import bisq.protocol.bisq_easy.ServiceProvider;
import bisq.protocol.bisq_easy.states.BisqEasyState;
import bisq.protocol.bisq_easy.taker.handlers.TakeOfferHandler;
import bisq.protocol.fsm.FiniteStateMachine;

public interface BisqEasyTakerProtocol<M extends BisqEasyProtocolModel> {
    FiniteStateMachine getFsm();

    M getModel();

    default void configStateMachine() {
        getFsm().transition()
                .from(BisqEasyState.INIT)
                .on(BisqEasyEvent.TAKE_OFFER)
                .to(BisqEasyState.TAKE_OFFER_REQUEST_SENT);

      /*  getFsm().transition()
                .from(BisqEasyTakerState.TAKER_TAKE_OFFER_REQUEST_SENT)
                .on(BisqEasyEventType.TAKER_TAKE_OFFER)
                .to(BisqEasyTakerState.COMPLETE);*/
    }

    default void takeOffer(ServiceProvider serviceProvider,
                           Identity takerIdentity,
                           BisqEasyOffer bisqEasyOffer,
                           Monetary baseSideAmount,
                           Monetary quoteSideAmount,
                           BitcoinPaymentMethodSpec bitcoinPaymentMethodSpec,
                           FiatPaymentMethodSpec fiatPaymentMethodSpec) {
        TakeOfferHandler handler = new TakeOfferHandler(serviceProvider,
                getModel(),
                takerIdentity,
                bisqEasyOffer,
                baseSideAmount,
                quoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec);
        getFsm().onEvent(BisqEasyEvent.TAKE_OFFER, handler);
    }
}
