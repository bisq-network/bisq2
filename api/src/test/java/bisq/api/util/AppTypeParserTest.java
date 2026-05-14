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

package bisq.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static bisq.bonded_roles.release.AppType.DESKTOP;

class AppTypeParserTest {

    @Test
    @DisplayName("parse throws for null blank and empty values")
    void parse_throws_for_null_blank_and_empty_values() {
        assertThatThrownBy(() -> AppTypeParser.parse((String) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appType parameter is required");
        assertThatThrownBy(() -> AppTypeParser.parse("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appType parameter is required");
        assertThatThrownBy(() -> AppTypeParser.parse(Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appType parameter is required");
    }

    @Test
    @DisplayName("parse accepts case insensitive value")
    void parse_accepts_case_insensitive_value() {
        assertThat(AppTypeParser.parse("desktop")).isEqualTo(DESKTOP);
        assertThat(AppTypeParser.parse(Optional.of("desktop"))).isEqualTo(DESKTOP);
    }

    @Test
    @DisplayName("parse rejects invalid value")
    void parse_rejects_invalid_value() {
        assertThatThrownBy(() -> AppTypeParser.parse("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid appType: invalid");
    }
}