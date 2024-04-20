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

package bisq.network.p2p.services.confidential;

import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.ConfidentialData;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class ConfidentialMessage implements EnvelopePayloadMessage, DistributedData {
    private final ConfidentialData confidentialData;
    private final String receiverKeyId;

    ConfidentialMessage(ConfidentialData confidentialData, String receiverKeyId) {
        this.confidentialData = confidentialData;
        this.receiverKeyId = receiverKeyId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(receiverKeyId);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setConfidentialMessage(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.ConfidentialMessage toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.ConfidentialMessage.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.ConfidentialMessage.newBuilder()
                .setConfidentialData(confidentialData.toProto(serializeForHash))
                .setReceiverKeyId(receiverKeyId);
    }

    public static ConfidentialMessage fromProto(bisq.network.protobuf.ConfidentialMessage proto) {
        return new ConfidentialMessage(
                ConfidentialData.fromProto(proto.getConfidentialData()),
                proto.getReceiverKeyId()
        );
    }

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.1, 1, confidentialData.getCostFactor());
    }

    @Override
    public MetaData getMetaData() {
        throw new RuntimeException("Metadata of ConfidentialMessage is not provided as content is encrypted and " +
                "therefore the content type is unknown. " +
                "The MailBoxData class must provide instead the meta data");
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return confidentialData == null ||
                receiverKeyId == null ||
                !Arrays.equals(DigestUtil.hash(confidentialData.getSenderPublicKey()), pubKeyHash);
    }
}
