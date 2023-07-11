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

import bisq.common.data.ByteArray;
import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.user.NymIdGenerator;
import com.google.common.base.Charsets;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Publicly shared user profile (from other peers or mine).
 */
@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public final class UserProfile implements DistributedData {
    // We give a bit longer TTL than the chat messages to ensure the chat user is available as long the messages are 
    private final static long TTL = TimeUnit.DAYS.toMillis(1);
    // Metadata are not sent over the wire but hardcoded as we want to control it by ourselves.
    private final static MetaData META_DATA = new MetaData(TTL, 100000, UserProfile.class.getSimpleName());

    public static UserProfile from(UserProfile userProfile, String terms, String statement) {
        return new UserProfile(userProfile.getNickName(), userProfile.getProofOfWork(), userProfile.getNetworkId(), terms, statement);
    }

    private final String nickName;
    // We need the proofOfWork for verification of the nym and robohash icon
    private final ProofOfWork proofOfWork;
    private final NetworkId networkId;
    private final String terms;
    private final String statement;

    private transient String nym;
    private transient ByteArray proofOfBurnHash;
    private transient ByteArray bondedReputationHash;

    public UserProfile(String nickName,
                       ProofOfWork proofOfWork,
                       NetworkId networkId,
                       String terms,
                       String statement) {
        this.nickName = nickName.trim().isEmpty() ? "Invalid nickname" :
                nickName.trim();
        this.proofOfWork = proofOfWork;
        this.networkId = networkId;
        this.terms = terms;
        this.statement = statement;
    }

    @Override
    public bisq.user.protobuf.UserProfile toProto() {
        return bisq.user.protobuf.UserProfile.newBuilder()
                .setNickName(nickName)
                .setTerms(terms)
                .setStatement(statement)
                .setProofOfWork(proofOfWork.toProto())
                .setNetworkId(networkId.toProto())
                .build();
    }

    public static UserProfile fromProto(bisq.user.protobuf.UserProfile proto) {
        return new UserProfile(proto.getNickName(),
                ProofOfWork.fromProto(proto.getProofOfWork()),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getTerms(),
                proto.getStatement());
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
    public MetaData getMetaData() {
        return META_DATA;
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

    public String getNodeId() {
        return networkId.getNodeId();
    }

    public String getNym() {
        if (nym == null) {
            nym = NymIdGenerator.fromHash(getPubKeyHash());
        }
        return nym;
    }

    public ByteArray getProofOfBurnKey() {
        if (proofOfBurnHash == null) {
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
        return Res.get("user.userProfile.tooltip", nickName, getNym(), getId());
    }

    public String getUserName() {
        return UserNameLookup.getUserName(getNym(), nickName);
    }
}