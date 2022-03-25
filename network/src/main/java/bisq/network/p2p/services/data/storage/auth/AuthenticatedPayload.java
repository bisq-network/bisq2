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

import bisq.common.encoding.Proto;
import bisq.network.p2p.services.data.storage.NetworkPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@ToString
@EqualsAndHashCode
public class AuthenticatedPayload implements NetworkPayload {
    @Getter
    protected final Proto data;
    protected final MetaData metaData;

    public AuthenticatedPayload(Proto data) {
        // 463 is overhead of sig/pubkeys,...
        // 582 is pubkey+sig+hash
        this(data, new MetaData(TimeUnit.DAYS.toMillis(10), 251 + 463, data.getClass().getSimpleName()));
    }

    public AuthenticatedPayload(Proto data, MetaData metaData) {
        this.data = data;
        this.metaData = metaData;
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
