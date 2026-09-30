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

package bisq.api.dto.alert;

import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;

import java.util.Optional;

public record AuthorizedAlertDataDto(String id,
                                     long date,
                                     AlertType alertType,
                                     Optional<String> headline,
                                     Optional<String> message,
                                     boolean haltTrading,
                                     boolean requireVersionForTrading,
                                     Optional<String> minVersion,
                                     String securityManagerProfileId,
                                     AppType appType) {
}