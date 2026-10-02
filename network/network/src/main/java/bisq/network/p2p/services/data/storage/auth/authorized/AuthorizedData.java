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

package bisq.network.p2p.services.data.storage.auth.authorized;

import bisq.common.encoding.Hex;
import bisq.common.encoding.canonical.CanonicalEncoder;
import bisq.common.encoding.canonical.CanonicalSchema;
import bisq.common.encoding.canonical.EncodingPurpose;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.util.DateUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.DistributedDataAny;
import bisq.network.p2p.services.data.storage.OwnershipVerifiableData;
import bisq.network.p2p.services.data.storage.auth.AuthenticatedData;
import bisq.security.DigestUtil;
import bisq.security.SignatureUtil;
import bisq.security.keys.EcPublicKeyValidation;
import bisq.security.keys.KeyGeneration;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.Optional;

/**
 * Used for verifying if data publisher is authorized to publish this data (e.g. ProofOfBurnData, Filter, Alert, DisputeAgent...).
 * We use the provided signature and pubKey and check if the pubKey is in the set of provided authorized puKeys from
 * the authorizedDistributedData object, which will return a hard coded set of pubKeys.
 */
@Slf4j
@Getter
public final class AuthorizedData extends AuthenticatedData {
    // Oracle and seed nodes of earlier versions published data of some types with the key pair of their network
    // identity (see AuthorizedDistributedData.isLegacyOwnerAccepted). Their data created before this date is accepted
    // with another owner than the authorized key. The date must be after all oracle and seed nodes publish with their
    // authorized key pair (see docs/specifications/network/storage-ownership.md).
    public static final Date LEGACY_OWNER_CUTOFF_DATE = DateUtils.getUTCDate(2027, GregorianCalendar.JANUARY, 1);


    private final Optional<byte[]> signature;
    private final byte[] authorizedPublicKeyBytes;
    transient private final PublicKey authorizedPublicKey;

    // At remove, we do not need to authorizedPublicKey as the normal keypair is used to verify right to remove.
    public AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                          PublicKey authorizedPublicKey) {
        this(authorizedDistributedData,
                authorizedDistributedData.toAny(false),
                Optional.empty(),
                authorizedPublicKey,
                authorizedPublicKey.getEncoded());
    }

    public AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                          Optional<byte[]> signature,
                          PublicKey authorizedPublicKey) {
        this(authorizedDistributedData,
                authorizedDistributedData.toAny(false),
                signature,
                authorizedPublicKey,
                authorizedPublicKey.getEncoded());
    }

    // distributedDataAny: see AuthenticatedData
    private AuthorizedData(AuthorizedDistributedData authorizedDistributedData,
                           Any distributedDataAny,
                           Optional<byte[]> signature,
                           PublicKey authorizedPublicKey,
                           byte[] authorizedPublicKeyBytes) {
        super(authorizedDistributedData, distributedDataAny);
        this.signature = signature;
        this.authorizedPublicKey = authorizedPublicKey;
        this.authorizedPublicKeyBytes = authorizedPublicKeyBytes;

        verify();
    }

    @Override
    public void verify() {
        signature.ifPresent(NetworkDataValidation::validateECSignature);
        EcPublicKeyValidation.validate(authorizedPublicKeyBytes);
    }

    @Override
    public byte[] serializeForHash() {
        // We omit the signature for the hash, otherwise we would get a new map entry for the same data at each republishing
        return getAuthenticatedDataBuilder(true).setAuthorizedData(
                        bisq.network.protobuf.AuthorizedData.newBuilder()
                                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes)))
                .build().toByteArray();
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthenticatedData.Builder getBuilder(boolean serializeForHash) {
        return getAuthenticatedDataBuilder(serializeForHash).setAuthorizedData(toValueProto(serializeForHash));
    }

    public bisq.network.protobuf.AuthorizedData toValueProto(boolean serializeForHash) {
        return resolveBuilder(getValueBuilder(serializeForHash), serializeForHash).build();
    }

    public bisq.network.protobuf.AuthorizedData.Builder getValueBuilder(boolean serializeForHash) {
        bisq.network.protobuf.AuthorizedData.Builder builder = bisq.network.protobuf.AuthorizedData.newBuilder()
                .setAuthorizedPublicKeyBytes(ByteString.copyFrom(authorizedPublicKeyBytes));
        signature.ifPresent(signature -> builder.setSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    public static AuthorizedData fromProto(bisq.network.protobuf.AuthenticatedData proto)
            throws UnresolvableProtobufException {
        try {
            bisq.network.protobuf.AuthorizedData authorizedDataProto = proto.getAuthorizedData();
            byte[] authorizedPublicKeyBytes = authorizedDataProto.getAuthorizedPublicKeyBytes().toByteArray();
            PublicKey authorizedPublicKey = KeyGeneration.generatePublic(authorizedPublicKeyBytes);
            Any distributedDataAny = DistributedDataAny.retain(proto.getDistributedData());
            DistributedData distributedData = DistributedData.fromAny(proto.getDistributedData());
            if (distributedData instanceof AuthorizedDistributedData) {
                Optional<byte[]> signature = authorizedDataProto.hasSignature() ?
                        Optional.of(authorizedDataProto.getSignature().toByteArray()) :
                        Optional.empty();
                return new AuthorizedData((AuthorizedDistributedData) distributedData,
                        distributedDataAny,
                        signature,
                        authorizedPublicKey,
                        authorizedPublicKeyBytes
                );
            } else {
                throw new UnresolvableProtobufException("DistributedData must be type of AuthorizedDistributedData");
            }
        } catch (GeneralSecurityException | RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }


    /* --------------------------------------------------------------------- */
    // Canonical
    /* --------------------------------------------------------------------- */

    // Unlike serializeForHash(), which omits the signature so that republishing keeps the storage key, the default
    // encoding contains all non-transient fields, including the signature. The SIGNATURE purpose uses the default
    // schema: it contains the DistributedData with its exact bytes. The STORAGE_KEY, PROOF_OF_WORK and TRADE_CONTRACT
    // schemas contain the DistributedData in its hash serialization. serializeForHash() omits the signature only where
    // AuthorizedData is the root of the preimage (see docs/specifications/encoding):
    // - STORAGE_KEY is the preimage of the data hash of the storage key, where AuthorizedData is always the root. It
    //   omits the signature, as serializeForHash() does.
    // - PROOF_OF_WORK and TRADE_CONTRACT: AuthorizedData is always nested, in a request or in a contract, whose hash
    //   serialization contains the signature.
    public static final CanonicalSchema<AuthorizedData> DEFAULT_SCHEMA =
            AuthorizedData.<AuthorizedData>getAuthenticatedDataSchemaBuilder()
                    .extend(11, authorizedData -> authorizedData, getAuthorizedDataSchemaBuilder())
                    .build();

    public static final CanonicalSchema<AuthorizedData> STORAGE_KEY_SCHEMA =
            AuthorizedData.<AuthorizedData>getSchemaBuilderForHash()
                    .extend(11, authorizedData -> authorizedData, CanonicalSchema.<AuthorizedData>newBuilder()
                            .bytes(2, authorizedData -> authorizedData.authorizedPublicKeyBytes))
                    .build();

    public static final CanonicalSchema<AuthorizedData> PROOF_OF_WORK_SCHEMA = createNestedSchemaForHash();
    public static final CanonicalSchema<AuthorizedData> TRADE_CONTRACT_SCHEMA = createNestedSchemaForHash();

    private static CanonicalSchema<AuthorizedData> createNestedSchemaForHash() {
        return AuthorizedData.<AuthorizedData>getSchemaBuilderForHash()
                .extend(11, authorizedData -> authorizedData, getAuthorizedDataSchemaBuilder())
                .build();
    }

    private static CanonicalSchema.Builder<AuthorizedData> getAuthorizedDataSchemaBuilder() {
        return CanonicalSchema.<AuthorizedData>newBuilder()
                .bytes(1, authorizedData -> authorizedData.signature.orElse(null))
                .bytes(2, authorizedData -> authorizedData.authorizedPublicKeyBytes);
    }

    @Override
    public byte[] encodeCanonical(CanonicalEncoder canonicalEncoder, EncodingPurpose encodingPurpose) {
        CanonicalSchema<AuthorizedData> schema = switch (encodingPurpose) {
            case DEFAULT -> DEFAULT_SCHEMA;
            case SIGNATURE -> DEFAULT_SCHEMA;
            case STORAGE_KEY -> STORAGE_KEY_SCHEMA;
            case PROOF_OF_WORK -> PROOF_OF_WORK_SCHEMA;
            case TRADE_CONTRACT -> TRADE_CONTRACT_SCHEMA;
        };
        return canonicalEncoder.encode(this, schema, encodingPurpose);
    }

    public AuthorizedDistributedData getAuthorizedDistributedData() {
        return (AuthorizedDistributedData) distributedData;
    }

    public boolean isNotAuthorized() {
        try {
            AuthorizedDistributedData authorizedDistributedData = getAuthorizedDistributedData();
            if (!SignatureUtil.verify(distributedData.serializeForHash(), signature.orElseThrow(), authorizedPublicKey)) {
                return true;
            }

            if (authorizedDistributedData.staticPublicKeysProvided()) {
                return !authorizedDistributedData.getAuthorizedPublicKeys().contains(Hex.encode(authorizedPublicKeyBytes));
            }

            return false;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return true;
        }
    }

    // The owner which the distributed data names, else the authorized key. The authorization signature does not cover
    // the owner of the add request, so the authorized key must own the data: otherwise anyone could publish and remove
    // a copy of it.
    @Override
    public boolean isOwner(byte[] pubKeyHash) {
        byte[] ownerPubKeyHash = distributedData instanceof OwnershipVerifiableData ownershipVerifiableData
                ? ownershipVerifiableData.getOwnerPubKeyHash()
                : DigestUtil.hash(authorizedPublicKeyBytes);
        return Arrays.equals(ownerPubKeyHash, pubKeyHash);
    }

    @Override
    public boolean isOtherOwnerAccepted(long created) {
        return getAuthorizedDistributedData().isLegacyOwnerAccepted() && created < LEGACY_OWNER_CUTOFF_DATE.getTime();
    }

    @Override
    public String toString() {
        return "AuthorizedData{" +
                "\r\n               signature=" + signature.map(Hex::encode).orElse("null") +
                ",\r\n               authorizedPublicKeyBytes=" + Hex.encode(authorizedPublicKeyBytes) +
                ",\r\n               distributedData=" + distributedData +
                "\r\n} ";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizedData that)) return false;
        if (!super.equals(o)) return false;

        return Objects.equals(signature, that.signature) &&
                Arrays.equals(authorizedPublicKeyBytes, that.authorizedPublicKeyBytes) &&
                Objects.equals(authorizedPublicKey, that.authorizedPublicKey);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(signature);
        result = 31 * result + Arrays.hashCode(authorizedPublicKeyBytes);
        result = 31 * result + Objects.hashCode(authorizedPublicKey);
        return result;
    }
}
