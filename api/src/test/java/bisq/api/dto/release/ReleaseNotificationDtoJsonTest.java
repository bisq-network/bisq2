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

package bisq.api.dto.release;

import bisq.bonded_roles.release.AppType;
import bisq.common.json.JsonMapperProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the wire shape clients parse, through the mapper the WebSocket services serialize with. The
 * one key at risk is {@code isPreRelease}: Jackson's handling of an {@code is}-prefixed boolean
 * record component has changed between versions, and a stripped {@code preRelease} would silently
 * read as a regular release on the client.
 */
class ReleaseNotificationDtoJsonTest {
    private static final long DATE = 1_700_000_000_000L;

    @Test
    void serializesExactlyTheWireKeysWithTheirTypes() throws Exception {
        ReleaseNotificationDto dto = new ReleaseNotificationDto("id-1",
                DATE,
                true,
                "Release notes",
                "2.1.13",
                AppType.MOBILE_CLIENT);

        JsonNode json = JsonMapperProvider.get().readTree(JsonMapperProvider.get().writeValueAsString(dto));

        List<String> keys = new ArrayList<>();
        json.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder("id", "date", "isPreRelease", "releaseNotes", "versionString", "appType");
        assertThat(json.get("id").isTextual()).isTrue();
        assertThat(json.get("date").isIntegralNumber()).isTrue();
        assertThat(json.get("date").asLong()).isEqualTo(DATE);
        assertThat(json.get("isPreRelease").isBoolean()).isTrue();
        assertThat(json.get("isPreRelease").asBoolean()).isTrue();
        assertThat(json.get("releaseNotes").isTextual()).isTrue();
        assertThat(json.get("versionString").asText()).isEqualTo("2.1.13");
        assertThat(json.get("appType").asText()).isEqualTo("MOBILE_CLIENT");
    }
}
