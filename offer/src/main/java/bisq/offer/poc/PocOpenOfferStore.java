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

package bisq.offer.poc;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public final class PocOpenOfferStore implements PersistableStore<PocOpenOfferStore> {
    @Getter
    private final ObservableSet<PocOpenOffer> openOffers = new ObservableSet<>();

    public PocOpenOfferStore() {
    }

    private PocOpenOfferStore(Set<PocOpenOffer> openOffers) {
        this.openOffers.addAll(openOffers);
    }


    @Override
    public PocOpenOfferStore getClone() {
        return new PocOpenOfferStore(new HashSet<>(openOffers));
    }

    @Override
    public void applyPersisted(PocOpenOfferStore persisted) {
        openOffers.clear();
        openOffers.addAll(persisted.getOpenOffers());
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return null;
    }

    public void add(PocOpenOffer openOffer) {
        openOffers.add(openOffer);
    }

    public void remove(PocOpenOffer openOffer) {
        openOffers.remove(openOffer);
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    @Override
    public Message toProto(boolean serializeForHash) {
        return null;
    }
}