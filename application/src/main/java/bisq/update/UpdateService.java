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
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.timer.Scheduler;
import bisq.common.util.Version;
import bisq.security.PgPUtils;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class UpdateService implements Service {
    private static final String RELEASES_URL = "https://github.com/bisq-network/bisq/releases/tag/v";
    public static final String EXTENSION = PgPUtils.EXTENSION;
    public static final String SIGNING_KEY_FILE = "signingkey.asc";
    // public static final String FILE = "desktop.jar";
    public static final String FILE = "Bisq-1.9.9.dmg";
    // public static final String SIGNATURE_FILE = "Bisq-1.9.9.dmg.asc";
    public static final String KEY_4A133008 = "4A133008";
    public static final String KEY_E222AA02 = "E222AA02";

    //https://github.com/bisq-network/bisq/releases/download/v1.9.9/Bisq-1.9.9.dmg
    @Getter
    private final ObservableArray<DownloadInfo> downloadInfoList = new ObservableArray<>();
    @Getter
    private final Observable<Boolean> downloadCompleted = new Observable<>(false);
    @Getter
    private final Observable<ReleaseNotification> releaseNotification = new Observable<>();
    @Getter
    private final Observable<String> downloadUrl = new Observable<>("");
    private final SettingsService settingsService;
    private final ReleaseNotificationsService releaseNotificationsService;
    private final Version installedVersion;

    public UpdateService(ApplicationService.Config config, SettingsService settingsService, ReleaseNotificationsService releaseNotificationsService) {
        installedVersion = config.getVersion();
        this.settingsService = settingsService;
        this.releaseNotificationsService = releaseNotificationsService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        releaseNotificationsService.getReleaseNotifications().addListener(new CollectionObserver<>() {
            @Override
            public void add(ReleaseNotification releaseNotification) {
                onNewReleaseNotificationAdded(releaseNotification);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof ReleaseNotification) {
                    ReleaseNotification toRemove = (ReleaseNotification) element;
                    if (releaseNotification.get() != null) {
                        if (toRemove.equals(releaseNotification.get())) {
                            releaseNotification.set(null);
                        } else {
                            log.debug("We got a remove call with a different releaseNotification as we have stored. releaseNotification={}, toRemove={}",
                                    releaseNotification, toRemove);
                        }
                    }
                }
            }

            @Override
            public void clear() {
                releaseNotification.set(null);
            }
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    public void onNewReleaseNotificationAdded(ReleaseNotification releaseNotification) {
        if (releaseNotification == null) {
            log.warn("releaseNotification is null");
            return;
        }

        Version newVersion = releaseNotification.getVersion();
        if (newVersion.belowOrEqual(installedVersion)) {
            log.debug("Our installed version is the same or higher as the version of the new releaseNotification.");
            return;
        }

        if (this.releaseNotification.get() != null && newVersion.belowOrEqual(this.releaseNotification.get().getVersion())) {
            log.debug("The version of our existing releaseNotification is the same or higher as the version of the new releaseNotification.");
            return;
        }

        boolean ignoreVersion = settingsService.getCookie().asBoolean(CookieKey.IGNORE_VERSION, newVersion.toString()).orElse(false);
        if (ignoreVersion) {
            log.debug("We had clicked ignore for that version");
            return;
        }
        boolean notifyForPreRelease = settingsService.getCookie().asBoolean(CookieKey.NOTIFY_FOR_PRE_RELEASE).orElse(false);
        if (releaseNotification.isPreRelease() && !notifyForPreRelease) {
            log.debug("This is a pre-release and we have not enabled to get notified for pre-releases.");
            return;
        }
        this.releaseNotification.set(releaseNotification);
        downloadUrl.set(RELEASES_URL + newVersion);
    }

    public void download() {

        downloadInfoList.add(new DownloadInfo("test file1"));
        downloadInfoList.add(new DownloadInfo("test file 2"));
        downloadInfoList.add(new DownloadInfo("test file 3"));

        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(0.2)).after(1000);
        Scheduler.run(() -> downloadInfoList.get(0).getProgress().set(1d)).after(2000);
        Scheduler.run(() -> downloadInfoList.get(0).getIsVerified().set(true)).after(2500);
        Scheduler.run(() -> downloadCompleted.set(true)).after(4000);
    }
}