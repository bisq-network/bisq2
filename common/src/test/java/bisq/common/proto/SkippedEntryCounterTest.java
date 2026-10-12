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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkippedEntryCounterTest {
    private static final ProtoConverter<String, Integer> CONVERTER = proto -> {
        try {
            return Integer.parseInt(proto);
        } catch (NumberFormatException e) {
            throw new UnresolvableProtobufException(e);
        }
    };

    @Test
    void countsTheEntriesWhichBothHelpersSkip() {
        try (SkippedEntryCounter counter = SkippedEntryCounter.open()) {
            assertEquals(2, ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("1", "x", "3"), CONVERTER).count());
            assertEquals(1, ProtobufUtils.fromProtoEntryStreamSkippingInvalid(Map.of("a", "y", "b", "2"), CONVERTER)
                    .count());

            assertEquals(2, counter.getNumSkippedEntries());
        }
    }

    @Test
    void countsNothingWhenNoEntryIsSkipped() {
        try (SkippedEntryCounter counter = SkippedEntryCounter.open()) {
            ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("1", "2"), CONVERTER).count();

            assertEquals(0, counter.getNumSkippedEntries());
        }
    }

    @Test
    void anInnerCounterCountsAloneAndTheOuterCounterResumesAfterItCloses() {
        try (SkippedEntryCounter outer = SkippedEntryCounter.open()) {
            ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("x"), CONVERTER).count();
            try (SkippedEntryCounter inner = SkippedEntryCounter.open()) {
                ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("y", "z"), CONVERTER).count();
                assertEquals(2, inner.getNumSkippedEntries());
            }
            ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("w"), CONVERTER).count();

            assertEquals(2, outer.getNumSkippedEntries());
        }
    }

    @Test
    void skipsOnAnotherThreadAreNotCounted() {
        try (SkippedEntryCounter counter = SkippedEntryCounter.open()) {
            CompletableFuture.runAsync(() ->
                    ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("x"), CONVERTER).count()).join();

            assertEquals(0, counter.getNumSkippedEntries());
        }
    }

    @Test
    void skipsAfterTheCounterClosedAreNotCounted() {
        SkippedEntryCounter counter = SkippedEntryCounter.open();
        counter.close();
        ProtobufUtils.fromProtoStreamSkippingInvalid(List.of("x"), CONVERTER).count();

        assertEquals(0, counter.getNumSkippedEntries());
    }
}
