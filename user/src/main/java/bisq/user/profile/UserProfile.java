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

package bisq.user.profile;

import bisq.common.annotation.ExcludeForHash;
import bisq.common.application.ApplicationVersion;
import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.StringUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.i18n.Res;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.PublishDateAware;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.user.identity.NymIdGenerator;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static bisq.network.p2p.services.data.storage.MetaData.*;

/**
 * Publicly shared user profile (from other peers or mine).
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Slf4j
@Getter
public final class UserProfile implements DistributedData, PublishDateAware {
    public static final int VERSION = 1;
    public static final int MAX_LENGTH_NICK_NAME = 100;
    public static final int MAX_LENGTH_TERMS = 500;
    public static final int MAX_LENGTH_STATEMENT = 100;

    public static UserProfile forEdit(UserProfile userProfile, String terms, String statement) {
        return new UserProfile(userProfile.getNickName(), userProfile.getProofOfWork(), userProfile.getAvatarVersion(),
                userProfile.getNetworkId(), terms, statement);
    }

    public static UserProfile createNew(String nickName,
                                        ProofOfWork proofOfWork,
                                        int avatarVersion,
                                        NetworkId networkId,
                                        String terms,
                                        String statement) {
        return new UserProfile(nickName,
                proofOfWork,
                avatarVersion,
                networkId,
                terms,
                statement);
    }

    public static UserProfile withVersion(UserProfile userProfile, int version) {
        return new UserProfile(version, userProfile.getNickName(), userProfile.getProofOfWork(), userProfile.getAvatarVersion(),
                userProfile.getNetworkId(), userProfile.getTerms(), userProfile.getStatement(),
                ApplicationVersion.getVersion().getVersionAsString());
    }

    // We give a bit longer TTL than the chat messages to ensure the chat user is available as long the messages are
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_15_DAYS, DEFAULT_PRIORITY, getClass().getSimpleName(), MAX_MAP_SIZE_10_000);

    @EqualsAndHashCode.Include
    private final String nickName;
    // We need the proofOfWork for verification of the nym and catHash icon
    @EqualsAndHashCode.Include
    private final ProofOfWork proofOfWork;
    @EqualsAndHashCode.Include
    private final NetworkId networkId;
    @EqualsAndHashCode.Include
    private final String terms;
    @EqualsAndHashCode.Include
    private final String statement;

    @ExcludeForHash(excludeOnlyInVersions = {1, 2, 3})
    private final int avatarVersion;
    @ExcludeForHash
    private final int version;
    @ExcludeForHash(excludeOnlyInVersions = {0})
    private final String applicationVersion;

    private transient String nym;
    private transient ByteArray proofOfBurnHash;
    private transient ByteArray bondedReputationHash;
    private transient long publishDate;

    private UserProfile(String nickName,
                        ProofOfWork proofOfWork,
                        int avatarVersion,
                        NetworkId networkId,
                        String terms,
                        String statement) {
        this(VERSION,
                nickName,
                proofOfWork,
                avatarVersion,
                networkId,
                terms,
                statement,
                ApplicationVersion.getVersion().getVersionAsString());
    }

    private UserProfile(int version,
                        String nickName,
                        ProofOfWork proofOfWork,
                        int avatarVersion,
                        NetworkId networkId,
                        String terms,
                        String statement,
                        String applicationVersion) {
        this.version = version;
        this.nickName = nickName;
        this.proofOfWork = proofOfWork;
        this.avatarVersion = avatarVersion;
        this.networkId = networkId;
        this.terms = terms;
        this.statement = statement;
        this.applicationVersion = applicationVersion;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(nickName, MAX_LENGTH_NICK_NAME);
        NetworkDataValidation.validateText(terms, MAX_LENGTH_TERMS);
        NetworkDataValidation.validateText(statement, MAX_LENGTH_STATEMENT);
        if (!applicationVersion.isEmpty()) {
            NetworkDataValidation.validateVersion(applicationVersion);
        }
    }

    @Override
    public bisq.user.protobuf.UserProfile.Builder getBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.UserProfile.newBuilder()
                .setVersion(version)
                .setNickName(nickName)
                .setTerms(terms)
                .setStatement(statement)
                .setAvatarVersion(avatarVersion)
                .setProofOfWork(proofOfWork.toProto(serializeForHash))
                .setNetworkId(networkId.toProto(serializeForHash))
                .setApplicationVersion(applicationVersion);
    }

    @Override
    public bisq.user.protobuf.UserProfile toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    public static UserProfile fromProto(bisq.user.protobuf.UserProfile proto) {
        return new UserProfile(proto.getVersion(),
                proto.getNickName(),
                ProofOfWork.fromProto(proto.getProofOfWork()),
                proto.getAvatarVersion(),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getTerms(),
                proto.getStatement(),
                proto.getApplicationVersion());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.UserProfile.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public void setPublishDate(long publishDate) {
        this.publishDate = publishDate;
    }

    @Override
    public double getCostFactor() {
        return 0.3;
    }

    @Override
    public boolean isDataInvalid(byte[] pubKeyHash) {
        return !Arrays.equals(networkId.getPubKey().getHash(), pubKeyHash);
    }

    public boolean isDataInvalid() {
        return !Arrays.equals(proofOfWork.getPayload(), getPubKeyHash());
    }

    public byte[] getPubKeyHash() {
        return networkId.getPubKey().getHash();
    }

    public String getPubKeyAsHex() {
        return Hex.encode(networkId.getPubKey().getPublicKey().getEncoded());
    }

    public String getId() {
        return networkId.getPubKey().getId();
    }

    public String getNym() {
        if (nym == null) {
            nym = NymIdGenerator.generate(getPubKeyHash(), proofOfWork.getSolution());
        }
        return nym;
    }

    public ByteArray getProofOfBurnKey() {
        if (proofOfBurnHash == null) {
            // Must be compatible with Bisq 1 proofOfBurn input
            proofOfBurnHash = new ByteArray(DigestUtil.hash(getId().getBytes(Charsets.UTF_8)));
        }
        return proofOfBurnHash;
    }

    public ByteArray getBondedReputationKey() {
        if (bondedReputationHash == null) {
            bondedReputationHash = new ByteArray(DigestUtil.hash(getPubKeyHash()));
        }
        return bondedReputationHash;
    }

    public ByteArray getAccountAgeKey() {
        return new ByteArray(getId().getBytes(StandardCharsets.UTF_8));
    }

    public ByteArray getProfileAgeKey() {
        return new ByteArray(getId().getBytes(StandardCharsets.UTF_8));
    }

    public ByteArray getSignedWitnessKey() {
        return new ByteArray(getId().getBytes(StandardCharsets.UTF_8));
    }

    public String getTooltipString() {
        return Res.get("user.userProfile.tooltip",
                nickName, getNym(), getId(), getAddressByTransportDisplayString());
    }

    public String getUserName() {
        return UserNameLookup.getUserName(getNym(), nickName);
    }

    public String getAddressByTransportDisplayString() {
        return getAddressByTransportDisplayString(Integer.MAX_VALUE);
    }

    public String getAddressByTransportDisplayString(int maxAddressLength) {
        return Joiner.on("\n").join(networkId.getAddressByTransportTypeMap().entrySet().stream()
                .map(e -> Res.get("user.userProfile.addressByTransport." + e.getKey().name(),
                        StringUtils.truncate(e.getValue().getFullAddress(), maxAddressLength)))
                .collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "\r\n                    version='" + version + '\'' +
                ",\r\n                    nickName='" + nickName + '\'' +
                ",\r\n                    proofOfWork=" + proofOfWork +
                ",\r\n                    avatarVersion=" + avatarVersion +
                ",\r\n                    networkId=" + networkId +
                ",\r\n                    terms='" + terms + '\'' +
                ",\r\n                    statement='" + statement + '\'' +
                ",\r\n                    nym='" + nym + '\'' +
                ",\r\n                    proofOfBurnHash=" + proofOfBurnHash +
                ",\r\n                    bondedReputationHash=" + bondedReputationHash +
                ",\r\n                    applicationVersion=" + applicationVersion +
                "\r\n}";
    }
}
