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

package bisq.persistence.backup;

import bisq.common.file.FileMutatorUtils;
import bisq.common.platform.PlatformUtils;
import bisq.persistence.Persistence;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class BackupServiceTest {
    private BackupService backupService;
    private final Path dataDirPath = PlatformUtils.getUserDataDirPath().resolve("bisq2_BackupServiceTest");
    private Path storeFilePath;

    @BeforeEach
    void setUp() throws IOException {
        Path dbDirPath = dataDirPath.resolve("db");
        FileMutatorUtils.createRestrictedDirectories(dbDirPath);
        String storeFileName = "test_store" + Persistence.EXTENSION;
        storeFilePath = dbDirPath.resolve(storeFileName);
        backupService = new BackupService(dataDirPath, this.storeFilePath, MaxBackupSize.HUNDRED_MB);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileMutatorUtils.deleteFileOrDirectory(dataDirPath);
    }

    @Test
    void testGetRelativePath2() {
        Path dataDirPath, storeFilePath;
        String dirPathString;

        // ✅ Windows Test
        dataDirPath = Path.of("C:\\Users\\bisq_user\\alice");
        storeFilePath = Path.of(dataDirPath + "\\db\\private\\key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, true);
        assertEquals("\\db\\private\\key_bundle_store.protobuf", dirPathString);

        // ✅ macOS Test
        dataDirPath = Path.of("/Users/bisq_user/Library/Application Support/alice");
        storeFilePath = Path.of(dataDirPath + "/db/private/key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPathString);

        // ✅ Unix Test
        dataDirPath = Path.of("/home/bisq_user/alice");
        storeFilePath = Path.of(dataDirPath + "/db/private/key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPathString);

        // ✅ Windows UNC Path Test (Server Shares)
        dataDirPath = Path.of("\\\\Server\\Share\\bisq_user\\alice");
        storeFilePath = Path.of("\\\\Server\\Share\\bisq_user\\alice\\db\\private\\key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, true);
        assertEquals("\\db\\private\\key_bundle_store.protobuf", dirPathString);

        // ✅ Nested Directory Test
        dataDirPath = Path.of("/home/bisq_user/alice");
        storeFilePath = Path.of("/home/bisq_user/alice/db/subdir/private/key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, false);
        assertEquals("/db/subdir/private/key_bundle_store.protobuf", dirPathString);

        // ✅ Path with Trailing Slash in dataDirPath
        dataDirPath = Path.of("/home/bisq_user/alice/");
        storeFilePath = Path.of("/home/bisq_user/alice/db/private/key_bundle_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPathString);

        // ✅ Special Characters in Path
        dataDirPath = Path.of("C:\\Users\\bisq user\\data");
        storeFilePath = Path.of("C:\\Users\\bisq user\\data\\db\\private\\my file_store.protobuf");
        dirPathString = BackupService.getRelativePath(dataDirPath, storeFilePath, true);
        assertEquals("\\db\\private\\my file_store.protobuf", dirPathString);

        // ❌ File Outside `dataDirPath` (Should Throw Exception)
        dataDirPath = Path.of("/home/bisq_user/alice");
        storeFilePath = Path.of("/home/bisq_user/other_user/db/private/key_bundle_store.protobuf");
        Path finalDataDirPath = dataDirPath;
        Path finalStoreFilePath = storeFilePath;
        assertThrows(IllegalArgumentException.class, () -> {
            BackupService.getRelativePath(finalDataDirPath, finalStoreFilePath, false);
        });
    }

    @Test
    void testResolveDirPath() {
        Path dataDirPath, storeFilePath, dirPath;

        // Windows
        dataDirPath = Path.of("C:\\Users\\bisq_user\\alice");
        storeFilePath = Path.of(dataDirPath + "\\db\\private\\key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDirPath, storeFilePath);
        assertEquals(Path.of(dataDirPath + "\\backups\\private\\key_bundle"), dirPath);

        // OSX
        dataDirPath = Path.of("/Users/bisq_user/Library/Application Support/alice");
        storeFilePath = Path.of(dataDirPath + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDirPath, storeFilePath);
        assertEquals(Path.of(dataDirPath + "/backups/private/key_bundle"), dirPath);

        // Unix
        dataDirPath = Path.of("/home/bisq_user/alice");
        storeFilePath = Path.of(dataDirPath + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDirPath, storeFilePath);
        assertEquals(Path.of(dataDirPath + "/backups/private/key_bundle"), dirPath);
    }

    @Test
    void testBackup() throws IOException {
        FileMutatorUtils.writeToPath("test", storeFilePath);
        assertThat(storeFilePath).exists();
        Path backupFilePath = backupService.getBackupFilePath();
        assertThat(backupFilePath).doesNotExist();
        backupService.backup(backupFilePath);
        assertThat(backupFilePath).exists();
        assertThat(storeFilePath).doesNotExist();
    }

    @Test
    void testPrune(@TempDir Path tempDir) {
        Predicate<BackupFileInfo> isMaxFileSizeReachedFunction = e -> false;
        List<Path> paths;
        List<BackupFileInfo> list;
        List<BackupFileInfo> outdatedBackupFileInfos;
        List<BackupFileInfo> remaining;
        LocalDateTime now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);
        String fileName = "test_store.protobuf";


        // Empty
        paths = List.of();
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());

        // Backup date is in the future. We treat backup as daily backup
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        now = LocalDateTime.parse("2024-09-14_1755", BackupService.DATE_FORMAT);
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());
        now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);

        // Last 2 minutes. We keep all
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-15_1754"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // After 1 hour we keep only the newest per hour
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-15_1653"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1654"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1655"), // remove as
                tempDir.resolve("test_store.protobuf_2024-09-15_1656"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(1))));


        // At 24 hour we keep only the newest per day
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-14_1754"), // remove as 14_1755 is newer
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"), // use day map
                tempDir.resolve("test_store.protobuf_2024-09-14_1756"), // remove as 14_1757 is newer
                tempDir.resolve("test_store.protobuf_2024-09-14_1757"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1855"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(2))));


        // Different day, we keep all as in last week
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(list.getFirst().getPath().getFileName(), paths.get(1).getFileName());
        assertEquals(0, outdatedBackupFileInfos.size());


        // Same day, we keep all as in last week
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-15_1754"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1752")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // Same day, we keep only newest as it's older than past 7 days
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-05_1754"),
                tempDir.resolve("test_store.protobuf_2024-09-05_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-05_1752")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(2))));


        // Past 7 days in calendar week
        now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-09_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-10_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-11_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-12_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-13_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertTrue(outdatedBackupFileInfos.isEmpty());


        // Past 7 days crossing the calendar week index
        now = LocalDateTime.parse("2024-09-14_1755", BackupService.DATE_FORMAT);
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-08_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-09_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-10_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-11_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-12_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-13_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertTrue(outdatedBackupFileInfos.isEmpty());


        // Older than 7 days, we keep only newest per week index
        now = LocalDateTime.parse("2024-09-23_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-09_1755"), // monday
                tempDir.resolve("test_store.protobuf_2024-09-10_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-11_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-12_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-13_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755") // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(6))));


        // Older than 7 days, we keep only newest per week index
        now = LocalDateTime.parse("2024-09-23_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-08_1755"), // sunday
                tempDir.resolve("test_store.protobuf_2024-09-09_1755"), // monday
                tempDir.resolve("test_store.protobuf_2024-09-10_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-11_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-12_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-13_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755") // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(7))));
        assertEquals(2, remaining.size());


        // Past 28 days, we keep only newest of each week
        now = LocalDateTime.parse("2024-09-30_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-01_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-08_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755"), // sunday
                tempDir.resolve("test_store.protobuf_2024-09-22_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // Past 29 days, we keep only newest of each week and older than 28 days for months
        now = LocalDateTime.parse("2024-09-30_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2024-09-01_1755"),// sunday
                tempDir.resolve("test_store.protobuf_2024-09-02_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-07_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-08_1755"),// sunday
                tempDir.resolve("test_store.protobuf_2024-09-09_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-14_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-15_1755"), // sunday
                tempDir.resolve("test_store.protobuf_2024-09-16_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-21_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-22_1755") // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(3))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(6))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, paths.get(9))));
        assertEquals(4, remaining.size());


        // Past 12 months, we keep the newest one per month
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2023-12-31_1755"),
                tempDir.resolve("test_store.protobuf_2024-01-01_1755"),
                tempDir.resolve("test_store.protobuf_2024-01-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-02-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-03-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-04-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-05-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-06-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-07-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-08-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-09-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-10-30_1755"),
                tempDir.resolve("test_store.protobuf_2024-11-29_1755"),
                tempDir.resolve("test_store.protobuf_2024-11-30_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(1))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(12))));


        // Past years, we keep only the newest per year if older than 1 year
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2018-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2019-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2020-11-29_1755"),
                tempDir.resolve("test_store.protobuf_2020-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2021-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2022-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2023-12-29_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(1, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(2))));


        // All mixed up
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        paths = List.of(
                tempDir.resolve("test_store.protobuf_2020-10-29_1755"), // remove, as newer exist
                tempDir.resolve("test_store.protobuf_2020-11-28_1755"), // remove, as newer exist
                tempDir.resolve("test_store.protobuf_2020-11-29_1755"), // remove, as newer exist
                tempDir.resolve("test_store.protobuf_2020-12-29_1755"), // keep
                tempDir.resolve("test_store.protobuf_2021-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2022-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2023-12-29_1755"),
                tempDir.resolve("test_store.protobuf_2024-01-01_1755"),// remove, as newer exist
                tempDir.resolve("test_store.protobuf_2024-01-30_1755"),// keep
                // past 28 days, keep the newest per week
                tempDir.resolve("test_store.protobuf_2024-12-01_1755"),  // keep

                tempDir.resolve("test_store.protobuf_2024-12-02_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-03_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-08_1755"), // keep

                tempDir.resolve("test_store.protobuf_2024-12-09_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-15_1755"),// keep

                tempDir.resolve("test_store.protobuf_2024-12-16_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-21_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-22_1755"),// keep

                // last 7 days, keep one per day
                tempDir.resolve("test_store.protobuf_2024-12-23_1750"), // we only use day not exact time to check for past 7 days. so we keep it
                tempDir.resolve("test_store.protobuf_2024-12-24_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-25_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-26_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-28_1755"),
                tempDir.resolve("test_store.protobuf_2024-12-29_1750"), //remove as 29_1755 is newer
                tempDir.resolve("test_store.protobuf_2024-12-29_1755"),

                // Last 24 hours, keep one per hour
                tempDir.resolve("test_store.protobuf_2024-12-29_1756"),
                tempDir.resolve("test_store.protobuf_2024-12-30_1555"),
                tempDir.resolve("test_store.protobuf_2024-12-30_1654"), //remove as 30_1655 is newer
                tempDir.resolve("test_store.protobuf_2024-12-30_1655"),

                // Last hour we keep all per minute
                tempDir.resolve("test_store.protobuf_2024-12-30_1656"),
                tempDir.resolve("test_store.protobuf_2024-12-30_1754"),
                tempDir.resolve("test_store.protobuf_2024-12-30_1755")
        );
        list = BackupService.createBackupFileInfo(fileName, paths);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertEquals(11, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(1))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(2))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(7))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(10))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(11))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(13))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(15))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(16))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(23))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, paths.get(27))));
    }

    @Test
    void testGetBackupsReadsDirectoryAndReturnsParsedBackups(@TempDir Path tempDir) throws IOException {
        Path dbDirPath = tempDir.resolve("db");
        Path storePath = dbDirPath.resolve("test_store.protobuf");

        List<String> fileNames = List.of(
                "test_store.protobuf_2025-12-04_0901",
                "test_store.protobuf_2024-01-01_0000",
                "other.txt",
                ".DS_Store"
        );

        Path backupPathDir = tempDir.resolve("backups").resolve("test");
        Files.createDirectories(backupPathDir);

        // create files on disk
        for (String fn : fileNames) {
            Files.writeString(backupPathDir.resolve(fn), "dummy text", StandardCharsets.UTF_8);
        }

        BackupService bs = new BackupService(tempDir, storePath, MaxBackupSize.HUNDRED_MB);
        List<BackupFileInfo> backups = bs.getBackups();

        // Only the two valid backup files should be returned, newest first
        assertEquals(2, backups.size());
        assertEquals("test_store.protobuf_2025-12-04_0901", backups.get(0).getPath().getFileName().toString());
        assertEquals("test_store.protobuf_2024-01-01_0000", backups.get(1).getPath().getFileName().toString());
    }

    @Test
    void testCreateBackupFileInfoParsesAndSorts(@TempDir Path tempDir) {
        String baseName = "test_store.protobuf";
        List<Path> paths = List.of(
                tempDir.resolve("test_store.protobuf_2025-12-04_0901"),
                tempDir.resolve("test_store.protobuf_2024-01-01_0000"),
                tempDir.resolve("not_a_backup.txt"),
                tempDir.resolve(".DS_Store")
        );

        List<BackupFileInfo> infos = BackupService.createBackupFileInfo(baseName, paths);

        // Only two valid backups expected and newest first
        assertEquals(2, infos.size());
        assertEquals("test_store.protobuf_2025-12-04_0901", infos.get(0).getPath().getFileName().toString());
        assertEquals("test_store.protobuf_2024-01-01_0000", infos.get(1).getPath().getFileName().toString());
    }

    void createBackups() throws IOException {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date());
        int writeFrequency = 60 * 60 * 24; // every hour
        int seconds = 60;
        int minutes = 60;
        int hours = 24;
        int days = 1000;
        long ts = System.currentTimeMillis();
        int numItems = seconds * minutes * hours * days / writeFrequency;
        for (int i = 0; i < numItems; i++) {
            calendar.add(Calendar.SECOND, -writeFrequency);
            Date calendarTime = calendar.getTime();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(calendarTime.toInstant(), ZoneId.systemDefault());
            Path backupFilePath = backupService.getBackupFilePath(localDateTime);
            // As we rename the storage file at backup we need to create it before backup.
            FileMutatorUtils.writeToPath("test", storeFilePath);
            backupService.backup(backupFilePath);
        }
        log.error("createBackups took {} ms", System.currentTimeMillis() - ts); // seconds * minutes * hours -> took 16275 ms
    }


    private static BackupFileInfo createBackupFileInfo(String fileName, Path path) {
        return BackupFileInfo.from(fileName, path).orElseThrow();
    }
}
