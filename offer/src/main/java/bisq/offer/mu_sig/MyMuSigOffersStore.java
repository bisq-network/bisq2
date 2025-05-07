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

package bisq.offer.mu_sig;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.offer.Offer;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
final class MyMuSigOffersStore implements PersistableStore<MyMuSigOffersStore> {
    // ObservableSet use concurrent set implementation
    private final ObservableSet<MuSigOffer> offers = new ObservableSet<>();
    private final ObservableSet<String> deactivatedOfferIds = new ObservableSet<>();

    private MyMuSigOffersStore(Set<MuSigOffer> offers, Set<String> deactivatedOfferIds) {
        this.offers.addAll(offers);
        this.deactivatedOfferIds.addAll(deactivatedOfferIds);
    }

    @Override
    public MyMuSigOffersStore getClone() {
        return new MyMuSigOffersStore(new HashSet<>(offers), new HashSet<>(deactivatedOfferIds));
    }

    @Override
    public void applyPersisted(MyMuSigOffersStore persisted) {
        synchronized (this) {
            offers.setAll(persisted.getOffers());
            Set<String> allOfferIds = offers.stream().map(Offer::getId).collect(Collectors.toSet());
            // We filter out orphaned deactivatedOfferIds
            deactivatedOfferIds.setAll(persisted.getDeactivatedOfferIds().stream()
                    .filter(allOfferIds::contains)
                    .collect(Collectors.toSet()));
        }
    }

    @Override
    public bisq.offer.protobuf.MyMuSigOffersStore.Builder getBuilder(boolean serializeForHash) {
        synchronized (this) {
            return bisq.offer.protobuf.MyMuSigOffersStore.newBuilder()
                    .addAllOffers(offers.stream()
                            .map(e -> e.toProto(serializeForHash))
                            .collect(Collectors.toList()))
                    .addAllDeactivatedOfferIds(new ArrayList<>(deactivatedOfferIds));
        }
    }

    @Override
    public bisq.offer.protobuf.MyMuSigOffersStore toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static MyMuSigOffersStore fromProto(bisq.offer.protobuf.MyMuSigOffersStore proto) {
        return new MyMuSigOffersStore(proto.getOffersList().stream().map(MuSigOffer::fromProto).collect(Collectors.toSet()),
                new HashSet<>(proto.getDeactivatedOfferIdsList()));
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

    void addOffer(MuSigOffer offer) {
        offers.add(offer);
    }

    void activateOffer(MuSigOffer offer) {
        deactivatedOfferIds.remove(offer.getId());
    }

    void deactivateOffer(MuSigOffer offer) {
        deactivatedOfferIds.add(offer.getId());
    }

    synchronized void removeOffer(MuSigOffer offer) {
        offers.remove(offer);
        deactivateOffer(offer);
    }

    synchronized Set<MuSigOffer> getActivatedOffers() {
        return offers.stream()
                .filter(offer -> !deactivatedOfferIds.contains(offer.getId()))
                .collect(Collectors.toUnmodifiableSet());
    }

    ReadOnlyObservableSet<MuSigOffer> getOffersAsObservableSet() {
        return offers;
    }

    Set<MuSigOffer> getOffers() {
        return offers.getUnmodifiableSet();
    }

    ReadOnlyObservableSet<String> getDeactivatedOfferIdsAsObservableSet() {
        return deactivatedOfferIds;
    }

    Set<String> getDeactivatedOfferIds() {
        return deactivatedOfferIds.getUnmodifiableSet();
    }
}