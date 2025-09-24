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

import bisq.common.proto.PersistableProto;
import bisq.common.util.OptionalUtils;
import bisq.user.profile.UserProfile;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

@EqualsAndHashCode
@ToString
@Getter
public final class ContactListEntry implements PersistableProto {
    private final UserProfile userProfile;
    private final long date; // Data when the entry was added
    private final ContactReason contactReason; // What was the reason of the contact
    private Optional<Double> trustScore; // 0-1, 1 = 100% - highest trust score
    private Optional<String> tag; // Short tag for quick identification
    private Optional<String> notes;

    public ContactListEntry(UserProfile userProfile, ContactReason contactReason) {
        this(userProfile, System.currentTimeMillis(),
                contactReason,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    public ContactListEntry(UserProfile userProfile,
                            long date,
                            ContactReason contactReason,
                            Optional<Double> trustScore,
                            Optional<String> tag,
                            Optional<String> notes) {
        this.userProfile = userProfile;
        this.date = date;
        this.contactReason = contactReason;
        this.trustScore = trustScore;
        this.tag = tag;
        this.notes = notes;
    }

    @Override
    public bisq.user.protobuf.ContactListEntry.Builder getBuilder(boolean serializeForHash) {
        bisq.user.protobuf.ContactListEntry.Builder builder = bisq.user.protobuf.ContactListEntry.newBuilder()
                .setUserProfile(userProfile.toProto(serializeForHash))
                .setDate(date)
                .setContactReason(contactReason.toProtoEnum());
        trustScore.ifPresent(builder::setTrustScore);
        tag.ifPresent(builder::setTag);
        notes.ifPresent(builder::setNotes);
        return builder;
    }

    @Override
    public bisq.user.protobuf.ContactListEntry toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static ContactListEntry fromProto(bisq.user.protobuf.ContactListEntry proto) {
        return new ContactListEntry(UserProfile.fromProto(proto.getUserProfile()),
                proto.getDate(),
                ContactReason.fromProto(proto.getContactReason()),
                OptionalUtils.optionalIf(proto.hasTrustScore(), proto::getTrustScore),
                OptionalUtils.optionalIf(proto.hasTag(), proto::getTag),
                OptionalUtils.optionalIf(proto.hasNotes(), proto::getNotes));
    }

    void setTrustScore(Double newTrustScore) {
        trustScore = Optional.of(newTrustScore);
    }

    void setTag(String newTag) {
        tag = newTag != null ? Optional.of(newTag) : Optional.empty();
    }

    void setNotes(String newNotes) {
        notes = newNotes != null ? Optional.of(newNotes) : Optional.empty();
    }
}
