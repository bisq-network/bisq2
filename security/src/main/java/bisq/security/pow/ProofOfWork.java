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

import bisq.common.encoding.Hex;
import bisq.common.proto.Proto;
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
@Getter
@EqualsAndHashCode
public final class ProofOfWork implements Proto {
    // payload is usually the pubKeyHash
    private final byte[] payload;       // message of 1000 chars has about 1300 bytes
    // If challenge does not make sense we set it null
    @Nullable
    private final byte[] challenge; // 15 or 16 bytes
    private final double difficulty;
    private final byte[] solution; // 72 bytes

    public ProofOfWork(byte[] payload,
                       @Nullable byte[] challenge,
                       double difficulty,
                       byte[] solution) {
        this.payload = payload;
        this.challenge = challenge;
        this.difficulty = difficulty;
        this.solution = solution;

        NetworkDataValidation.validateByteArray(payload, 20_000);
        if (challenge != null) {
            NetworkDataValidation.validateByteArray(challenge, 20);
        }
        NetworkDataValidation.validateByteArray(solution, 75);
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
        return "ProofOfWork(" +
                "payload=" + Hex.encode(payload) +
                ", challenge=" + (challenge == null ? "null" : Hex.encode(challenge)) +
                ", difficulty=" + difficulty +
                ", solution=" + Hex.encode(solution) +
                ")";
    }
}
