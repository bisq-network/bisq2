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

package bisq.evolution.updater;

import bisq.application.ApplicationService;
import bisq.bonded_roles.release.ReleaseNotification;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.application.ApplicationVersion;
import bisq.common.application.Service;
import bisq.common.file.FileUtils;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableArray;
import bisq.common.platform.PlatformUtils;
import bisq.common.platform.Version;
import bisq.common.threading.ExecutorFactory;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import static bisq.evolution.updater.UpdaterUtils.*;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class UpdaterService implements Service {
    private final SettingsService settingsService;
    private final ReleaseNotificationsService releaseNotificationsService;
    private final AlertService alertService;

    @Getter
    private final Observable<ReleaseNotification> releaseNotification = new Observable<>();
    @Getter
    private final Observable<Boolean> isNewReleaseAvailable = new Observable<>();
    @Getter
    private final Observable<Boolean> ignoreNewRelease = new Observable<>();
    @Getter
    private final ObservableArray<DownloadItem> downloadItemList = new ObservableArray<>();
    private final ApplicationService.Config config;
    private ExecutorService executorService;
    private final CollectionObserver<ReleaseNotification> releaseNotificationsObserver;
    @Getter
    private boolean requireVersionForTrading;
    @Getter
    private Optional<String> minRequiredVersionForTrading = Optional.empty();
    private Pin releaseNotificationsPin, authorizedAlertDataSetPin;

    public UpdaterService(ApplicationService.Config config,
                          SettingsService settingsService,
                          ReleaseNotificationsService releaseNotificationsService,
                          AlertService alertService) {
        this.config = config;

        this.settingsService = settingsService;
        this.releaseNotificationsService = releaseNotificationsService;
        this.alertService = alertService;

        releaseNotificationsObserver = new CollectionObserver<>() {
            @Override
            public void add(ReleaseNotification releaseNotification) {
                processAddedReleaseNotification(releaseNotification);
            }

            @Override
            public void remove(Object element) {
                if (element instanceof ReleaseNotification toRemove) {
                    if (releaseNotification.get() == null) {
                        return;
                    }
                    if (releaseNotification.get().equals(toRemove)) {
                        releaseNotification.set(null);
                        isNewReleaseAvailable.set(false);
                        settingsService.setCookie(CookieKey.IGNORE_VERSION, toRemove.getVersionString(), false);
                    } else {
                        log.info("We got a remove call with a different releaseNotification as we have stored. releaseNotification={}, toRemove={}",
                                releaseNotification, toRemove);
                    }
                }
            }

            @Override
            public void clear() {
                releaseNotification.set(null);
                isNewReleaseAvailable.set(false);
            }
        };
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        log.info("initialize");

        releaseNotificationsPin = releaseNotificationsService.getReleaseNotifications().addObserver(releaseNotificationsObserver);

        authorizedAlertDataSetPin = alertService.getAuthorizedAlertDataSet().addObserver(new CollectionObserver<>() {
            @Override
            public void add(AuthorizedAlertData authorizedAlertData) {
                if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.isRequireVersionForTrading()) {
                    requireVersionForTrading = true;
                    minRequiredVersionForTrading = authorizedAlertData.getMinVersion();
                    reapplyAllReleaseNotifications();
                }
            }

            @Override
            public void remove(Object element) {
                if (element instanceof AuthorizedAlertData authorizedAlertData) {
                    if (authorizedAlertData.getAlertType() == AlertType.EMERGENCY && authorizedAlertData.isRequireVersionForTrading()) {
                        requireVersionForTrading = false;
                        minRequiredVersionForTrading = Optional.empty();
                        reapplyAllReleaseNotifications();
                    }
                }
            }

            @Override
            public void clear() {
                requireVersionForTrading = false;
                minRequiredVersionForTrading = Optional.empty();
                reapplyAllReleaseNotifications();
            }
        });

        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        log.info("shutdown");
        if (releaseNotificationsPin != null) {
            releaseNotificationsPin.unbind();
        }
        if (authorizedAlertDataSetPin != null) {
            authorizedAlertDataSetPin.unbind();
        }
        return CompletableFuture.supplyAsync(() -> {
            if (executorService != null) {
                ExecutorFactory.shutdownAndAwaitTermination(executorService, 100);
            }
            return true;
        });
    }


    /* --------------------------------------------------------------------- */
    // API
    /* --------------------------------------------------------------------- */

    public void reapplyAllReleaseNotifications() {
        if (releaseNotificationsPin != null) {
            releaseNotificationsPin.unbind();
        }
        releaseNotificationsPin = releaseNotificationsService.getReleaseNotifications().addObserver(releaseNotificationsObserver);
    }

    public CompletableFuture<Void> downloadAndVerify() throws IOException {
        ReleaseNotification releaseNotification = this.releaseNotification.get();
        if (releaseNotification == null) {
            return CompletableFuture.completedFuture(null);
        }

        String version = releaseNotification.getVersionString();
        boolean isLauncherUpdate = releaseNotification.isLauncherUpdate();
        String baseDir = config.getBaseDir().toAbsolutePath().toString();
        List<String> keyIds = config.getKeyIds();
        checkArgument(!keyIds.isEmpty());

        String downloadFileName = UpdaterUtils.getDownloadFileName(version, isLauncherUpdate);
        String destinationDirectory = isLauncherUpdate ? PlatformUtils.getDownloadOfHomeDir() :
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


    /* --------------------------------------------------------------------- */
    // Private/package static
    /* --------------------------------------------------------------------- */

    private void processAddedReleaseNotification(ReleaseNotification newReleaseNotification) {
        releaseNotification.set(newReleaseNotification);

        if (newReleaseNotification == null) {
            isNewReleaseAvailable.set(false);
            return;
        }

        Version newVersion = newReleaseNotification.getReleaseVersion();
        boolean isNewRelease = ApplicationVersion.getVersion().below(newVersion);
        if (isNewRelease) {
            boolean notifyForPreRelease = settingsService.getCookie().asBoolean(CookieKey.NOTIFY_FOR_PRE_RELEASE).orElse(false);
            if (newReleaseNotification.isPreRelease()) {
                isNewReleaseAvailable.set(notifyForPreRelease);
            } else {
                isNewReleaseAvailable.set(true);
            }
        } else {
            isNewReleaseAvailable.set(false);
        }

        if (requireVersionForTrading &&
                minRequiredVersionForTrading.isPresent() &&
                ApplicationVersion.getVersion().below(new Version(minRequiredVersionForTrading.get()))) {
            log.info("The ignore flag is not applied because we received a minRequiredVersionForTrading which is above the applicationVersion");
            ignoreNewRelease.set(false);
        } else {
            Boolean ignore = settingsService.getCookie().asBoolean(CookieKey.IGNORE_VERSION, newVersion.toString()).orElse(false);
            ignoreNewRelease.set(ignore);
        }
    }

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
                    log.info("Download {}", downloadItem);
                    URL url = URI.create(downloadItem.getUrlPath()).toURL();
                    FileUtils.downloadFile(url, downloadItem.getDestinationFile(), downloadItem.getProgress());
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, executorService);
    }

    @VisibleForTesting
    static CompletableFuture<Void> verify(String version,
                                          boolean isLauncherUpdate,
                                          String destinationDir,
                                          List<String> keyIds,
                                          boolean ignoreSigningKeyInResourcesCheck,
                                          ExecutorService executorService) {
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