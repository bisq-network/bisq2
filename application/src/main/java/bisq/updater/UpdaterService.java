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

package bisq.updater;

import bisq.application.ApplicationService;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.common.application.Service;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.FileUtils;
import bisq.common.util.OsUtils;
import bisq.common.util.Version;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import static bisq.updater.UpdaterUtils.UPDATES_DIR;
import static bisq.updater.UpdaterUtils.VERSION_FILE_NAME;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UpdaterService implements Service {
    private final SettingsService settingsService;
    private final ReleaseNotificationsService releaseNotificationsService;
    @Getter
    private final Observable<ReleaseNotification> releaseNotification = new Observable<>();
    @Getter
    private final ObservableArray<DownloadItem> downloadItemList = new ObservableArray<>();
    private final ApplicationService.Config config;
    private ExecutorService executorService;

    public UpdaterService(ApplicationService.Config config, SettingsService settingsService, ReleaseNotificationsService releaseNotificationsService) {
        this.config = config;

        this.settingsService = settingsService;
        this.releaseNotificationsService = releaseNotificationsService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");
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
        log.info("shutdown");
        if (executorService != null) {
            ExecutorFactory.shutdownAndAwaitTermination(executorService, 100);
        }
        return CompletableFuture.completedFuture(true);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public void onNewReleaseNotificationAdded(ReleaseNotification releaseNotification) {
        if (releaseNotification == null) {
            log.warn("releaseNotification is null");
            return;
        }

        Version newVersion = releaseNotification.getVersion();
        Version installedVersion = config.getVersion();
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
    }

    public CompletableFuture<Void> downloadAndVerify() throws IOException {
        String version = releaseNotification.get().getVersionString();
        boolean isLauncherUpdate = releaseNotification.get().isLauncherUpdate();
        String baseDir = config.getBaseDir().toAbsolutePath().toString();
        List<String> keyIds = config.getKeyIds();
        checkArgument(!keyIds.isEmpty());

        String downloadFileName = UpdaterUtils.getDownloadFileName(version, isLauncherUpdate);
        String destinationDirectory = isLauncherUpdate ? OsUtils.getDownloadOfHomeDir() :
                Path.of(baseDir, UPDATES_DIR, version).toString();
        FileUtils.makeDirs(new File(destinationDirectory));
        downloadItemList.setAll(DownloadItem.createDescriptorList(version, destinationDirectory, downloadFileName, keyIds));
        if (executorService == null) {
            executorService = ExecutorFactory.newSingleThreadExecutor("DownloadExecutor");
        }
        boolean isIgnoreSigningKeyInResourcesCheck = config.isIgnoreSigningKeyInResourcesCheck();
        return downloadAndVerify(version,
                isLauncherUpdate,
                downloadItemList,
                destinationDirectory,
                baseDir,
                keyIds,
                isIgnoreSigningKeyInResourcesCheck,
                executorService);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private/package static
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private static CompletableFuture<Void> downloadAndVerify(String version,
                                                             boolean isLauncherUpdate,
                                                             List<DownloadItem> downloadItemList,
                                                             String destinationDirectory,
                                                             String baseDir,
                                                             List<String> keyIds,
                                                             boolean ignoreSigningKeyInResourcesCheck,
                                                             ExecutorService executorService) {
        return download(downloadItemList, executorService)
                .thenCompose(nil -> verify(version, isLauncherUpdate, destinationDirectory, keyIds, ignoreSigningKeyInResourcesCheck, executorService))
                .thenCompose(nil -> writeVersionFile(version, baseDir, executorService));
    }

    @VisibleForTesting
    static CompletableFuture<Void> download(List<DownloadItem> downloadItemList, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            for (DownloadItem downloadItem : downloadItemList) {
                try {
                    FileUtils.downloadFile(new URL(downloadItem.getUrlPath()), downloadItem.getDestinationFile(), downloadItem.getProgress());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, executorService);
    }

    @VisibleForTesting
    static CompletableFuture<Void> verify(String version, boolean isLauncherUpdate, String destinationDir, List<String> keyIds, boolean ignoreSigningKeyInResourcesCheck, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DownloadedFilesVerification.verify(destinationDir, UpdaterUtils.getDownloadFileName(version, isLauncherUpdate), keyIds, ignoreSigningKeyInResourcesCheck);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @VisibleForTesting
    static CompletionStage<Void> writeVersionFile(String version, String baseDir, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileUtils.writeToFile(version, new File(baseDir, VERSION_FILE_NAME));
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }, executorService);
    }
}