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

package bisq.network.p2p.services.data.inventory;

import bisq.common.observable.Observable;
import bisq.network.p2p.common.RequestFuture;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class InventoryRequestModel {
    private final Observable<Integer> numPendingRequests = new Observable<>(0);
    private final Observable<Boolean> allDataReceived = new Observable<>(false);
    private final Map<String, Long> requestTimestampByConnectionId = new ConcurrentHashMap<>();
    private final Map<String, RequestFuture<InventoryRequest, InventoryResponse>> requestFuturesByConnectionId;

    public InventoryRequestModel(Map<String, RequestFuture<InventoryRequest, InventoryResponse>> requestFuturesByConnectionId) {
        this.requestFuturesByConnectionId = requestFuturesByConnectionId;
    }
}
