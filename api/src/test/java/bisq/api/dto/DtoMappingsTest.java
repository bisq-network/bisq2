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

package bisq.api.dto;

import bisq.api.dto.mappings.alert.AuthorizedAlertDataDtoMapping;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoMappingsTest {

    @Test
    void authorizedAlertDataMappingRejectsUnsupportedAlertTypes() {
        AuthorizedAlertData banAlert = new AuthorizedAlertData(
                "ban-alert",
                System.currentTimeMillis(),
                AlertType.BAN,
                Optional.empty(),
                Optional.of("Message ban-alert"),
                false,
                false,
                Optional.empty(),
                Optional.empty(),
                "0123456789abcdef0123456789abcdef01234567",
                false,
                Optional.empty(),
                AppType.MOBILE_CLIENT
        );

        assertThatThrownBy(() -> AuthorizedAlertDataDtoMapping.fromBisq2Model(banAlert))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AuthorizedAlertDataDto does not support alertType: BAN");
    }
}