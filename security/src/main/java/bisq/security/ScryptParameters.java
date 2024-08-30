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

package bisq.security;

import bisq.common.proto.PersistableProto;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@EqualsAndHashCode
@ToString
public class ScryptParameters implements PersistableProto {
    public static final int KEY_LENGTH = 32; // 256 bits.

    private final byte[] salt;
    private final int cost;
    private final int blockSize;
    private final int parallelization;
    private final int keyLength;

    public ScryptParameters(byte[] salt) {
        this(salt, 16384);
    }

    public ScryptParameters(byte[] salt, int cost) {
        this(salt, cost, 8, 1, KEY_LENGTH);
    }

    /**
     * @param salt            the salt to use for this invocation.
     * @param cost            CPU/Memory cost parameter. Must be larger than 1, a power of 2 and less than
     *                        <code>2^(128 * r / 8)</code>.
     * @param blockSize       the block size, must be &gt;= 1.
     * @param parallelization Parallelization parameter. Must be a positive integer less than or equal to
     *                        <code>Integer.MAX_VALUE / (128 * r * 8)</code>.
     * @param keyLength       the length of the key to generate.
     */
    public ScryptParameters(byte[] salt, int cost, int blockSize, int parallelization, int keyLength) {
        this.salt = salt != null ? salt : new byte[0];
        this.cost = cost;
        this.blockSize = blockSize;
        this.parallelization = parallelization;
        this.keyLength = keyLength;

        if (this.salt.length == 0) {
            log.warn("You are using a ScryptParameters with no salt. Your encryption may be vulnerable to a dictionary attack.");
        }
    }

    @Override
    public bisq.security.protobuf.ScryptParameters toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.security.protobuf.ScryptParameters.Builder getBuilder(boolean serializeForHash) {
        return bisq.security.protobuf.ScryptParameters.newBuilder()
                .setSalt(ByteString.copyFrom(salt))
                .setCost(cost)
                .setBlockSize(blockSize)
                .setParallelization(parallelization)
                .setKeyLength(keyLength);
    }

    public static ScryptParameters fromProto(bisq.security.protobuf.ScryptParameters proto) {
        return new ScryptParameters(proto.getSalt().toByteArray(),
                proto.getCost(),
                proto.getBlockSize(),
                proto.getParallelization(),
                proto.getKeyLength());
    }
}