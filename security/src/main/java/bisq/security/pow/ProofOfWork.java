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

// Taken from: https://github.com/bisq-network/bisq
@Getter
@EqualsAndHashCode
public final class ProofOfWork implements Proto {
    private final byte[] payload;
    private final long counter;
    private final byte[] challenge;
    private final double difficulty;
    private final long duration;
    private final byte[] solution;

    public ProofOfWork(byte[] payload,
                       long counter,
                       byte[] challenge,
                       double difficulty,
                       long duration,
                       byte[] solution) {
        this.payload = payload;
        this.counter = counter;
        this.challenge = challenge;
        this.difficulty = difficulty;
        this.duration = duration;
        this.solution = solution;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public bisq.security.protobuf.ProofOfWork toProto() {
        return bisq.security.protobuf.ProofOfWork.newBuilder()
                .setPayload(ByteString.copyFrom(payload))
                .setCounter(counter)
                .setChallenge(ByteString.copyFrom(challenge))
                .setDifficulty(difficulty)
                .setDuration(duration)
                .setSolution(ByteString.copyFrom(solution))
                .build();
    }

    public static ProofOfWork fromProto(bisq.security.protobuf.ProofOfWork proto) {
        return new ProofOfWork(
                proto.getPayload().toByteArray(),
                proto.getCounter(),
                proto.getChallenge().toByteArray(),
                proto.getDifficulty(),
                proto.getDuration(),
                proto.getSolution().toByteArray()
        );
    }

    @Override
    public String toString() {
        return "ProofOfWork{" +
                ",\r\n     counter=" + counter +
                ",\r\n     difficulty=" + difficulty +
                ",\r\n     duration=" + duration +
                "\r\n}";
    }
}
