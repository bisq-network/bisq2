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

package bisq.http_api.access.pairing;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class PairingResponse {
    private final String clientId;
    private final String clientSecret;
    private final String sessionId;
    private final long sessionExpiryDate;

    public PairingResponse(String clientId,
                           String clientSecret,
                           String sessionId,
                           long sessionExpiryDate) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.sessionId = sessionId;
        this.sessionExpiryDate = sessionExpiryDate;
    }
}
