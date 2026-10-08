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

package bisq.api.dto.access.identity;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A paired client as exposed by the list endpoint.
 * <p>
 * Carries neither the client secret nor the client id, as both are part of what a client
 * authenticates with. {@code managementId} is the derived handle instead (see
 * {@code ClientManagementId}), named so it cannot be mistaken for the client id. Do not add either
 * of the real values here.
 */
@Getter
@EqualsAndHashCode
public final class PairedClientDto {
    private final String managementId;
    private final String clientName;

    public PairedClientDto(String managementId, String clientName) {
        this.managementId = managementId;
        this.clientName = clientName;
    }
}
