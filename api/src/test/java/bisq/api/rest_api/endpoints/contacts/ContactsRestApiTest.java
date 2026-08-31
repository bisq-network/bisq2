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

package bisq.api.rest_api.endpoints.contacts;

import bisq.api.dto.user.contact_list.ContactReasonDto;
import bisq.user.UserService;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.contact_list.ContactReason;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactsRestApiTest {
    private static final String PROFILE_ID = "profile-1";

    private ContactListService contactListService;
    private UserProfileService userProfileService;
    private UserIdentityService userIdentityService;
    private ContactsRestApi restApi;

    @BeforeEach
    void setUp() {
        contactListService = mock(ContactListService.class);
        userProfileService = mock(UserProfileService.class);
        userIdentityService = mock(UserIdentityService.class, RETURNS_DEEP_STUBS);
        UserService userService = mock(UserService.class);
        when(userService.getContactListService()).thenReturn(contactListService);
        when(userService.getUserProfileService()).thenReturn(userProfileService);
        when(userService.getUserIdentityService()).thenReturn(userIdentityService);
        when(userIdentityService.findUserIdentity(PROFILE_ID)).thenReturn(Optional.empty());
        // POST consults the contact list before the profile store; absent unless a test says otherwise.
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.empty());

        restApi = new ContactsRestApi(userService);
    }

    @Test
    void addingAnOwnIdentityIsRejectedAsBadRequest() {
        when(userIdentityService.findUserIdentity(PROFILE_ID)).thenReturn(Optional.of(mock(UserIdentity.class)));

        Response response = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(contactListService, never()).addContactListEntry(any(), any(), any());
    }

    /** Jersey hands back null for an absent entity, so the endpoint must answer 400, not NPE into a 500. */
    @Test
    void addingWithoutABodyOrReasonIsRejectedAsBadRequest() {
        assertThat(restApi.addContact(PROFILE_ID, null).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(restApi.addContact(PROFILE_ID, new AddContactRequest(null)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(contactListService, never()).addContactListEntry(any(), any(), any());
    }

    @Test
    void addingAnUnknownProfileIsNotFound() {
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.empty());

        Response response = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    /** Created vs already-present rides the status code too: 201 for a new contact, 200 for a re-add. */
    @Test
    void addAnswers201OnCreationAnd200WithChangedFalseOnAReAdd() {
        UserProfile peer = mock(UserProfile.class);
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.of(peer));
        when(contactListService.addContactListEntry(eq(peer), any(), eq(ContactReason.MANUALLY_ADDED))).thenReturn(true);

        Response first = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        assertThat(first.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
        assertThat(((AddContactResponse) first.getEntity()).changed()).isTrue();

        ContactListEntry stored = new ContactListEntry(mockMappableProfile(), mockMappableProfile(), 1000L,
                ContactReason.MANUALLY_ADDED, Optional.empty(), Optional.empty(), Optional.empty());
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.of(stored));

        Response second = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        // Idempotent add: the desired state already held — reported, never an error.
        assertThat(second.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AddContactResponse entity = (AddContactResponse) second.getEntity();
        assertThat(entity.changed()).isFalse();
        assertThat(entity.entry()).isNotNull();
        verify(contactListService, times(1)).addContactListEntry(any(), any(), any());
    }

    /**
     * Profiles expire from the network store while the contact list keeps its embedded copy. The
     * existing-contact check must come first, or an idempotent re-add of such a contact turns into
     * a 404 that contradicts the documented changed=false contract.
     */
    @Test
    void reAddingAContactWhoseProfileExpiredAnswersChangedFalseNotNotFound() {
        ContactListEntry stored = new ContactListEntry(mockMappableProfile(), mockMappableProfile(), 1000L,
                ContactReason.MANUALLY_ADDED, Optional.empty(), Optional.empty(), Optional.empty());
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.of(stored));
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.empty());

        Response response = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        AddContactResponse entity = (AddContactResponse) response.getEntity();
        assertThat(entity.changed()).isFalse();
        assertThat(entity.entry()).isNotNull();
        verify(contactListService, never()).addContactListEntry(any(), any(), any());
    }

    /** A concurrent add can win between the existence check and the insert; that is a 200, not a 201. */
    @Test
    void addLosingARaceToAConcurrentAddAnswers200() {
        UserProfile peer = mock(UserProfile.class);
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.of(peer));
        when(contactListService.addContactListEntry(eq(peer), any(), eq(ContactReason.MANUALLY_ADDED))).thenReturn(false);

        Response response = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(((AddContactResponse) response.getEntity()).changed()).isFalse();
    }

    /**
     * The response must carry the stored entry: mobile clients render the add from it, because the
     * subscription push can take seconds over Tor — waiting on it made a successful add look like
     * it did nothing.
     */
    @Test
    void addReturnsTheStoredEntrySoClientsRenderItWithoutWaitingForThePush() {
        UserProfile peer = mock(UserProfile.class);
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.of(peer));
        when(contactListService.addContactListEntry(eq(peer), any(), eq(ContactReason.MANUALLY_ADDED))).thenReturn(true);
        ContactListEntry stored = new ContactListEntry(mockMappableProfile(), mockMappableProfile(), 1000L,
                ContactReason.MANUALLY_ADDED, Optional.empty(), Optional.empty(), Optional.empty());
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.of(stored));

        Response response = restApi.addContact(PROFILE_ID, new AddContactRequest(ContactReasonDto.MANUALLY_ADDED));

        AddContactResponse entity = (AddContactResponse) response.getEntity();
        assertThat(entity.entry()).isNotNull();
        assertThat(entity.entry().date()).isEqualTo(1000L);
    }

    /** A contact whose profile expired from the network store must still be removable. */
    @Test
    void removeResolvesFromTheContactListNotTheProfileStore() {
        ContactListEntry entry = mock(ContactListEntry.class);
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.of(entry));
        when(contactListService.removeContactListEntry(entry)).thenReturn(true);
        when(userProfileService.findUserProfile(PROFILE_ID)).thenReturn(Optional.empty());

        Response response = restApi.removeContact(PROFILE_ID);

        assertThat(((ContactMutationResponse) response.getEntity()).changed()).isTrue();
    }

    @Test
    void removingANonContactAnswersChangedFalse() {
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.empty());

        Response response = restApi.removeContact(PROFILE_ID);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(((ContactMutationResponse) response.getEntity()).changed()).isFalse();
    }

    /**
     * The domain setters silently ignore invalid input, which over an API would read as success —
     * so the endpoint is the place that has to answer 400. Refused atomically: a Save with one
     * invalid field must not half-apply the others.
     */
    @Test
    void annotationValuesAreValidatedHereBecauseTheDomainSettersIgnoreInvalidInputSilently() {
        String tooLongTag = "x".repeat(ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH + 1);
        String tooLongNotes = "x".repeat(ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH + 1);

        assertThat(restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest(tooLongTag, null, null)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest("ok", tooLongNotes, 0.5)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest(null, null, 1.5)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(contactListService, never()).updateAnnotations(any(), any(), any(), any());
    }

    /**
     * NaN passes plain range checks (both comparisons are false), and Jackson coerces the string
     * "NaN" into a Double on the way in — without an isFinite guard it would be persisted and then
     * serialized back as a JSON string where consumers expect a number.
     */
    @Test
    void nonFiniteTrustScoresAreRejectedAsBadRequest() {
        assertThat(restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest(null, null, Double.NaN)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(restApi.updateAnnotations(PROFILE_ID,
                new UpdateContactAnnotationsRequest(null, null, Double.POSITIVE_INFINITY)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        assertThat(restApi.updateAnnotations(PROFILE_ID,
                new UpdateContactAnnotationsRequest(null, null, Double.NEGATIVE_INFINITY)).getStatus())
                .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(contactListService, never()).updateAnnotations(any(), any(), any(), any());
    }

    @Test
    void updatingAnnotationsOnANonContactIsNotFound() {
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.empty());

        Response response = restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest("friend", null, null));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    /**
     * ONE domain call for the whole Save — one persist request and one subscription push; null
     * means "leave unchanged", not "clear". Per-field setters could leave a partially captured
     * store snapshot behind (the persistence rate limiter drops follow-up writes).
     */
    @Test
    void updateAppliesTheProvidedAnnotationsInOneDomainCall() {
        ContactListEntry entry = mock(ContactListEntry.class);
        when(contactListService.findContactListEntry(PROFILE_ID)).thenReturn(Optional.of(entry));

        Response response = restApi.updateAnnotations(PROFILE_ID, new UpdateContactAnnotationsRequest("friend", null, 0.7));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
        verify(contactListService, times(1)).updateAnnotations(entry, "friend", null, 0.7);
        verify(contactListService, never()).setTag(any(), any());
        verify(contactListService, never()).setNotes(any(), any());
        verify(contactListService, never()).setTrustScore(any(), any());
    }

    /**
     * Deep stub plus the byte[] getters the dto mapping digests; see
     * {@code ContactsWebSocketServiceTest#mockUserProfile}.
     */
    private UserProfile mockMappableProfile() {
        UserProfile profile = mock(UserProfile.class, RETURNS_DEEP_STUBS);
        when(profile.getNickName()).thenReturn("peer");
        when(profile.getProofOfWork().getPayload()).thenReturn(new byte[0]);
        when(profile.getProofOfWork().getSolution()).thenReturn(new byte[0]);
        when(profile.getNetworkId().getPubKey().getPublicKey().getEncoded()).thenReturn(new byte[0]);
        return profile;
    }
}
