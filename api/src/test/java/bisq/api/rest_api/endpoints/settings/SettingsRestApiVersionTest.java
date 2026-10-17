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

package bisq.api.rest_api.endpoints.settings;

import bisq.api.dto.settings.SettingsApiVersionDto;
import bisq.common.application.ApplicationVersion;
import bisq.common.json.JsonMapperProvider;
import bisq.settings.SettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// The blank/trim rules are covered by ImageVersionTest; these guard what deployed clients read here.
class SettingsRestApiVersionTest {
    @Test
    void reportsPlainCoreVersion() {
        assertThat(getVersion(Map.of()).version()).isEqualTo(ApplicationVersion.getVersion().toString());
    }

    // Deployed clients key their config caches on it, so dropping it here would go unnoticed.
    @Test
    void reportsImageVersionWhenSet() {
        assertThat(getVersion(Map.of("BISQ_IMAGE_VERSION", " 2.1.13.0 ")).imageVersion()).hasValue("2.1.13.0");
    }

    @Test
    void reportsNoImageVersionWhenNotSet() {
        assertThat(getVersion(Map.of()).imageVersion()).isEmpty();
    }

    // Released versions omit the key when no image version is set; an empty Optional must not write null.
    @Test
    void imageVersionKeyIsOmittedFromJsonWhenNotSet() {
        assertThat(toJson(getVersion(Map.of())).has("imageVersion")).isFalse();
        assertThat(toJson(getVersion(Map.of("BISQ_IMAGE_VERSION", "2.1.13.0"))).get("imageVersion").asText())
                .isEqualTo("2.1.13.0");
    }

    private static SettingsApiVersionDto getVersion(Map<String, String> env) {
        Response response = new SettingsRestApi(mock(SettingsService.class), env::get).getVersion();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return (SettingsApiVersionDto) response.getEntity();
    }

    private static JsonNode toJson(SettingsApiVersionDto dto) {
        return JsonMapperProvider.get().valueToTree(dto);
    }
}
