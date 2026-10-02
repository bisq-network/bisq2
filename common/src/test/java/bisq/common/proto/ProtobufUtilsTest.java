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

import com.google.protobuf.StringValue;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProtobufUtilsTest {
    // Fails with the checked exception for "invalid", and with a RuntimeException of the converter itself for "cast".
    private static final ProtoConverter<StringValue, String> CONVERTER = proto -> {
        if (proto.getValue().equals("invalid")) {
            throw new UnresolvableProtobufException("invalid entry");
        }
        if (proto.getValue().equals("cast")) {
            throw new ClassCastException("wrong type");
        }
        return proto.getValue().toUpperCase();
    };

    @Test
    void fromProtoStreamSkippingInvalidKeepsTheOtherEntriesInTheirOrder() {
        List<StringValue> protos = List.of(of("a"), of("invalid"), of("b"), of("cast"), of("c"));

        List<String> results = ProtobufUtils.fromProtoStreamSkippingInvalid(protos, CONVERTER)
                .collect(Collectors.toList());

        assertThat(results).containsExactly("A", "B", "C");
    }

    @Test
    void fromProtoStreamSkippingInvalidSkipsMoreEntriesThanItLogs() {
        List<StringValue> protos = List.of(of("invalid"), of("invalid"), of("invalid"), of("invalid"), of("a"));

        assertThat(ProtobufUtils.fromProtoStreamSkippingInvalid(protos, CONVERTER)).containsExactly("A");
    }

    @Test
    void fromProtoEntryStreamSkippingInvalidKeepsTheKeysOfTheOtherEntries() {
        Map<String, StringValue> protos = new LinkedHashMap<>();
        protos.put("1", of("a"));
        protos.put("2", of("invalid"));
        protos.put("3", of("cast"));
        protos.put("4", of("b"));

        Map<String, String> results = ProtobufUtils.fromProtoEntryStreamSkippingInvalid(protos, CONVERTER)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(results).containsExactlyInAnyOrderEntriesOf(Map.of("1", "A", "4", "B"));
    }

    private static StringValue of(String value) {
        return StringValue.of(value);
    }
}
