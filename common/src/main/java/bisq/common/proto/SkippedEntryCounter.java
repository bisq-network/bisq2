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

package bisq.common.proto;

import javax.annotation.Nullable;

/**
 * Counts the entries which {@link ProtobufUtils#fromProtoStreamSkippingInvalid} and
 * {@link ProtobufUtils#fromProtoEntryStreamSkippingInvalid} skip on the current thread while the counter is open.
 * <p>
 * The persistence layer opens a counter around the parsing of a store file. So it learns whether the parsed store lost
 * entries, without a change to each store. Skipped entries of parsing on another thread are not counted.
 */
public final class SkippedEntryCounter implements AutoCloseable {
    private static final ThreadLocal<SkippedEntryCounter> CURRENT = new ThreadLocal<>();

    @Nullable
    private final SkippedEntryCounter previous;
    private int numSkippedEntries;

    private SkippedEntryCounter(@Nullable SkippedEntryCounter previous) {
        this.previous = previous;
    }

    /**
     * Opens a counter on the current thread. It counts until {@link #close()}. A counter which is opened while another
     * one is open counts alone, and the other one counts again after the close.
     */
    public static SkippedEntryCounter open() {
        SkippedEntryCounter counter = new SkippedEntryCounter(CURRENT.get());
        CURRENT.set(counter);
        return counter;
    }

    static void countSkippedEntry() {
        SkippedEntryCounter counter = CURRENT.get();
        if (counter != null) {
            counter.numSkippedEntries++;
        }
    }

    public int getNumSkippedEntries() {
        return numSkippedEntries;
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
