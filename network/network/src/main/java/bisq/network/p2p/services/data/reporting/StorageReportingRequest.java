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

package bisq.network.p2p.services.data.reporting;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.message.Request;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public final class StorageReportingRequest implements EnvelopePayloadMessage, Request {
    private final String requestId;

    public StorageReportingRequest(String requestId) {
        this.requestId = requestId;
        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setStorageReportingRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.StorageReportingRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.StorageReportingRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.StorageReportingRequest.newBuilder().setRequestId(requestId);
    }

    public static StorageReportingRequest fromProto(bisq.network.protobuf.StorageReportingRequest proto) {
        return new StorageReportingRequest(proto.getRequestId());
    }

    @Override
    public double getCostFactor() {
        return 0.05;
    }
}