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

package bisq.api.rest_api.endpoints.info;

import bisq.api.access.AllowUnauthenticated;
import bisq.api.dto.info.ApiInfoDto;
import bisq.api.rest_api.endpoints.config.ApiFeature;
import bisq.common.application.ApplicationVersion;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InfoRestApiTest {

    @Test
    void getInfoReturnsVersionAndAllFeatures() {
        ApiInfoDto dto = getInfo(Map.of());

        assertThat(dto.version()).isNotBlank().isEqualTo(ApplicationVersion.getVersion().toString());
        assertThat(dto.features()).containsExactlyElementsOf(ApiFeature.allKeys());
    }

    @Test
    void imageVersionIsAbsentWhenEnvIsMissingOrBlank() {
        assertThat(getInfo(Map.of()).imageVersion()).isNull();
        assertThat(getInfo(Map.of("BISQ_IMAGE_VERSION", "   ")).imageVersion()).isNull();
    }

    @Test
    void imageVersionIsTrimmedWhenSet() {
        assertThat(getInfo(Map.of("BISQ_IMAGE_VERSION", " 2.1.13.1 ")).imageVersion()).isEqualTo("2.1.13.1");
    }

    /**
     * imageVersion names the exact docker build, so /info keeps the guard of /settings/version, which
     * it replaces. RestPermissionMappingTest skips annotated resources, so it would not notice this.
     */
    @Test
    void infoRequiresASession() throws NoSuchMethodException {
        assertThat(InfoRestApi.class.isAnnotationPresent(AllowUnauthenticated.class)).isFalse();
        assertThat(InfoRestApi.class.getMethod("getInfo").isAnnotationPresent(AllowUnauthenticated.class)).isFalse();
    }

    private static ApiInfoDto getInfo(Map<String, String> env) {
        Response response = new InfoRestApi(env::get).getInfo();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        return (ApiInfoDto) response.getEntity();
    }
}
