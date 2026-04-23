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

package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.offer.Direction;
import bisq.offer.mu_sig.MuSigOffer;

public class TakeOfferDraft extends ReadOnlyOfferDraft {

    private MuSigOffer offer;

    TakeOfferDraft() {
    }

    void setOffer(MuSigOffer offer) {
        this.offer = offer;
    }

    MuSigOffer getOffer() {
        return offer;
    }

    @Override
    public Market getMarket() {
        return offer.getMarket();
    }

    @Override
    public Direction getDirection() {
        return offer.getDirection();
    }
}
