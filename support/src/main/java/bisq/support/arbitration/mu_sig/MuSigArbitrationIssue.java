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

package bisq.support.arbitration.mu_sig;

import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.contract.Role;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode
@ToString(onlyExplicitlyIncluded = true)
public final class MuSigArbitrationIssue implements PersistableProto {
    @ToString.Include
    private final long date;
    @ToString.Include
    private final Role causingRole;
    @ToString.Include
    private final MuSigArbitrationIssueType type;
    private final Optional<String> details;

    public MuSigArbitrationIssue(Role causingRole, MuSigArbitrationIssueType type) {
        this(currentTimeMillis(), causingRole, type, Optional.empty());
    }

    public MuSigArbitrationIssue(Role causingRole, MuSigArbitrationIssueType type, Optional<String> details) {
        this(currentTimeMillis(), causingRole, type, details);
    }

    private MuSigArbitrationIssue(long date,
                                  Role causingRole,
                                  MuSigArbitrationIssueType type,
                                  Optional<String> details) {
        this.date = date;
        this.causingRole = causingRole;
        this.type = type;
        this.details = details;
        verify();
    }

    public void verify() {
        NetworkDataValidation.validateDate(date);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationIssue.Builder getBuilder(boolean serializeForHash) {
        var builder = bisq.support.protobuf.MuSigArbitrationIssue.newBuilder()
                .setDate(date)
                .setCausingRole(causingRole.toProtoEnum())
                .setType(type.toProtoEnum());
        details.ifPresent(builder::setDetails);
        return builder;
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationIssue toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigArbitrationIssue fromProto(bisq.support.protobuf.MuSigArbitrationIssue proto) {
        return new MuSigArbitrationIssue(
                proto.getDate(),
                Role.fromProto(proto.getCausingRole()),
                MuSigArbitrationIssueType.fromProto(proto.getType()),
                proto.hasDetails() ? Optional.of(proto.getDetails()) : Optional.empty());
    }
}
