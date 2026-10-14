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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageVersionTest {
    @Test
    void reportsNoImageVersionWhenNotSet() {
        assertThat(ImageVersion.resolve(Map.<String, String>of()::get)).isEmpty();
        assertThat(ImageVersion.resolve(Map.of("BISQ_IMAGE_VERSION", "")::get)).isEmpty();
        assertThat(ImageVersion.resolve(Map.of("BISQ_IMAGE_VERSION", "   ")::get)).isEmpty();
    }

    @Test
    void reportsGranularImageVersionWhenSet() {
        assertThat(ImageVersion.resolve(Map.of("BISQ_IMAGE_VERSION", "2.1.11.2")::get)).hasValue("2.1.11.2");
        assertThat(ImageVersion.resolve(Map.of("BISQ_IMAGE_VERSION", " 2.1.11.3-rc1 ")::get)).hasValue("2.1.11.3-rc1");
    }
}
