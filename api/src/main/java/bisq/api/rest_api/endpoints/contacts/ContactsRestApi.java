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

import bisq.api.dto.mappings.user.contact_list.ContactListEntryDtoMapping;
import bisq.api.rest_api.endpoints.RestApiBase;
import bisq.user.UserService;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactListService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import bisq.user.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * The node owner's contact list (My Contacts). Live updates ride the {@code CONTACTS} WebSocket
 * topic; these endpoints only mutate.
 * <p>
 * Add/remove report {@code changed=false} when the desired state already held, mirroring
 * {@link ContactListService}'s idempotent booleans — clients must not surface that as an error.
 * The annotation setters validate here and answer 400, because the domain setters silently ignore
 * invalid input, which over an API would read as success.
 * <p>
 * Error and log messages deliberately avoid the tag/notes/trustScore values (user-authored, the one
 * place a user could hand-write identifying information) and keep profile ids out of bodies clients
 * relay into logs. That guarantee covers this class only: the shared WebSocket send path TRACE-logs
 * full payloads, a tradeoff it already accepts for private chat text.
 */
@Slf4j
@Path("/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Contacts API", description = "Endpoints for the node owner's contact list (My Contacts)")
public class ContactsRestApi extends RestApiBase {
    private final ContactListService contactListService;
    private final UserProfileService userProfileService;
    private final UserIdentityService userIdentityService;

    public ContactsRestApi(UserService userService) {
        contactListService = userService.getContactListService();
        userProfileService = userService.getUserProfileService();
        userIdentityService = userService.getUserIdentityService();
    }

    @POST
    @Path("/{userProfileId}")
    @Operation(
            summary = "Add a user profile to the contact list",
            description = "Adds the profile as a contact under the node's currently selected user identity. "
                    + "Idempotent: an already-present contact answers 200 with changed=false. The response carries "
                    + "the entry as the node now holds it, so clients can render the add without waiting for the "
                    + "subscription push.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "The contact was added; changed=true, plus the entry",
                            content = @Content(schema = @Schema(implementation = AddContactResponse.class))),
                    @ApiResponse(responseCode = "200", description = "Already a contact; changed=false, plus the entry",
                            content = @Content(schema = @Schema(implementation = AddContactResponse.class))),
                    @ApiResponse(responseCode = "400", description = "The profile is one of my own identities"),
                    @ApiResponse(responseCode = "404", description = "No user profile found for the given profile ID"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public Response addContact(@PathParam("userProfileId") String userProfileId, AddContactRequest request) {
        // Jersey hands back null for an absent entity rather than rejecting the request.
        if (request == null || request.contactReason() == null) {
            return buildResponse(Response.Status.BAD_REQUEST, "A contact reason is required");
        }
        try {
            // Answered before consulting the profile store: profiles expire from the network while
            // the contact list keeps its embedded copy, and an idempotent re-add must keep its
            // documented changed=false answer rather than turn into a 404 (see removeContact).
            Optional<ContactListEntry> existingEntry = contactListService.findContactListEntry(userProfileId);
            if (existingEntry.isPresent()) {
                return buildOkResponse(new AddContactResponse(false,
                        ContactListEntryDtoMapping.fromBisq2Model(existingEntry.get())));
            }
            if (userIdentityService.findUserIdentity(userProfileId).isPresent()) {
                return buildResponse(Response.Status.BAD_REQUEST, "Cannot add an own identity as contact");
            }
            Optional<UserProfile> userProfile = userProfileService.findUserProfile(userProfileId);
            if (userProfile.isEmpty()) {
                return buildNotFoundResponse("No user profile found for the given profile ID");
            }
            UserProfile myUserProfile = userIdentityService.getSelectedUserIdentity().getUserProfile();
            boolean changed = contactListService.addContactListEntry(userProfile.get(),
                    myUserProfile,
                    ContactListEntryDtoMapping.toBisq2Model(request.contactReason()));
            AddContactResponse response = new AddContactResponse(changed,
                    contactListService.findContactListEntry(userProfileId)
                            .map(ContactListEntryDtoMapping::fromBisq2Model)
                            .orElse(null));
            // changed=false despite the earlier absence means a concurrent add won the race.
            return changed
                    ? buildResponse(Response.Status.CREATED, response)
                    : buildOkResponse(response);
        } catch (Exception e) {
            log.error("Failed to add contact", e);
            return buildErrorResponse("Failed to add contact");
        }
    }

    @DELETE
    @Path("/{userProfileId}")
    @Operation(
            summary = "Remove a user profile from the contact list",
            description = "Idempotent: a profile that is not a contact answers changed=false.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Whether the list changed",
                            content = @Content(schema = @Schema(implementation = ContactMutationResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public Response removeContact(@PathParam("userProfileId") String userProfileId) {
        try {
            // Resolved from the contact list, not the profile store: a contact whose profile has
            // expired from the network must still be removable.
            boolean changed = contactListService.findContactListEntry(userProfileId)
                    .map(contactListService::removeContactListEntry)
                    .orElse(false);
            return buildOkResponse(new ContactMutationResponse(changed));
        } catch (Exception e) {
            log.error("Failed to remove contact", e);
            return buildErrorResponse("Failed to remove contact");
        }
    }

    @PUT
    @Path("/{userProfileId}/annotations")
    @Operation(
            summary = "Update the contact's annotations (tag, notes, trust score) in one call",
            description = "A Save is one action, and for mobile clients one round trip. "
                    + "Omitted (null) fields are left unchanged; sending none is a no-op.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Annotations applied"),
                    @ApiResponse(responseCode = "400", description = "A provided field violates its limit"),
                    @ApiResponse(responseCode = "404", description = "The profile is not a contact"),
                    @ApiResponse(responseCode = "500", description = "Unexpected internal error")
            }
    )
    public Response updateAnnotations(@PathParam("userProfileId") String userProfileId, UpdateContactAnnotationsRequest request) {
        // Validated up front and atomically refused: the domain setters silently ignore invalid
        // input, which over an API would read as success — and a Save must not half-apply.
        if (request == null) {
            return buildResponse(Response.Status.BAD_REQUEST, "Request body is required");
        }
        if (request.tag() != null && request.tag().length() > ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "Tag must not exceed " + ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH + " characters");
        }
        if (request.notes() != null && request.notes().length() > ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "Notes must not exceed " + ContactListService.CONTACT_LIST_ENTRY_MAX_NOTES_LENGTH + " characters");
        }
        // isFinite: NaN slips through plain range checks (both comparisons are false), and Jackson
        // coerces the string "NaN" into a Double on the way in.
        if (request.trustScore() != null
                && (!Double.isFinite(request.trustScore())
                || request.trustScore() < ContactListService.CONTACT_LIST_ENTRY_MIN_TRUST_SCORE
                || request.trustScore() > ContactListService.CONTACT_LIST_ENTRY_MAX_TRUST_SCORE)) {
            return buildResponse(Response.Status.BAD_REQUEST, "Trust score must be between 0 and 1");
        }
        try {
            Optional<ContactListEntry> entry = contactListService.findContactListEntry(userProfileId);
            if (entry.isEmpty()) {
                return buildNotFoundResponse("The profile is not a contact");
            }
            // One domain call: one persist request and one subscription push for the whole Save,
            // where per-field setters could leave a partially captured store snapshot behind.
            contactListService.updateAnnotations(entry.get(), request.tag(), request.notes(), request.trustScore());
            return buildNoContentResponse();
        } catch (Exception e) {
            log.error("Failed to update the contact's annotations", e);
            return buildErrorResponse("Failed to update the contact's annotations");
        }
    }
}
