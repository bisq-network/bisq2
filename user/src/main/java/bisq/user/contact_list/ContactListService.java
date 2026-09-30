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
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ReadOnlyObservableSet;
import bisq.network.p2p.services.data.DataService;
import bisq.persistence.DbSubDirectory;
import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.persistence.RateLimitedPersistenceClient;
import bisq.user.profile.UserProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ContactListService extends RateLimitedPersistenceClient<ContactListStore> implements Service, DataService.Listener {
    public final static int CONTACT_LIST_ENTRY_MAX_TAG_LENGTH = 30;
    public final static int CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH = 600;
    public final static double CONTACT_LIST_ENTRY_MIN_TRUST_SCORE = 0;
    public final static double CONTACT_LIST_ENTRY_MAX_TRUST_SCORE = 1;

    @Getter
    private final ContactListStore persistableStore = new ContactListStore();
    @Getter
    private final Persistence<ContactListStore> persistence;
    @Getter
    private final Map<String, Set<String>> nymsByNickName = new ConcurrentHashMap<>();
    // Tag/notes/trustScore edits mutate an entry in place, so the observable set never fires for
    // them; observers that need edits too (the API push) watch this counter alongside the set.
    @Getter
    private final Observable<Long> numContactListEntryEdits = new Observable<>(0L);

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

    public boolean addContactListEntry(UserProfile userProfile, UserProfile myUserProfile, ContactReason contactReason) {
        return addContactListEntry(new ContactListEntry(userProfile, myUserProfile, contactReason));
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
                .anyMatch(contactListEntry -> contactListEntry.getUserProfile().getId().equals(userProfile.getId()));
    }

    public Optional<ContactListEntry> findContactListEntry(String userProfileId) {
        return persistableStore.getContactListEntries().stream()
                .filter(contactListEntry -> contactListEntry.getUserProfile().getId().equals(userProfileId))
                .findAny();
    }

    public Optional<ContactListEntry> findContactListEntry(UserProfile userProfile) {
        return persistableStore.getContactListEntries().stream()
                .filter(contactListEntry -> contactListEntry.getUserProfile().getId().equals(userProfile.getId()))
                .findAny();
    }

    public void setTag(ContactListEntry contactListEntry, String newTag) {
        updateAnnotations(contactListEntry, newTag, null, null);
    }

    public void setNotes(ContactListEntry contactListEntry, String newNotes) {
        updateAnnotations(contactListEntry, null, newNotes, null);
    }

    public void setTrustScore(ContactListEntry contactListEntry, Double newTrustScore) {
        updateAnnotations(contactListEntry, null, null, newTrustScore);
    }

    /**
     * Applies all provided annotations as one edit: one persist request and one edit notification.
     * Per-field application persisted and notified per field, so a multi-field save could hit the
     * persistence rate limit with only some fields captured by the async snapshot, and subscribers
     * saw intermediate states. Null means "leave unchanged"; if any provided value is invalid,
     * nothing is applied. Returns whether anything changed.
     * <p>
     * Synchronized because every annotation mutation funnels through here: without it, two
     * concurrent edits of the same entry both read the old values and the loser's write is
     * silently dropped.
     */
    public synchronized boolean updateAnnotations(ContactListEntry contactListEntry,
                                     @Nullable String newTag,
                                     @Nullable String newNotes,
                                     @Nullable Double newTrustScore) {
        if (newTag != null && newTag.length() > CONTACT_LIST_ENTRY_MAX_TAG_LENGTH) {
            return false;
        }
        if (newNotes != null && newNotes.length() > CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH) {
            return false;
        }
        // isFinite: NaN passes plain range checks (both comparisons are false) and would poison
        // the persisted store and the JSON snapshot, where it serializes as a string.
        if (newTrustScore != null
                && (!Double.isFinite(newTrustScore)
                || newTrustScore < CONTACT_LIST_ENTRY_MIN_TRUST_SCORE
                || newTrustScore > CONTACT_LIST_ENTRY_MAX_TRUST_SCORE)) {
            return false;
        }

        return findContactListEntry(contactListEntry).map(cle -> {
            boolean changed = false;
            if (newTag != null && !cle.getTag().equals(Optional.of(newTag))) {
                cle.setTag(newTag);
                changed = true;
            }
            if (newNotes != null && !cle.getNotes().equals(Optional.of(newNotes))) {
                cle.setNotes(newNotes);
                changed = true;
            }
            if (newTrustScore != null && !cle.getTrustScore().equals(Optional.of(newTrustScore))) {
                cle.setTrustScore(newTrustScore);
                changed = true;
            }
            if (changed) {
                persist();
                notifyContactListEntryEdited();
            }
            return changed;
        }).orElse(false);
    }

    // Synchronized: two concurrent edits doing a plain read-modify-write could write the same
    // value, and Observable.set drops an equal value without notifying — silently losing the
    // second edit's change notification.
    private synchronized void notifyContactListEntryEdited() {
        numContactListEntryEdits.set(numContactListEntryEdits.get() + 1);
    }

    private Optional<ContactListEntry> findContactListEntry(ContactListEntry contactListEntry) {
        return persistableStore.getContactListEntries().stream()
                .filter(cle -> cle.equals(contactListEntry))
                .findFirst();
    }
}
