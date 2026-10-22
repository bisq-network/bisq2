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
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.release.ReleaseNotificationsService;
import bisq.bonded_roles.security_manager.alert.AlertService;
import bisq.common.application.ApplicationVersion;
import bisq.common.file.FileMutatorUtils;
import bisq.settings.SettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UpdaterServiceTest {
    private static final String CURRENT_VERSION = ApplicationVersion.getVersion().toString();

    @Test
    void initializeWritesVersionFile(@TempDir Path appDataDirPath) {
        initialize(appDataDirPath);

        assertEquals(Optional.of(CURRENT_VERSION), UpdaterUtils.readVersionFromVersionFile(appDataDirPath));
    }

    @Test
    void maybeUpdateVersionFileWritesVersionFileIfMissing(@TempDir Path appDataDirPath) {
        UpdaterService.maybeUpdateVersionFile(CURRENT_VERSION, appDataDirPath);

        assertEquals(Optional.of(CURRENT_VERSION), UpdaterUtils.readVersionFromVersionFile(appDataDirPath));
    }

    @Test
    void maybeUpdateVersionFileOverwritesVersionFileWithOtherVersion(@TempDir Path appDataDirPath) throws IOException {
        FileMutatorUtils.writeToPath("0.0.1", appDataDirPath.resolve(UpdaterUtils.VERSION_FILE_NAME));

        UpdaterService.maybeUpdateVersionFile(CURRENT_VERSION, appDataDirPath);

        assertEquals(Optional.of(CURRENT_VERSION), UpdaterUtils.readVersionFromVersionFile(appDataDirPath));
    }

    @Test
    void maybeUpdateVersionFileDoesNotTouchVersionFileWithCurrentVersion(@TempDir Path appDataDirPath) throws IOException {
        Path versionFilePath = appDataDirPath.resolve(UpdaterUtils.VERSION_FILE_NAME);
        FileMutatorUtils.writeToPath(CURRENT_VERSION, versionFilePath);
        FileTime lastModifiedTime = FileTime.fromMillis(0);
        Files.setLastModifiedTime(versionFilePath, lastModifiedTime);

        UpdaterService.maybeUpdateVersionFile(CURRENT_VERSION, appDataDirPath);

        assertEquals(lastModifiedTime, Files.getLastModifiedTime(versionFilePath));
        assertEquals(Optional.of(CURRENT_VERSION), UpdaterUtils.readVersionFromVersionFile(appDataDirPath));
    }

    private static void initialize(Path appDataDirPath) {
        ApplicationService.Config config = mock(ApplicationService.Config.class);
        when(config.getAppDataDirPath()).thenReturn(appDataDirPath);
        UpdaterService updaterService = new UpdaterService(config,
                mock(SettingsService.class, RETURNS_DEEP_STUBS),
                mock(ReleaseNotificationsService.class, RETURNS_DEEP_STUBS),
                mock(AlertService.class, RETURNS_DEEP_STUBS),
                AppType.DESKTOP);
        updaterService.initialize().join();
    }
}
