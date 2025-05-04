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

package bisq.offer.musig;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class MyMuSigOffersStore implements PersistableStore<MyMuSigOffersStore> {
    @Getter(AccessLevel.PACKAGE)
    private final ObservableSet<MuSigOffer> offers = new ObservableSet<>();

    private MyMuSigOffersStore(Set<MuSigOffer> offers) {
        this.offers.addAll(offers);
    }


    @Override
    public MyMuSigOffersStore getClone() {
        return new MyMuSigOffersStore(new HashSet<>(offers));
    }

    @Override
    public void applyPersisted(MyMuSigOffersStore persisted) {
        offers.clear();
        offers.addAll(persisted.getOffers());
    }

    @Override
    public bisq.offer.protobuf.MyMuSigOffersStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.offer.protobuf.MyMuSigOffersStore.newBuilder()
                .addAllOffers(offers.stream()
                        .map(e -> e.toProto(serializeForHash))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.offer.protobuf.MyMuSigOffersStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MyMuSigOffersStore fromProto(bisq.offer.protobuf.MyMuSigOffersStore proto) {
        return new MyMuSigOffersStore(proto.getOffersList().stream().map(MuSigOffer::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.MyMuSigOffersStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    void add(MuSigOffer offer) {
        offers.add(offer);
    }

    void remove(MuSigOffer offer) {
        offers.remove(offer);
    }
}