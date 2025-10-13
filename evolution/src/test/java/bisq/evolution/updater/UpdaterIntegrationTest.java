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

import bisq.common.file.FileUtils;
import bisq.common.threading.ExecutorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static bisq.evolution.updater.UpdaterUtils.UPDATES_DIR;
import static bisq.evolution.updater.UpdaterUtils.readVersionFromVersionFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// Tests deactivated as they fail on CI due FileNotFoundException probably related to the srcBaseDir location.
// Locally it works. But anyway the test could be removed as well. Leaving it still until the updater project is completed.
@Slf4j
public class UpdaterIntegrationTest {
    private Path srcBaseDir;
    private Path destinationBaseDir;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        srcBaseDir = Path.of("build/resources/test");
        destinationBaseDir = Path.of("temp");

        executorService = ExecutorFactory.newSingleThreadExecutor("DownloadExecutor");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(destinationBaseDir)) {
            FileUtils.deleteFileOrDirectory(destinationBaseDir);
        }
    }

    // @Test
    public void downloadAndVerifyLauncher() {
        String baseDir = destinationBaseDir.toAbsolutePath().toString();
        String version = "1.9.9";
        Path sourceDirectory = srcBaseDir.resolve(version);
        Path destinationDirectory = destinationBaseDir.resolve(UPDATES_DIR).resolve(version);

        boolean isLauncherUpdate = true;
        boolean ignoreSigningKeyInResourcesCheck = false;
        List<String> keyIds = List.of("387C8307");
        String downloadFileName = UpdaterUtils.getDownloadFileName(version, isLauncherUpdate);
        try {
            Files.createDirectories(destinationDirectory);
            List<DownloadItem> downloadItemList = new ArrayList<>(DownloadItem.createDescriptorList(version, destinationDirectory.toString(), downloadFileName, keyIds));
            simulateDownload(downloadItemList, sourceDirectory.toString(), executorService)
                    .thenCompose(nil -> UpdaterService.verify(version, isLauncherUpdate, destinationDirectory.toString(), keyIds, ignoreSigningKeyInResourcesCheck, executorService))
                    .thenCompose(nil -> UpdaterService.writeVersionFile(version, baseDir, executorService))
                    .whenComplete((e, throwable) -> {
                        if (throwable != null) {
                            throwable.printStackTrace();
                            fail();
                        }
                    })
                    .join();

            List<String> filesInSourceDirectory = FileUtils.listFilesInDirectory(sourceDirectory, 100).stream().filter(e -> !e.equals(".DS_Store")).sorted().toList();
            List<String> filesInDestinationDirectory = FileUtils.listFilesInDirectory(destinationDirectory, 100).stream().filter(e -> !e.equals(".DS_Store")).sorted().toList();
            assertEquals(filesInSourceDirectory, filesInDestinationDirectory);

            Optional<String> versionFromFile = readVersionFromVersionFile(baseDir);
            assertTrue(versionFromFile.isPresent());
            assertEquals(version, versionFromFile.get());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    // @Test
    public void downloadAndVerifyJar() {
        String baseDir = destinationBaseDir.toAbsolutePath().toString();
        String version = "1.9.10";
        Path sourceDirectory = srcBaseDir.resolve(version);
        Path destinationDirectory = destinationBaseDir.resolve(UPDATES_DIR).resolve(version);

        boolean isLauncherUpdate = false;
        boolean ignoreSigningKeyInResourcesCheck = false;
        List<String> keyIds = List.of("387C8307");
        String downloadFileName = UpdaterUtils.getDownloadFileName(version, isLauncherUpdate);
        try {
            Files.createDirectories(destinationDirectory);
            List<DownloadItem> downloadItemList = new ArrayList<>(DownloadItem.createDescriptorList(version, destinationDirectory.toString(), downloadFileName, keyIds));
            simulateDownload(downloadItemList, sourceDirectory.toString(), executorService)
                    .thenCompose(nil -> UpdaterService.verify(version, isLauncherUpdate, destinationDirectory.toString(), keyIds, ignoreSigningKeyInResourcesCheck, executorService))
                    .thenCompose(nil -> UpdaterService.writeVersionFile(version, baseDir, executorService))
                    .whenComplete((e, throwable) -> {
                        if (throwable != null) {
                            throwable.printStackTrace();
                            fail();
                        }
                    })
                    .join();

            List<String> filesInSourceDirectory = FileUtils.listFilesInDirectory(sourceDirectory, 100).stream().filter(e -> !e.equals(".DS_Store")).sorted().toList();
            List<String> filesInDestinationDirectory = FileUtils.listFilesInDirectory(destinationDirectory, 100).stream().filter(e -> !e.equals(".DS_Store")).sorted().toList();
            assertEquals(filesInSourceDirectory, filesInDestinationDirectory);

            Optional<String> versionFromFile = readVersionFromVersionFile(baseDir);
            assertTrue(versionFromFile.isPresent());
            assertEquals(version, versionFromFile.get());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    CompletableFuture<Void> simulateDownload(List<DownloadItem> downloadItemList,
                                             String localDevTestSrcDir,
                                             ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            for (DownloadItem downloadItem : downloadItemList) {
                try {
                    FileUtils.copyFile(
                            Path.of(localDevTestSrcDir, downloadItem.getSourceFileName()),
                            downloadItem.getDestinationFile().toPath());
                    downloadItem.getProgress().set(1d);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            return null;
        }, executorService);
    }
}
