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

package bisq.api.dto.mappings.user.contact_list;

import bisq.api.dto.DtoMappings;
import bisq.api.dto.user.contact_list.ContactListEntryDto;
import bisq.api.dto.user.contact_list.ContactReasonDto;
import bisq.user.contact_list.ContactListEntry;
import bisq.user.contact_list.ContactReason;

public class ContactListEntryDtoMapping {
    // toBisq2Model not provided: nothing consumes it, matching the sibling mappings.

    public static ContactListEntryDto fromBisq2Model(ContactListEntry value) {
        return new ContactListEntryDto(DtoMappings.UserProfileMapping.fromBisq2Model(value.getUserProfile()),
                value.getDate(),
                fromBisq2Model(value.getContactReason()),
                value.getTrustScore().orElse(null),
                value.getTag().orElse(null),
                value.getNotes().orElse(null));
    }

    /**
     * An explicit switch rather than {@code valueOf(value.name())}: the dto is the wire contract, and
     * a renamed domain constant must fail to compile here rather than change what clients receive.
     */
    public static ContactReasonDto fromBisq2Model(ContactReason value) {
        return switch (value) {
            case PRIVATE_CHAT -> ContactReasonDto.PRIVATE_CHAT;
            case BISQ_EASY_TRADE -> ContactReasonDto.BISQ_EASY_TRADE;
            case MUSIG_TRADE -> ContactReasonDto.MUSIG_TRADE;
            case MANUALLY_ADDED -> ContactReasonDto.MANUALLY_ADDED;
        };
    }

    public static ContactReason toBisq2Model(ContactReasonDto value) {
        return switch (value) {
            case PRIVATE_CHAT -> ContactReason.PRIVATE_CHAT;
            case BISQ_EASY_TRADE -> ContactReason.BISQ_EASY_TRADE;
            case MUSIG_TRADE -> ContactReason.MUSIG_TRADE;
            case MANUALLY_ADDED -> ContactReason.MANUALLY_ADDED;
        };
    }
}
