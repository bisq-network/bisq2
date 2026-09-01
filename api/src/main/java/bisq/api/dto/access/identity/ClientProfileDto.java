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
 * Deliberately carries no client secret: the secret is stored in plaintext and is handed to the
 * client once at pairing time only. Do not add it here.
 */
@Getter
@EqualsAndHashCode
public final class ClientProfileDto {
    private final String clientId;
    private final String clientName;

    public ClientProfileDto(String clientId, String clientName) {
        this.clientId = clientId;
        this.clientName = clientName;
    }
}
