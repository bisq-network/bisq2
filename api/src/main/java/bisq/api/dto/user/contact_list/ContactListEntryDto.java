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

package bisq.api.dto.user.contact_list;

import bisq.api.dto.user.profile.UserProfileDto;

import javax.annotation.Nullable;

/**
 * An entry of the node owner's contact list.
 * <p>
 * Deliberately without {@code myUserProfile}, unlike the domain's {@code ContactListEntry}: a client
 * renders the contact, not which of the owner's identities added it. {@code trustScore}, {@code tag}
 * and {@code notes} are the user-editable annotations and are null until set.
 */
public record ContactListEntryDto(UserProfileDto userProfile,
                                  long date,
                                  ContactReasonDto contactReason,
                                  @Nullable Double trustScore,
                                  @Nullable String tag,
                                  @Nullable String notes) {
}
