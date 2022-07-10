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

package bisq.security.pow;

import bisq.common.proto.Proto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Optional;

// Borrowed from: https://github.com/bisq-network/bisq
@Getter
@EqualsAndHashCode
public final class ProofOfWork implements Proto {
    // payload is usually the pubKeyHash
    private final byte[] payload;
    // If challenge does not make sense we set it null
    @Nullable
    private final byte[] challenge;
    private final double difficulty;
    private final byte[] solution;

    public ProofOfWork(byte[] payload,
                       byte[] challenge,
                       @Nullable double difficulty,
                       byte[] solution) {
        this.payload = payload;
        this.challenge = challenge;
        this.difficulty = difficulty;
        this.solution = solution;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public bisq.security.protobuf.ProofOfWork toProto() {
        bisq.security.protobuf.ProofOfWork.Builder builder = bisq.security.protobuf.ProofOfWork.newBuilder()
                .setPayload(ByteString.copyFrom(payload))
                .setDifficulty(difficulty)
                .setSolution(ByteString.copyFrom(solution));

        Optional.ofNullable(challenge).ifPresent(challenge -> builder.setChallenge(ByteString.copyFrom(challenge)));
        return builder.build();
    }

    public static ProofOfWork fromProto(bisq.security.protobuf.ProofOfWork proto) {
        return new ProofOfWork(
                proto.getPayload().toByteArray(),
                proto.getChallenge().isEmpty() ? null : proto.getChallenge().toByteArray(),
                proto.getDifficulty(),
                proto.getSolution().toByteArray()
        );
    }

    @Override
    public String toString() {
        return "ProofOfWork{" +
                ",\r\n     difficulty=" + difficulty +
                "\r\n}";
    }
}
