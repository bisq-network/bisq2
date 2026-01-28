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

package bisq.user.contact_list;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Slf4j
public final class ContactListStore implements PersistableStore<ContactListStore> {
    private final ObservableSet<ContactListEntry> contactListEntries = new ObservableSet<>();

    private ContactListStore(Set<ContactListEntry> contactListEntries) {
        this.contactListEntries.setAll(contactListEntries);
    }

    @Override
    public synchronized bisq.user.protobuf.ContactListStore.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.ContactListStore.newBuilder()
                .addAllContactListEntries(contactListEntries.stream()
                        .sorted(Comparator.comparing(e -> e.getUserProfile().getId()))
                        .map(e -> e.toProto(serializeForHash))
                        .toList());
    }

    @Override
    public bisq.user.protobuf.ContactListStore toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static ContactListStore fromProto(bisq.user.protobuf.ContactListStore proto) {
        return new ContactListStore(new HashSet<>(proto.getContactListEntriesList().stream()
                .map(ContactListEntry::fromProto).
                collect(Collectors.toSet())));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.ContactListStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public synchronized ContactListStore getClone() {
        return new ContactListStore(Set.copyOf(contactListEntries));
    }

    @Override
    public synchronized void applyPersisted(ContactListStore persisted) {
        contactListEntries.setAll(persisted.getContactListEntries());
    }

    synchronized boolean addContactListEntry(ContactListEntry contactListEntry) {
        boolean didNotExist = contactListEntries.stream()
                .noneMatch(e -> e.getUserProfile().getId().equals(contactListEntry.getUserProfile().getId()));
        if (didNotExist) {
            contactListEntries.add(contactListEntry);
        }
        return didNotExist;
    }

    synchronized boolean removeContactListEntry(ContactListEntry contactListEntry) {
        return contactListEntries.remove(contactListEntry);
    }

    ReadOnlyObservableSet<ContactListEntry> getContactListEntries() {
        return contactListEntries;
    }
}