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

package bisq.tor.onionservice;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import net.freehaven.tor.control.TorControlConnection;

@Getter
public class CreateHiddenServiceResult {
    private final String serviceId;
    private final String privateKey;

    public CreateHiddenServiceResult(TorControlConnection.CreateHiddenServiceResult result) {
        this(result.serviceID, result.privateKey);
    }

    @VisibleForTesting
    public CreateHiddenServiceResult(String serviceId, String privateKey) {
        this.serviceId = serviceId;
        this.privateKey = privateKey;
    }
}
