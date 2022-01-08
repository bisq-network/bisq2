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

package bisq.network.p2p.services.data.storage.auth;

import bisq.network.p2p.NetworkId;
import bisq.network.p2p.message.Proto;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@EqualsAndHashCode
@ToString
@Getter
public class AuthenticatedNetworkIdPayload implements AuthenticatedPayload {
    private final Proto data;
    private final NetworkId networkId;
    final MetaData metaData;

    public AuthenticatedNetworkIdPayload(Proto data, NetworkId networkId) {
        this.data = data;
        this.networkId = networkId;
        // 463 is overhead of sig/pubkeys,...
        // 582 is pubkey+sig+hash
        metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 251 + 463, data.getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}
