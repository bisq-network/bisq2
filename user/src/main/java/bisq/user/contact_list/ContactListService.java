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
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContactListService implements PersistenceClient<ContactListStore>, DataService.Listener, Service {
    @Getter
    private final ContactListStore persistableStore = new ContactListStore();
    @Getter
    private final Persistence<ContactListStore> persistence;
    private final UserProfileService userProfileService;

    public ContactListService(PersistenceService persistenceService, UserProfileService userProfileService) {
        persistence = persistenceService.getOrCreatePersistence(this, DbSubDirectory.SETTINGS, persistableStore);
        this.userProfileService = userProfileService;
    }

    public ReadOnlyObservableSet<ContactListEntry> getContactListEntries() {
        return persistableStore.getContactListEntries();
    }

    public void addContactListEntry(String profileId, ContactReason contactReason) {
        userProfileService.findUserProfile(profileId).ifPresent(userProfile -> {
            persistableStore.addContactListEntry(new ContactListEntry(userProfile, contactReason));
            persist();
        });
    }

    public void addContactListEntry(ContactListEntry contactListEntry) {
        persistableStore.addContactListEntry(contactListEntry);
        persist();
    }

    public void removeContactListEntry(ContactListEntry contactListEntry) {
        persistableStore.removeContactListEntry(contactListEntry);
        persist();
    }
}
