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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtobufUtilsFromProtoStreamTest {
    private static final ProtoConverter<String, Integer> CONVERTER = proto -> {
        try {
            return Integer.parseInt(proto);
        } catch (NumberFormatException e) {
            throw new UnresolvableProtobufException(e);
        }
    };

    @Test
    void fromProtoStreamFailsAtTheFirstInvalidProto() {
        assertThrows(UnresolvableProtobufException.class,
                () -> ProtobufUtils.fromProtoStream(List.of("1", "x", "3"), CONVERTER));
    }

    @Test
    void fromProtoStreamSkippingFailuresKeepsTheOtherProtosInTheirOrder() {
        List<UnresolvableProtobufException> failures = new ArrayList<>();

        List<Integer> results = ProtobufUtils.fromProtoStreamSkippingFailures(List.of("1", "x", "3", "y"), CONVERTER,
                        failures::add)
                .collect(Collectors.toList());

        assertEquals(List.of(1, 3), results);
        assertEquals(2, failures.size());
    }
}
