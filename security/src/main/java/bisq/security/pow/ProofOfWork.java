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
import bisq.common.validation.NetworkDataValidation;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Optional;

// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
@Getter
@EqualsAndHashCode
public final class ProofOfWork implements Proto {
    // payload is usually the pubKeyHash
    private final byte[] payload;       // message of 1000 chars has about 1300 bytes
    private final long counter;
    // If challenge does not make sense we set it null
    // Challenge need to be hashed to 256 bits
    @Nullable
    private final byte[] challenge; // 32 bytes
    private final double difficulty;
    private final byte[] solution; // 72 bytes
    private final long duration;

    public ProofOfWork(byte[] payload,
                       long counter,
                       @Nullable byte[] challenge,
                       double difficulty,
                       byte[] solution,
                       long duration) {
        this.payload = payload;
        this.counter = counter;
        this.challenge = challenge;
        this.difficulty = difficulty;
        this.solution = solution;
        this.duration = duration;

        NetworkDataValidation.validateByteArray(payload, 20_000);
        if (challenge != null) {
            NetworkDataValidation.validateByteArray(challenge, 32);
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
                .setCounter(counter)
                .setDifficulty(difficulty)
                .setSolution(ByteString.copyFrom(solution))
                .setDuration(duration);

        Optional.ofNullable(challenge).ifPresent(challenge -> builder.setChallenge(ByteString.copyFrom(challenge)));
        return builder.build();
    }

    public static ProofOfWork fromProto(bisq.security.protobuf.ProofOfWork proto) {
        return new ProofOfWork(
                proto.getPayload().toByteArray(),
                proto.getCounter(),
                proto.getChallenge().isEmpty() ? null : proto.getChallenge().toByteArray(),
                proto.getDifficulty(),
                proto.getSolution().toByteArray(),
                proto.getDuration()
        );
    }

    @Override
    public String toString() {
        return "ProofOfWork{" +
                "duration=" + duration +
                ", difficulty=" + difficulty +
                ", counter=" + counter +
                ", challenge=" + Arrays.toString(challenge) +
                ", solution=" + Arrays.toString(solution) +
                ", payload=" + Arrays.toString(payload) +
                '}';
    }
}
