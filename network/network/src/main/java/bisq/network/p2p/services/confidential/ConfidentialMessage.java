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

import bisq.common.encoding.canonical.Canonical;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.security.ConfidentialData;
import bisq.security.DigestUtil;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@ToString
@EqualsAndHashCode
@Getter
public final class ConfidentialMessage implements EnvelopePayloadMessage, OwnershipVerifiableData, Canonical {
    private final ConfidentialData confidentialData;
    private final String receiverKeyId;

    ConfidentialMessage(ConfidentialData confidentialData, String receiverKeyId) {
        this.confidentialData = confidentialData;
        this.receiverKeyId = receiverKeyId;

        verify();
    }

    @Override
    public void verify() {
        checkNotNull(confidentialData, "ConfidentialData must not be null");
        checkNotNull(receiverKeyId, "ReceiverKeyId must not be null");
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

    public static ConfidentialMessage fromProto(bisq.network.protobuf.ConfidentialMessage proto)
            throws UnresolvableProtobufException {
        try {
            return new ConfidentialMessage(
                    ConfidentialData.fromProto(proto.getConfidentialData()),
                    proto.getReceiverKeyId()
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // The complete encoding, with the EnvelopePayloadMessage envelope that serialize() writes
    public static final CanonicalSchema<ConfidentialMessage> DEFAULT_SCHEMA =
            CanonicalSchema.oneof("EnvelopePayloadMessage", 8, getValueSchemaBuilder());
    // The ConfidentialMessage message without envelope, as MailboxData contains it
    public static final CanonicalSchema<ConfidentialMessage> DEFAULT_VALUE_SCHEMA = getValueSchemaBuilder().build();
    public static final CanonicalSchema<ConfidentialMessage> SIGNATURE_VALUE_SCHEMA = DEFAULT_VALUE_SCHEMA;

    private static CanonicalSchema.Builder<ConfidentialMessage> getValueSchemaBuilder() {
        return CanonicalSchema.<ConfidentialMessage>newBuilder()
                .canonical(1, confidentialMessage -> confidentialMessage.confidentialData)
                .string(2, confidentialMessage -> confidentialMessage.receiverKeyId);
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<ConfidentialMessage> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> DEFAULT_SCHEMA;
            case PROOF_OF_WORK -> DEFAULT_SCHEMA;
            case TRADE_CONTRACT -> DEFAULT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    @Override
    public MetaData getMetaData() {
        throw new RuntimeException("Metadata of ConfidentialMessage is not provided as content is encrypted and " +
                "therefore the content type is unknown. " +
                "The MailBoxData class must provide instead the meta data");
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    // The sender owns the message. In the mailbox store, the sender signs the add request.
    @Override
    public byte[] getOwnerPubKeyHash() {
        return DigestUtil.hash(confidentialData.getSenderPublicKey());
    }
}
