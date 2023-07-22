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

package bisq.desktop.overlay.update.service;

import bisq.common.application.Service;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.timer.Scheduler;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

public class UpdateService implements Service {
    @Getter
    private final ObservableArray<DownloadInfo> downloadInfoList = new ObservableArray<>();

    public UpdateService() {
    }

    @Override
    public CompletableFuture<Boolean> initialize() {

        downloadInfoList.add(new DownloadInfo("test file1"));
        downloadInfoList.add(new DownloadInfo("test file 2"));
        downloadInfoList.add(new DownloadInfo("test file 3"));

        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(0.2)).after(1000);
        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(1d)).after(2000);
        Scheduler.run(() -> downloadInfoList.get(0).getIsVerified().set(true)).after(2500);

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }
}