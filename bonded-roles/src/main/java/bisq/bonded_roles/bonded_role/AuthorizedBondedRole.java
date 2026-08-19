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

package bisq.bonded_roles.bonded_role;

import bisq.bonded_roles.AuthorizedPubKeys;
import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.oracle.AuthorizedOracleNode;
import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.DevMode;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.BitcoinTransactionValidation;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedData;
import bisq.network.p2p.services.data.storage.auth.authorized.AuthorizedDistributedData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

import static bisq.bonded_roles.registration.BondedRoleRegistrationProtocol.LEGACY_VERSION;
import static bisq.bonded_roles.registration.BondedRoleRegistrationProtocol.PROPOSAL_KEY_VERSION;
import static bisq.network.p2p.services.data.storage.MetaData.HIGHEST_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_100_DAYS;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@EqualsAndHashCode
@Getter
public final class AuthorizedBondedRole implements AuthorizedDistributedData {
    private static final int LEGACY_DATA_VERSION = 1;
    private static final int PROPOSAL_KEY_DATA_VERSION = 2;
    private static final int CURRENT_DATA_VERSION = PROPOSAL_KEY_DATA_VERSION;

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_100_DAYS, HIGHEST_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final int version;
    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signatureBase64;
    private final Optional<AddressByTransportTypeMap> addressByTransportTypeMap;
    private final NetworkId networkId;
    // The oracle node which did the validation and publishing
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final Optional<AuthorizedOracleNode> authorizingOracleNode;

    // ExcludeForHash from version 1 on to not treat data from different oracle nodes with different staticPublicKeysProvided value as duplicate data.
    // We add version 2 and 3 for extra safety...
    // Once no nodes with versions below 2.1.0  are expected anymore in the network we can remove the parameter
    // and use default `@ExcludeForHash` instead.
    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    @EqualsAndHashCode.Exclude
    private final boolean staticPublicKeysProvided;

    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    private final int registrationProtocolVersion;
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    private final String proposalTxId;
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    private final String lockupTxId;

    public AuthorizedBondedRole(String profileId,
                                String authorizedPublicKey,
                                BondedRoleType bondedRoleType,
                                String bondUserName,
                                String signatureBase64,
                                Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                NetworkId networkId,
                                Optional<AuthorizedOracleNode> authorizingOracleNode,
                                boolean staticPublicKeysProvided) {
        this(LEGACY_DATA_VERSION,
                profileId,
                authorizedPublicKey,
                bondedRoleType,
                bondUserName,
                signatureBase64,
                addressByTransportTypeMap,
                networkId,
                authorizingOracleNode,
                staticPublicKeysProvided,
                LEGACY_VERSION,
                "",
                "");
        verifyRegistration();
    }

    public AuthorizedBondedRole(String profileId,
                                String authorizedPublicKey,
                                BondedRoleType bondedRoleType,
                                String bondUserName,
                                String signatureBase64,
                                Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                NetworkId networkId,
                                Optional<AuthorizedOracleNode> authorizingOracleNode,
                                boolean staticPublicKeysProvided,
                                int registrationProtocolVersion,
                                String proposalTxId,
                                String lockupTxId) {
        this(dataVersionForProtocol(registrationProtocolVersion),
                profileId,
                authorizedPublicKey,
                bondedRoleType,
                bondUserName,
                signatureBase64,
                addressByTransportTypeMap,
                networkId,
                authorizingOracleNode,
                staticPublicKeysProvided,
                registrationProtocolVersion,
                proposalTxId,
                lockupTxId);
        verifyRegistration();
    }

    private AuthorizedBondedRole(int version,
                                 String profileId,
                                 String authorizedPublicKey,
                                 BondedRoleType bondedRoleType,
                                 String bondUserName,
                                 String signatureBase64,
                                 Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                 NetworkId networkId,
                                 Optional<AuthorizedOracleNode> authorizingOracleNode,
                                 boolean staticPublicKeysProvided,
                                 int registrationProtocolVersion,
                                 String proposalTxId,
                                 String lockupTxId) {
        this.version = version;
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.addressByTransportTypeMap = addressByTransportTypeMap;
        this.networkId = networkId;
        this.authorizingOracleNode = authorizingOracleNode;
        this.staticPublicKeysProvided = staticPublicKeysProvided;
        this.registrationProtocolVersion = registrationProtocolVersion;
        this.proposalTxId = proposalTxId;
        this.lockupTxId = lockupTxId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validatePubKeyHex(authorizedPublicKey);
        NetworkDataValidation.validateBondUserName(bondUserName);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
        checkArgument(version >= 0, "AuthorizedBondedRole version must not be negative");
        checkArgument(registrationProtocolVersion >= 0,
                "Bonded-role registration protocol version must not be negative");
        checkArgument(proposalTxId.isEmpty() || BitcoinTransactionValidation.isValid(proposalTxId),
                "Proposal tx ID must be empty or a valid Bitcoin transaction ID");
        checkArgument(lockupTxId.isEmpty() || BitcoinTransactionValidation.isValid(lockupTxId),
                "Lockup tx ID must be empty or a valid Bitcoin transaction ID");
    }

    public boolean canReconstructForRemoval() {
        return version == LEGACY_DATA_VERSION || version == PROPOSAL_KEY_DATA_VERSION;
    }

    public void verifyRegistration() {
        if (version > CURRENT_DATA_VERSION) {
            return;
        }
        BondedRoleRegistrationProtocol.verifyProof(registrationProtocolVersion, proposalTxId, lockupTxId);
        checkArgument(switch (version) {
                    case 0, LEGACY_DATA_VERSION -> registrationProtocolVersion == LEGACY_VERSION;
                    case PROPOSAL_KEY_DATA_VERSION -> registrationProtocolVersion == PROPOSAL_KEY_VERSION;
                    default -> false;
                },
                "AuthorizedBondedRole version does not match its registration protocol version");
    }

    private static int dataVersionForProtocol(int registrationProtocolVersion) {
        return switch (registrationProtocolVersion) {
            case LEGACY_VERSION -> LEGACY_DATA_VERSION;
            case PROPOSAL_KEY_VERSION -> PROPOSAL_KEY_DATA_VERSION;
            default -> throw new IllegalArgumentException(
                    "Unsupported bonded-role registration protocol version: " + registrationProtocolVersion);
        };
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole.Builder getBuilder(boolean serializeForHash) {
        bisq.bonded_roles.protobuf.AuthorizedBondedRole.Builder builder = bisq.bonded_roles.protobuf.AuthorizedBondedRole.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProtoEnum())
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .setNetworkId(networkId.toProto(serializeForHash))
                .setStaticPublicKeysProvided(staticPublicKeysProvided)
                .setVersion(version)
                .setRegistrationProtocolVersion(registrationProtocolVersion)
                .setProposalTxId(proposalTxId)
                .setLockupTxId(lockupTxId);
        addressByTransportTypeMap.ifPresent(e -> builder.setAddressByTransportTypeMap(e.toProto(serializeForHash)));
        authorizingOracleNode.ifPresent(oracleNode -> builder.setAuthorizingOracleNode(oracleNode.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.bonded_roles.protobuf.AuthorizedBondedRole toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static AuthorizedBondedRole fromProto(bisq.bonded_roles.protobuf.AuthorizedBondedRole proto) {
        return new AuthorizedBondedRole(
                proto.getVersion(),
                proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignatureBase64(),
                proto.hasAddressByTransportTypeMap() ?
                        Optional.of(AddressByTransportTypeMap.fromProto(proto.getAddressByTransportTypeMap())) :
                        Optional.empty(),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.hasAuthorizingOracleNode() ?
                        Optional.of(AuthorizedOracleNode.fromProto(proto.getAuthorizingOracleNode())) :
                        Optional.empty(),
                proto.getStaticPublicKeysProvided(),
                BondedRoleRegistrationProtocol.versionFromProto(proto.hasRegistrationProtocolVersion(),
                        proto.getRegistrationProtocolVersion()),
                proto.getProposalTxId(),
                proto.getLockupTxId()
        );
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.bonded_roles.protobuf.AuthorizedBondedRole.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return 0.5;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        // Can be removed after I2P is activated
        if (!AuthorizedData.IS_I2P_ACTIVATED ) {
            if (networkId.getAddressByTransportTypeMap().containsKey(TransportType.I2P)) {
                log.warn("AuthorizedBondedRole considered invalid as it contains an I2PAddress address and we have not yet activated I2P.\n" +
                        "networkId={}", networkId);
                return true;
            }
            Boolean hasAuthorizingOracleNodeI2pAddress = authorizingOracleNode.map(node -> node.getNetworkId().getAddressByTransportTypeMap().containsKey(TransportType.I2P))
                    .orElse(false);
            if(hasAuthorizingOracleNodeI2pAddress){
                log.warn("AuthorizedBondedRole considered invalid as authorizingOracleNode contains an I2PAddress address and we have not yet activated I2P.\n" +
                        "authorizingOracleNode={}", authorizingOracleNode);
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> getAuthorizedPublicKeys() {
        if (DevMode.isDevMode()) {
            return AuthorizedPubKeys.DEV_PUB_KEYS;
        } else {
            return AuthorizedPubKeys.ORACLE_NODE_PUB_KEYS;
        }
    }

    @Override
    public boolean staticPublicKeysProvided() {
        return staticPublicKeysProvided;
    }

    @Override
    public String toString() {
        return "AuthorizedBondedRole{" +
                "\r\n                    bondedRoleType=" + bondedRoleType +
                ",\r\n                    profileId='" + profileId + '\'' +
                ",\r\n                    authorizedPublicKey='" + authorizedPublicKey + '\'' +
                ",\r\n                    bondUserName='" + bondUserName + '\'' +
                ",\r\n                    signature='" + signatureBase64 + '\'' +
                ",\r\n                    networkId=" + networkId +
                ",\r\n                    addressByTransportTypeMap=" + addressByTransportTypeMap +
                ",\r\n                    authorizedOracleNode=" + authorizingOracleNode +
                ",\r\n                    staticPublicKeysProvided=" + staticPublicKeysProvided +
                ",\r\n                    registrationProtocolVersion=" + registrationProtocolVersion +
                ",\r\n                    proposalTxId='" + proposalTxId + '\'' +
                ",\r\n                    lockupTxId='" + lockupTxId + '\'' +
                ",\r\n                    metaData=" + metaData +
                ",\r\n                    authorizedPublicKeys=" + getAuthorizedPublicKeys() +
                "\r\n}";
    }
}
