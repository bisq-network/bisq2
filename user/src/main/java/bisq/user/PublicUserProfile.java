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

package bisq.user;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.pow.ProofOfWork;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Publicly shared chat user profile.
 */
@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public final class PublicUserProfile implements DistributedData {
    // We give a bit longer TTL than the chat messages to ensure the chat user is available as long the messages are 
    private final static long TTL = TimeUnit.HOURS.toMillis(30);
    // Metadata are not sent over the wire but hardcoded as we want to control it by ourselves.
    private final static MetaData META_DATA = new MetaData(TTL, 100000, PublicUserProfile.class.getSimpleName());

    public static PublicUserProfile from(PublicUserProfile publicUserProfile, String terms, String bio) {
        return new PublicUserProfile(publicUserProfile.getNickName(), publicUserProfile.getProofOfWork(), publicUserProfile.getNetworkId(), terms, bio);
    }

    private final String nickName;
    // We need the proofOfWork for verification of the nym and robohash icon
    private final ProofOfWork proofOfWork;
    private final NetworkId networkId;
    private final String terms;
    private final String bio;

    private transient String nym;

    public PublicUserProfile(String nickName,
                             ProofOfWork proofOfWork,
                             NetworkId networkId,
                             String terms,
                             String bio) {
        this.nickName = nickName;
        this.proofOfWork = proofOfWork;
        this.networkId = networkId;
        this.terms = terms;
        this.bio = bio;
    }

    @Override
    public bisq.user.protobuf.PublicUserProfile toProto() {
        return bisq.user.protobuf.PublicUserProfile.newBuilder()
                .setNickName(nickName)
                .setTerms(terms)
                .setBio(bio)
                .setProofOfWork(proofOfWork.toProto())
                .setNetworkId(networkId.toProto())
                .build();
    }

    public static PublicUserProfile fromProto(bisq.user.protobuf.PublicUserProfile proto) {
        return new PublicUserProfile(proto.getNickName(),
                ProofOfWork.fromProto(proto.getProofOfWork()),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getTerms(),
                proto.getBio());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.user.protobuf.PublicUserProfile.class));
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
    public boolean isDataInvalid() {
        return !Arrays.equals(proofOfWork.getPayload(), getPubKeyHash());
    }

    public byte[] getPubKeyHash() {
        return networkId.getPubKey().getHash();
    }

    public String getId() {
        return networkId.getPubKey().getId();
    }

    public String getNym() {
        if (nym == null) {
            nym = NymIdGenerator.fromHash(getPubKeyHash());
        }
        return nym;
    }

    public String getTooltipString() {
        return Res.get("social.chatUser.tooltip", nickName, nym);
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }


    //todo
    public String getBurnScoreAsString() {
        return "301"; //todo implement instead of hardcode
    }

    //todo
    public String getAccountAgeAsString() {
        return "274 days"; //todo implement instead of hardcode
    }
}