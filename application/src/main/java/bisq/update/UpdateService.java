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

package bisq.update;

import bisq.application.ApplicationService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.timer.Scheduler;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

public class UpdateService implements Service {
    private static final String RELEASES_URL = "https://github.com/bisq-network/bisq2/releases/";
    @Getter
    private final ObservableArray<DownloadInfo> downloadInfoList = new ObservableArray<>();
    @Getter
    private final Observable<Boolean> downloadCompleted = new Observable<>(false);
    @Getter
    private final Observable<Boolean> ignoreVersion = new Observable<>(false);
    @Getter
    private final Observable<String> version = new Observable<>("");
    @Getter
    private final Observable<String> releaseNodes = new Observable<>("");
    @Getter
    private final Observable<String> downloadUrl = new Observable<>("");
    private final SettingsService settingsService;
    private final String currentVersion;

    public UpdateService(ApplicationService.Config config, SettingsService settingsService) {
        currentVersion = config.getVersion();
        this.settingsService = settingsService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    public void onNewVersion(String version) {
        ignoreVersion.set(settingsService.getCookie().asBoolean(CookieKey.IGNORE_VERSION, version).orElse(false));
        this.version.set(version);
    }

    public void download() {
        onNewVersion("2.0.1");
        releaseNodes.set("releaseNodes.....");
        downloadUrl.set(RELEASES_URL + version.get());
        

        downloadInfoList.add(new DownloadInfo("test file1"));
        downloadInfoList.add(new DownloadInfo("test file 2"));
        downloadInfoList.add(new DownloadInfo("test file 3"));

        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(0.2)).after(1000);
        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(1d)).after(2000);
        Scheduler.run(() -> downloadInfoList.get(0).getIsVerified().set(true)).after(2500);
        Scheduler.run(() -> downloadCompleted.set(true)).after(4000);
    }
}