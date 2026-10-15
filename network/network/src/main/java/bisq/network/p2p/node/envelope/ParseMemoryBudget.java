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

package bisq.network.p2p.node.envelope;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.message.NetworkEnvelope;
import lombok.Getter;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.Semaphore;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Limits the memory of the messages which the connections of a node parse at the same time (see rule 11 of
 * docs/specifications/network/message-framing.md). The parsed objects of a message can need about 22 times its bytes.
 * The size limit bounds one message, but several connections can parse large messages at the same time.
 * <p>
 * The budget counts the bytes of the messages which are being parsed. Established connections account for messages
 * of at least {@link #MIN_ACCOUNTED_SIZE} bytes. Handshakes use a separate, smaller budget and account for every
 * message, so that unauthenticated peers cannot bypass their budget with messages below the threshold.
 * <p>
 * The permits are held while the caller parses and converts the message. The caller must not keep a reference to the
 * parsed protobuf objects after it returns, so the budget covers their whole life. The caller must not wait for other
 * connections while it holds the permits, because their read threads can wait for the same budget.
 */
public class ParseMemoryBudget {
    public static final int MIN_ACCOUNTED_SIZE = 64 * 1024;
    // Two messages at the size limit. Their parse needs about 2 * 93 MB in the worst case (estimate).
    public static final int DEFAULT_BUDGET = 2 * NetworkEnvelope.MAX_SERIALIZED_SIZE;
    public static final int HANDSHAKE_BUDGET = 2 * NetworkEnvelope.MAX_HANDSHAKE_SERIALIZED_SIZE;

    @FunctionalInterface
    public interface Parser<T> {
        T parse(byte[] message) throws IOException, UnresolvableProtobufException;
    }

    @Getter
    private final int budget;
    private final int minAccountedSize;
    private final Semaphore semaphore;

    public static ParseMemoryBudget forHandshakes() {
        return new ParseMemoryBudget(HANDSHAKE_BUDGET, NetworkEnvelope.MAX_HANDSHAKE_SERIALIZED_SIZE, 0);
    }

    public ParseMemoryBudget() {
        this(DEFAULT_BUDGET);
    }

    public ParseMemoryBudget(int budget) {
        this(budget, NetworkEnvelope.MAX_SERIALIZED_SIZE, MIN_ACCOUNTED_SIZE);
    }

    private ParseMemoryBudget(int budget, int maxMessageSize, int minAccountedSize) {
        // A smaller budget would make a message at the size limit wait forever
        checkArgument(budget >= maxMessageSize,
                "The budget of %s bytes is smaller than the size limit of a message", budget);
        this.budget = budget;
        this.minAccountedSize = minAccountedSize;
        // Fair, so that a large message is not overtaken by smaller ones without end
        this.semaphore = new Semaphore(budget, true);
    }

    public <T> T parse(byte[] message, Parser<T> parser) throws IOException, UnresolvableProtobufException {
        if (message.length < minAccountedSize) {
            return parser.parse(message);
        }
        checkArgument(message.length <= budget, "The message of %s bytes exceeds the budget", message.length);
        try {
            semaphore.acquire(message.length);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for the parse memory budget");
        }
        try {
            return parser.parse(message);
        } finally {
            semaphore.release(message.length);
        }
    }

    public int getAvailableBytes() {
        return semaphore.availablePermits();
    }
}
