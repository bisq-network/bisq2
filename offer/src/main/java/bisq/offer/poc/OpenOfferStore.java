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
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class OpenOfferStore implements PersistableStore<OpenOfferStore> {
    @Getter
    private final ObservableSet<OpenOffer> openOffers = new ObservableSet<>();

    public OpenOfferStore() {
    }

    private OpenOfferStore(Set<OpenOffer> openOffers) {
        this.openOffers.addAll(openOffers);
    }

    @Override
    public bisq.offer.protobuf.OpenOfferStore toProto() {
        return bisq.offer.protobuf.OpenOfferStore.newBuilder()
                .addAllOpenOffers(openOffers.stream().map(OpenOffer::toProto).collect(Collectors.toSet()))
                .build();
    }

    public static OpenOfferStore fromProto(bisq.offer.protobuf.OpenOfferStore proto) {
        return new OpenOfferStore(proto.getOpenOffersList().stream().map(OpenOffer::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.OpenOfferStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
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
}