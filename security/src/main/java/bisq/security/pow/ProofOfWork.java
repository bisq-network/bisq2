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
import bisq.common.proto.NetworkProto;
import bisq.common.util.MathUtils;
import bisq.common.validation.NetworkDataValidation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.protobuf.ByteString;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;

// TODO (refactor, low prio) use Optional instead of Nullable
// Borrowed from: https://github.com/bisq-network/bisq
@Slf4j
@Getter
@EqualsAndHashCode
public final class ProofOfWork implements NetworkProto {
    // With HashCashV2 we use the 20 byte hash of the serialized message for the payload
    // instead the serialized message as in HashCash(V1)
    @Schema(
            description = "payload as byte array field represented as a JSON array of integers",
            type = "array",
            example = "[-80, -19, -60, 119, -20, -106, 115, 121, -122, 122, -28, 75, 30, 3, 15, -92, -8, -26, -125, 39]"
    )
    private final byte[] payload;
    private final long counter;
    // If challenge does not make sense we set it null
    // Challenge need to be hashed to 256 bits
    @Nullable
    @Schema(
            description = "payload as byte array field represented as a JSON array of integers",
            type = "array",
            example = "[-20, -106, 115, 121, -122, 122]"
    )
    private final byte[] challenge; // 32 bytes
    private final double difficulty;
    @Schema(
            description = "payload as byte array field represented as a JSON array of integers",
            type = "array",
            example = "[0, 0, 0, 0, 0, 1, 108, 27]"
    )
    private final byte[] solution; // 72 bytes
    private final long duration;

    @JsonCreator
    public ProofOfWork(@JsonProperty("payload") byte[] payload,
                       @JsonProperty("counter") long counter,
                       @JsonProperty("challenge") @Nullable byte[] challenge,
                       @JsonProperty("difficulty") double difficulty,
                       @JsonProperty("solution") byte[] solution,
                       @JsonProperty("duration") long duration) {
        this.payload = payload;
        this.counter = counter;
        this.challenge = challenge;
        this.difficulty = difficulty;
        this.solution = solution;
        this.duration = duration;

        verify();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void verify() {
        // We have to allow a large size here as InventoryData can be large
        NetworkDataValidation.validateByteArray(payload, 10_000_000);
        if (challenge != null) {
            NetworkDataValidation.validateByteArray(challenge, 32);
        }
        NetworkDataValidation.validateByteArray(solution, 75);
    }

    @Override
    public bisq.security.protobuf.ProofOfWork toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.ProofOfWork.Builder getBuilder(boolean serializeForHash) {
        bisq.security.protobuf.ProofOfWork.Builder builder = bisq.security.protobuf.ProofOfWork.newBuilder()
                .setPayload(ByteString.copyFrom(payload))
                .setCounter(counter)
                .setDifficulty(difficulty)
                .setSolution(ByteString.copyFrom(solution))
                .setDuration(duration);
        Optional.ofNullable(challenge).ifPresent(challenge -> builder.setChallenge(ByteString.copyFrom(challenge)));
        return builder;
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

    @JsonIgnore
    public double getLog2Difficulty() {
        return MathUtils.roundDouble(Math.log(difficulty) / MathUtils.LOG2, 2);
    }

    @Override
    public String toString() {
        return "ProofOfWork{" +
                "\nduration=" + duration +
                "\ndifficulty=2^" + getLog2Difficulty() + " = " + difficulty +
                "\ncounter=" + counter +
                "\nchallenge=" + (challenge != null ? Hex.encode(challenge) : "null") +
                "\nsolution=" + Hex.encode(solution) +
                "\npayload=" + Hex.encode(payload) +
                '}';
    }
}
