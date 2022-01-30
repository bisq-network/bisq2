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

package bisq.offer;

import bisq.persistence.PersistableStore;
import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OpenOfferStore implements PersistableStore<OpenOfferStore> {
    @Getter
    private final List<OpenOffer> openOffers = new CopyOnWriteArrayList<>();

    public OpenOfferStore() {
    }

    private OpenOfferStore(List<OpenOffer> openOffers) {
        this.openOffers.addAll(openOffers);
    }

    @Override
    public OpenOfferStore getClone() {
        return new OpenOfferStore(openOffers);
    }

    @Override
    public void applyPersisted(OpenOfferStore persisted) {
        openOffers.clear();
        openOffers.addAll(persisted.getOpenOffers());
    }

    public void add(OpenOffer openOffer) {
        if (openOffers.contains(openOffer)) return;

        openOffers.add(openOffer);
    }

    public void remove(OpenOffer openOffer) {
        if (!openOffers.contains(openOffer)) return;

        openOffers.remove(openOffer);
    }
}