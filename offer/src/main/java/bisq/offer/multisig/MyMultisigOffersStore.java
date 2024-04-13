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

package bisq.offer.multisig;

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
public final class MyMultisigOffersStore implements PersistableStore<MyMultisigOffersStore> {
    @Getter
    private final ObservableSet<MultisigOffer> offers = new ObservableSet<>();

    public MyMultisigOffersStore() {
    }

    private MyMultisigOffersStore(Set<MultisigOffer> offers) {
        this.offers.addAll(offers);
    }


    @Override
    public MyMultisigOffersStore getClone() {
        return new MyMultisigOffersStore(new HashSet<>(offers));
    }

    @Override
    public void applyPersisted(MyMultisigOffersStore persisted) {
        offers.clear();
        offers.addAll(persisted.getOffers());
    }

    @Override
    public bisq.offer.protobuf.MyMultisigOffersStore.Builder getBuilder(boolean ignoreAnnotation) {
        return bisq.offer.protobuf.MyMultisigOffersStore.newBuilder()
                .addAllOffers(offers.stream()
                        .map(e -> e.toProto(ignoreAnnotation))
                        .collect(Collectors.toList()));
    }

    @Override
    public bisq.offer.protobuf.MyMultisigOffersStore toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    public static MyMultisigOffersStore fromProto(bisq.offer.protobuf.MyMultisigOffersStore proto) {
        return new MyMultisigOffersStore(proto.getOffersList().stream().map(MultisigOffer::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.MyMultisigOffersStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public void add(MultisigOffer offer) {
        offers.add(offer);
    }

    public void remove(MultisigOffer offer) {
        offers.remove(offer);
    }
}