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

package bisq.network.p2p.services.data.storage.append;

import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.DistributedData;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public class AddAppendOnlyDataRequest implements AddDataRequest {
    private final AppendOnlyData appendOnlyData;

    public AddAppendOnlyDataRequest(AppendOnlyData appendOnlyData) {
        this.appendOnlyData = appendOnlyData;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setDataRequest(getDataRequestBuilder()
                        .setAddAppendOnlyDataRequest(
                                bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder()
                                        .setAppendOnlyData(appendOnlyData.toAny())
                        )
                ).build();
    }

    public static AddAppendOnlyDataRequest fromProto(bisq.network.protobuf.AddAppendOnlyDataRequest proto) {
        return new AddAppendOnlyDataRequest((AppendOnlyData) DistributedData.fromAny(proto.getAppendOnlyData()));
    }

    @Override
    public boolean isExpired() {
        // AppendOnlyData never expires
        return false;
    }
}
