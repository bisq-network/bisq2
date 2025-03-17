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

package bisq.network.p2p.node.authorization.token.hash_cash_v2;

import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenType;
import bisq.security.pow.ProofOfWork;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public final class HashCashV2Token extends AuthorizationToken {
    private final ProofOfWork proofOfWork;
    private final int messageCounter;

    public HashCashV2Token(ProofOfWork proofOfWork, int messageCounter) {
        this(AuthorizationTokenType.HASH_CASH_V2, proofOfWork, messageCounter);
    }

    private HashCashV2Token(AuthorizationTokenType authorizationTokenType, ProofOfWork proofOfWork, int messageCounter) {
        super(authorizationTokenType);

        this.proofOfWork = proofOfWork;
        this.messageCounter = messageCounter;

        verify();
    }

    @Override
    public void verify() {
        checkArgument(authorizationTokenType == AuthorizationTokenType.HASH_CASH_V2);
    }

    @Override
    public bisq.network.protobuf.AuthorizationToken toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AuthorizationToken.Builder getBuilder(boolean serializeForHash) {
        return getAuthorizationTokenBuilder().setHashCashV2Token(
                bisq.network.protobuf.HashCashV2Token.newBuilder()
                        .setProofOfWork(proofOfWork.toProto(serializeForHash))
                        .setMessageCounter(messageCounter));
    }

    public static HashCashV2Token fromProto(bisq.network.protobuf.AuthorizationToken proto) {
        bisq.network.protobuf.HashCashV2Token hashCashV2TokenProto = proto.getHashCashV2Token();
        return new HashCashV2Token(AuthorizationTokenType.fromProto(proto.getAuthorizationTokenType()),
                ProofOfWork.fromProto(hashCashV2TokenProto.getProofOfWork()),
                hashCashV2TokenProto.getMessageCounter());
    }
}
