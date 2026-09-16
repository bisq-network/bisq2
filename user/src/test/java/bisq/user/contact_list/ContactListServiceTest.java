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

import bisq.persistence.Persistence;
import bisq.persistence.PersistenceService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class ContactListServiceTest {
    private ContactListService service;
    private UserProfile peer;
    private UserProfile me;

    @BeforeEach
    void setUp() {
        PersistenceService persistenceService = mock(PersistenceService.class);
        Persistence persistence = mock(Persistence.class);
        when(persistence.persistAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        // Read by the rate limiter's JVM shutdown hook; a null store path NPEs there at JVM exit.
        when(persistence.getStorePath()).thenReturn(Path.of("test-store"));
        when(persistenceService.getOrCreatePersistence(any(), any(), any())).thenReturn(persistence);
        service = new ContactListService(persistenceService);

        peer = mockUserProfile("peer-1", "peer");
        me = mockUserProfile("me-1", "me");
    }

    /**
     * Regression for the mutable-hash bug: annotations used to be part of equals/hashCode, so an
     * edit changed an entry's hash inside the hash-backed set and remove() missed it — the contact
     * survived a delete that reported changed=false (until a restart rebuilt the set).
     */
    @Test
    void removeStillFindsTheEntryAfterAnAnnotationEdit() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();

        service.setTag(entry, "friend");

        assertTrue(service.removeContactListEntry(entry));
        assertTrue(service.getContactListEntries().isEmpty());
    }

    /** One Save is one edit: a multi-field update must persist and notify once, not per field. */
    @Test
    void batchUpdateEmitsASingleEditNotification() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();
        long editsBefore = service.getNumContactListEntryEdits().get();

        assertTrue(service.updateAnnotations(entry, "friend", "met at conf", 0.7));

        assertEquals(editsBefore + 1, service.getNumContactListEntryEdits().get());
        assertEquals(Optional.of("friend"), entry.getTag());
        assertEquals(Optional.of("met at conf"), entry.getNotes());
        assertEquals(Optional.of(0.7), entry.getTrustScore());
    }

    /** Null means "leave unchanged", never "clear". */
    @Test
    void nullFieldsAreLeftUntouched() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();
        service.updateAnnotations(entry, "friend", "note", 0.5);

        assertTrue(service.updateAnnotations(entry, null, null, 0.9));

        assertEquals(Optional.of("friend"), entry.getTag());
        assertEquals(Optional.of("note"), entry.getNotes());
        assertEquals(Optional.of(0.9), entry.getTrustScore());
    }

    /** An invalid field refuses the whole batch — a Save must not half-apply. */
    @Test
    void anInvalidFieldRejectsTheWholeBatch() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();
        String tooLongNotes = "x".repeat(ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH + 1);

        assertFalse(service.updateAnnotations(entry, "friend", tooLongNotes, 0.5));

        assertEquals(Optional.empty(), entry.getTag());
        assertEquals(Optional.empty(), entry.getNotes());
        assertEquals(Optional.empty(), entry.getTrustScore());
    }

    /** NaN passes plain range comparisons (both are false), so the guard must be isFinite. */
    @Test
    void nonFiniteTrustScoresAreRejected() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();

        assertFalse(service.updateAnnotations(entry, null, null, Double.NaN));
        assertFalse(service.updateAnnotations(entry, null, null, Double.POSITIVE_INFINITY));
        assertFalse(service.updateAnnotations(entry, null, null, Double.NEGATIVE_INFINITY));

        assertEquals(Optional.empty(), entry.getTrustScore());
    }

    /** Same values applied again must not tick the edit counter (no phantom pushes). */
    @Test
    void applyingUnchangedValuesIsANoOp() {
        service.addContactListEntry(peer, me, ContactReason.MANUALLY_ADDED);
        ContactListEntry entry = service.findContactListEntry("peer-1").orElseThrow();
        service.updateAnnotations(entry, "friend", null, null);
        long editsBefore = service.getNumContactListEntryEdits().get();

        assertFalse(service.updateAnnotations(entry, "friend", null, null));

        assertEquals(editsBefore, service.getNumContactListEntryEdits().get());
    }

    private UserProfile mockUserProfile(String id, String nickName) {
        UserProfile profile = mock(UserProfile.class);
        when(profile.getId()).thenReturn(id);
        when(profile.getNickName()).thenReturn(nickName);
        when(profile.getNym()).thenReturn("nym-" + id);
        return profile;
    }
}
