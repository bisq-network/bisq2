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

package bisq.network.p2p.services.reporting;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Response;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class ReportResponse implements EnvelopePayloadMessage, Response {
    private final String requestId;
    private final Report report;

    public ReportResponse(String requestId, Report report) {
        this.requestId = requestId;
        this.report = report;

        verify();
    }

    @Override
    public void verify() {
        report.verify();
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setReportResponse(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.ReportResponse toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.ReportResponse.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.ReportResponse.newBuilder()
                .setRequestId(requestId)
                .setReport(report.toProto(serializeForHash));
    }

    public static ReportResponse fromProto(bisq.network.protobuf.ReportResponse proto) {
        return new ReportResponse(proto.getRequestId(),
                Report.fromProto(proto.getReport()));
    }

    @Override
    public double getCostFactor() {
        return 0.2;
    }
}