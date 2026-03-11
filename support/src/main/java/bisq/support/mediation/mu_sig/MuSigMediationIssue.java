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

package bisq.support.mediation.mu_sig;

import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.contract.Role;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode
@ToString
public final class MuSigMediationIssue implements PersistableProto {
    private final long date;
    private final Role reportingRole;
    private final MuSigMediationIssueType type;

    public MuSigMediationIssue(Role reportingRole, MuSigMediationIssueType type) {
        this(currentTimeMillis(), reportingRole, type);
    }

    private MuSigMediationIssue(long date, Role reportingRole, MuSigMediationIssueType type) {
        this.date = date;
        this.reportingRole = reportingRole;
        this.type = type;
        verify();
    }

    public void verify() {
        NetworkDataValidation.validateDate(date);
    }

    @Override
    public bisq.support.protobuf.MuSigMediationIssue.Builder getBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigMediationIssue.newBuilder()
                .setDate(date)
                .setReportingRole(reportingRole.toProtoEnum())
                .setType(type.toProtoEnum());
    }

    @Override
    public bisq.support.protobuf.MuSigMediationIssue toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigMediationIssue fromProto(bisq.support.protobuf.MuSigMediationIssue proto) {
        return new MuSigMediationIssue(
                proto.getDate(),
                Role.fromProto(proto.getReportingRole()),
                MuSigMediationIssueType.fromProto(proto.getType()));
    }
}
