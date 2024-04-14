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

import bisq.common.util.MathUtils;
import bisq.network.p2p.services.data.AddDataRequest;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.protobuf.DataRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class AddAppendOnlyDataRequest implements AddDataRequest {
    private final AppendOnlyData appendOnlyData;

    public AddAppendOnlyDataRequest(AppendOnlyData appendOnlyData) {
        this.appendOnlyData = appendOnlyData;

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public DataRequest.Builder getDataRequestBuilder(boolean serializeForHash) {
        return newDataRequestBuilder().setAddAppendOnlyDataRequest(toValueProto(serializeForHash));
    }

    @Override
    public bisq.network.protobuf.AddAppendOnlyDataRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.AddAppendOnlyDataRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.AddAppendOnlyDataRequest.newBuilder()
                .setAppendOnlyData(appendOnlyData.toAny(serializeForHash));
    }

    public static AddAppendOnlyDataRequest fromProto(bisq.network.protobuf.AddAppendOnlyDataRequest proto) {
        return new AddAppendOnlyDataRequest((AppendOnlyData) DistributedData.fromAny(proto.getAppendOnlyData()));
    }

    @Override
    public double getCostFactor() {
        return MathUtils.bounded(0.5, 1, appendOnlyData.getCostFactor());
    }

    @Override
    public boolean isExpired() {
        // AppendOnlyData never expires
        return false;
    }

    @Override
    public long getCreated() {
        // Used for sorting at pruning, but AppendOnlyData does not get pruned 
        return 0;
    }

    @Override
    public int getMaxMapSize() {
        return appendOnlyData.getMetaData().getMaxMapSize();
    }
}
