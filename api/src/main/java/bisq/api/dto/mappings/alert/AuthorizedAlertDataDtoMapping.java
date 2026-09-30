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

package bisq.api.dto.mappings.alert;

import bisq.api.dto.alert.AuthorizedAlertDataDto;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;

public class AuthorizedAlertDataDtoMapping {
    public static boolean canRepresent(AuthorizedAlertData value) {
        return AlertType.isMessageAlert(value.getAlertType());
    }

    public static AuthorizedAlertDataDto fromBisq2Model(AuthorizedAlertData value) {
        if (!canRepresent(value)) {
            throw new IllegalArgumentException("AuthorizedAlertDataDto does not support alertType: " + value.getAlertType());
        }
        return new AuthorizedAlertDataDto(
                value.getId(),
                value.getDate(),
                value.getAlertType(),
                value.getHeadline(),
                value.getMessage(),
                value.isHaltTrading(),
                value.isRequireVersionForTrading(),
                value.getMinVersion(),
                value.getSecurityManagerProfileId(),
                value.getAppType()
        );
    }
}