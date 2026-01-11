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

package bisq.api.access.http;

import bisq.api.access.identity.DeviceProfile;
import bisq.api.access.pairing.PairingRequest;
import bisq.api.access.pairing.PairingService;
import bisq.api.access.session.SessionService;
import bisq.api.access.session.SessionToken;
import lombok.extern.slf4j.Slf4j;

@Slf4j

public class PairingRequestHandler {
    private final PairingService pairingService;
    private final SessionService sessionService;

    public PairingRequestHandler(PairingService pairingService,
                                 SessionService sessionService) {
        this.pairingService = pairingService;
        this.sessionService = sessionService;
    }

    public SessionToken handle(PairingRequest request) {
        DeviceProfile deviceProfile = pairingService.pairDevice(request);
        return sessionService.createSession(deviceProfile.getDeviceId());
    }
}
