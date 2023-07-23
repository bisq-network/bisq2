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
import bisq.common.threading.ExecutorFactory;
import bisq.common.util.FileUtils;
import bisq.common.util.Version;
import bisq.security.PgPUtils;
import bisq.settings.CookieKey;
import bisq.settings.SettingsService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import static bisq.security.PgPUtils.EXTENSION;

@Slf4j
public class UpdateService implements Service {
    // For testing, we can use bisq1 url
    public static final String RELEASES_URL = "https://github.com/bisq-network/bisq/releases/tag/v";
    public static final String GITHUB_DOWNLOAD_URL = "https://github.com/bisq-network/bisq/releases/download/v";
    public static final String SIGNING_KEY_FILE = "signingkey.asc";
    public static final String KEY_4A133008 = "4A133008";
    public static final String KEY_E222AA02 = "E222AA02";
    public static final String VERSION_FILE_NAME = "version.txt";
    public static final String DESTINATION_DIR = "jar";
    public static final String DESTINATION_FILE_NAME = "desktop.jar";


    private final Version installedVersion;
    private final String baseDir;
    private final SettingsService settingsService;
    private final ReleaseNotificationsService releaseNotificationsService;
    @Getter
    private final Observable<ReleaseNotification> releaseNotification = new Observable<>();
    @Getter
    private final ObservableArray<DownloadDescriptor> downloadDescriptorList = new ObservableArray<>();
    private ExecutorService executorService;

    public UpdateService(ApplicationService.Config config, SettingsService settingsService, ReleaseNotificationsService releaseNotificationsService) {
        installedVersion = config.getVersion();
        baseDir = config.getBaseDir();
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
        String sourceFileName = getSourceFileName(version);
        String destinationDirectory = Path.of(baseDir, DESTINATION_DIR, version).toString();
        FileUtils.makeDirs(new File(destinationDirectory));

        downloadDescriptorList.setAll(getDownloadDescriptorList(version, destinationDirectory, sourceFileName));
        return download(downloadDescriptorList, getExecutorService())
                .thenCompose(nil -> verify(destinationDirectory, getExecutorService()))
                .thenCompose(nil -> writeVersionFile(version, getExecutorService()));
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private CompletableFuture<Void> download(List<DownloadDescriptor> downloadDescriptorList, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            for (DownloadDescriptor downloadDescriptor : downloadDescriptorList) {
                try {
                    FileUtils.downloadFile(downloadDescriptor.getUrl(), downloadDescriptor.getDestination(), downloadDescriptor.getProgress());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, executorService);
    }

    private CompletableFuture<Void> verify(String destinationDir, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> keyList = List.of(UpdateService.KEY_4A133008, UpdateService.KEY_E222AA02);
                PgPUtils.checkIfKeysMatchesResourceKeys(destinationDir, keyList);
                PgPUtils.verifyDownloadedFile(destinationDir, DESTINATION_FILE_NAME, UpdateService.SIGNING_KEY_FILE, keyList);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }, executorService);
    }

    private CompletionStage<Void> writeVersionFile(String version, ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileUtils.writeToFile(version, new File(baseDir, VERSION_FILE_NAME));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }, executorService);
    }

    private List<DownloadDescriptor> getDownloadDescriptorList(String version, String destinationDirectory, String sourceFileName) throws IOException {
        String baseUrl = GITHUB_DOWNLOAD_URL + version + "/";

        return List.of(
                DownloadDescriptor.create(SIGNING_KEY_FILE, baseUrl, destinationDirectory),
                DownloadDescriptor.create(KEY_4A133008 + EXTENSION, baseUrl, destinationDirectory),
                DownloadDescriptor.create(KEY_E222AA02 + EXTENSION, baseUrl, destinationDirectory),
                DownloadDescriptor.create(sourceFileName + EXTENSION, DESTINATION_FILE_NAME + EXTENSION, baseUrl, destinationDirectory),
                DownloadDescriptor.create(sourceFileName, DESTINATION_FILE_NAME, baseUrl, destinationDirectory)
        );
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = ExecutorFactory.newSingleThreadExecutor("DownloadExecutor");
        }
        return executorService;
    }

    private String getSourceFileName(String version) {
        // for testing, we can use bisq 1 installer file
        return "Bisq-" + version + ".dmg";
        // return "desktop_app-" + version + "-all.jar";
    }
}