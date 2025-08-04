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

package bisq.common.platform;

import bisq.common.application.Service;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.common.observable.map.ReadOnlyObservableMap;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public interface MemoryReportService extends Service {
    int NUM_POOL_THREADS_UPDATE_INTERVAL_SEC = 1;
    long MAX_AGE_NUM_POOL_THREADS = TimeUnit.HOURS.toMillis(1); // We want the keep data for the past hour

    void logReport();

    long getUsedMemoryInBytes();

    long getUsedMemoryInMB();

    long getFreeMemoryInMB();

    long getTotalMemoryInMB();

    ReadOnlyObservableMap<String, ObservableHashMap<Long, AtomicInteger>> getHistoricalNumThreadsByThreadName();

    ReadOnlyObservable<Integer> getCurrentNumThreads();

    ReadOnlyObservable<Integer> getPeakNumThreads();
}
