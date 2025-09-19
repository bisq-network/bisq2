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

import bisq.common.application.Service;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.network.p2p.services.data.DataService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceClient;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ContactListService implements PersistenceClient<ContactListStore>, DataService.Listener, Service {
    @Getter
    private final ContactListStore persistableStore = new ContactListStore();
    @Getter
    private final Persistence<ContactListStore> persistence;
    @Getter
    private final Map<String, Set<String>> nymsByNickName = new ConcurrentHashMap<>();

    public ContactListService(PersistenceService persistenceService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
    }

    @Override
    public void onPersistedApplied(ContactListStore persisted) {
        persisted.getContactListEntries().forEach(contactListEntry -> {
            nymsByNickName.computeIfAbsent(contactListEntry.getUserProfile().getNickName(), k -> new HashSet<>())
                    .add(contactListEntry.getUserProfile().getNym());
        });
    }

    public ReadOnlyObservableSet<ContactListEntry> getContactListEntries() {
        return persistableStore.getContactListEntries();
    }

    public boolean addContactListEntry(UserProfile userProfile, ContactReason contactReason) {
        return addContactListEntry(new ContactListEntry(userProfile, contactReason));
    }

    public boolean addContactListEntry(ContactListEntry contactListEntry) {
        boolean wasAdded = persistableStore.addContactListEntry(contactListEntry);
        if (wasAdded) {
            nymsByNickName.computeIfAbsent(contactListEntry.getUserProfile().getNickName(), k -> new HashSet<>())
                    .add(contactListEntry.getUserProfile().getNym());
            persist();
        }
        return wasAdded;
    }

    public boolean removeContactListEntry(ContactListEntry contactListEntry) {
        boolean wasRemoved = persistableStore.removeContactListEntry(contactListEntry);
        if (wasRemoved) {
            Optional.ofNullable(nymsByNickName.get(contactListEntry.getUserProfile().getNickName()))
                    .ifPresent(set -> set.remove(contactListEntry.getUserProfile().getNym()));
            persist();
        }
        return wasRemoved;
    }

    public boolean isUserInContactList(UserProfile userProfile) {
        return persistableStore.getContactListEntries().stream()
                .anyMatch(contactListEntry -> contactListEntry.getUserProfile().equals(userProfile));
    }

    public Optional<ContactListEntry> findContactListEntry(UserProfile userProfile) {
        return persistableStore.getContactListEntries().stream()
                .filter(contactListEntry -> contactListEntry.getUserProfile().equals(userProfile))
                .findAny();
    }
}
