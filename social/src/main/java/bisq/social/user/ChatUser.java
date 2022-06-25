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

package bisq.social.user;

import bisq.common.encoding.Hex;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.i18n.Res;
import bisq.network.NetworkId;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.social.chat.messages.ChatMessage;
import bisq.social.chat.messages.PublicTradeChatMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Publicly shared chat user profile.
 */
@ToString
@EqualsAndHashCode
@Slf4j
@Getter
public class ChatUser implements DistributedData {
    // We give a bit longer TTL than the chat messages to ensure the chat user is available as long the messages are 
    private final static long TTL = Math.round(ChatMessage.TTL * 1.2);

    private final String nickName;
    private final ProofOfWork proofOfWork;
    private final NetworkId networkId;
    private final String terms;
    private final String bio;
    private final MetaData metaData;

    private transient final byte[] pubKeyHash;
    private transient final String id;
    private transient final String nym;

    public ChatUser(String nickName,
                    ProofOfWork proofOfWork,
                    NetworkId networkId,
                    String terms,
                    String bio) {
        this(nickName,
                proofOfWork,
                networkId,
                terms,
                bio,
                new MetaData(TTL, 100000, PublicTradeChatMessage.class.getSimpleName()));
    }

    public ChatUser(String nickName,
                    ProofOfWork proofOfWork,
                    NetworkId networkId,
                    String terms,
                    String bio,
                    MetaData metaData) {
        this.nickName = nickName;
        this.proofOfWork = proofOfWork;
        this.networkId = networkId;
        this.terms = terms;
        this.bio = bio;
        this.metaData = metaData;

        pubKeyHash = DigestUtil.hash(networkId.getPubKey().publicKey().getEncoded());
        id = Hex.encode(pubKeyHash);
        nym = NymIdGenerator.fromHash(proofOfWork.getPayload());
        
        log.error("ChatUser {} {} {}", nickName, terms,bio);
    }

    public static ChatUser from(ChatUser chatUser, String terms, String bio) {
        return new ChatUser(chatUser.getNickName(), chatUser.getProofOfWork(), chatUser.getNetworkId(), terms, bio);
    }

    public bisq.social.protobuf.ChatUser toProto() {
        return bisq.social.protobuf.ChatUser.newBuilder()
                .setNickName(nickName)
                .setTerms(terms)
                .setBio(bio)
                .setProofOfWork(proofOfWork.toProto())
                .setNetworkId(networkId.toProto())
                .setMetaData(metaData.toProto())
                .build();
    }

    public static ChatUser fromProto(bisq.social.protobuf.ChatUser proto) {
        return new ChatUser(proto.getNickName(),
                ProofOfWork.fromProto(proto.getProofOfWork()),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getTerms(),
                proto.getBio(),
                MetaData.fromProto(proto.getMetaData()));
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.social.protobuf.ChatUser.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }


    public String getTooltipString() {
        return Res.get("social.chatUser.tooltip", nickName, nym);
    }

    public String getUserName() {
        return NymLookup.getUserName(nym, nickName);
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        //todo
        return false;
    }

    //todo
    public String getBurnScoreAsString() {
        return "301"; //todo implement instead of hardcode
    }

    //todo
    public String getAccountAgeAsString() {
        return "274 days"; //todo implement instead of hardcode
    }

   /* public static record BurnInfo(long totalBsqBurned, long firstBurnDate) {
    }*/
}