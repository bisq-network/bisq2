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

package bisq.offer.bisq_musig;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class MyBisqMuSigOffersStore implements PersistableStore<MyBisqMuSigOffersStore> {
    @Getter
    private final ObservableSet<BisqMuSigOffer> offers = new ObservableSet<>();

    public MyBisqMuSigOffersStore() {
    }

    private MyBisqMuSigOffersStore(Set<BisqMuSigOffer> offers) {
        this.offers.addAll(offers);
    }


    @Override
    public MyBisqMuSigOffersStore getClone() {
        return new MyBisqMuSigOffersStore(new HashSet<>(offers));
    }

    @Override
    public void applyPersisted(MyBisqMuSigOffersStore persisted) {
        offers.clear();
        offers.addAll(persisted.getOffers());
    }

    @Override
    public bisq.offer.protobuf.MyBisqMuSigOffersStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.MyBisqMuSigOffersStore.newBuilder()
                .addAllOffers(offers.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.offer.protobuf.MyBisqMuSigOffersStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MyBisqMuSigOffersStore fromProto(bisq.offer.protobuf.MyBisqMuSigOffersStore proto) {
        return new MyBisqMuSigOffersStore(proto.getOffersList().stream().map(BisqMuSigOffer::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.MyBisqMuSigOffersStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public void add(BisqMuSigOffer offer) {
        offers.add(offer);
    }

    public void remove(BisqMuSigOffer offer) {
        offers.remove(offer);
    }
}