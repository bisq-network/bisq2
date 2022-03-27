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

import bisq.common.observable.ObservableSet;
import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
@Slf4j
public class OpenOfferStore implements PersistableStore<OpenOfferStore> {
    @Getter
    private final ObservableSet<OpenOffer> openOffers = new ObservableSet<>();

    public OpenOfferStore() {
    }

    private OpenOfferStore(Set<OpenOffer> openOffers) {
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
        openOffers.add(openOffer);
    }

    public void remove(OpenOffer openOffer) {
        openOffers.remove(openOffer);
    }

    @Override
    public Message toProto() {
        log.error("Not impl yet");
        return null;
    }
}