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

import bisq.common.file.FileUtils;
import bisq.common.platform.PlatformUtils;
import bisq.persistence.Persistence;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class BackupServiceTest {
    private BackupService backupService;
    private final Path dataDir = PlatformUtils.getUserDataDir().resolve("bisq2_BackupServiceTest");
    private File storeFile;

    @BeforeEach
    void setUp() throws IOException {
        Path dbDir = dataDir.resolve("db");
        FileUtils.makeDirs(dbDir);
        String storeFileName = "test_store" + Persistence.EXTENSION;
        Path storeFilePath = dbDir.resolve(storeFileName);
        storeFile = storeFilePath.toFile();
        backupService = new BackupService(dataDir, storeFilePath, MaxBackupSize.HUNDRED_MB);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteFileOrDirectory(dataDir);
    }


    @Test
    void testGetRelativePath2() {
        Path dataDir, storeFilePath;
        String dirPath;

        // ✅ Windows Test
        dataDir = Paths.get("C:\\Users\\bisq_user\\alice");
        storeFilePath = Paths.get(dataDir + "\\db\\private\\key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, true);
        assertEquals("\\db\\private\\key_bundle_store.protobuf", dirPath);

        // ✅ macOS Test
        dataDir = Paths.get("/Users/bisq_user/Library/Application Support/alice");
        storeFilePath = Paths.get(dataDir + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPath);

        // ✅ Unix Test
        dataDir = Paths.get("/home/bisq_user/alice");
        storeFilePath = Paths.get(dataDir + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPath);

        // ✅ Windows UNC Path Test (Server Shares)
        dataDir = Paths.get("\\\\Server\\Share\\bisq_user\\alice");
        storeFilePath = Paths.get("\\\\Server\\Share\\bisq_user\\alice\\db\\private\\key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, true);
        assertEquals("\\db\\private\\key_bundle_store.protobuf", dirPath);

        // ✅ Nested Directory Test
        dataDir = Paths.get("/home/bisq_user/alice");
        storeFilePath = Paths.get("/home/bisq_user/alice/db/subdir/private/key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, false);
        assertEquals("/db/subdir/private/key_bundle_store.protobuf", dirPath);

        // ✅ Path with Trailing Slash in dataDir
        dataDir = Paths.get("/home/bisq_user/alice/");
        storeFilePath = Paths.get("/home/bisq_user/alice/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, false);
        assertEquals("/db/private/key_bundle_store.protobuf", dirPath);

        // ✅ Special Characters in Path
        dataDir = Paths.get("C:\\Users\\bisq user\\data");
        storeFilePath = Paths.get("C:\\Users\\bisq user\\data\\db\\private\\my file_store.protobuf");
        dirPath = BackupService.getRelativePath(dataDir, storeFilePath, true);
        assertEquals("\\db\\private\\my file_store.protobuf", dirPath);

        // ❌ File Outside `dataDir` (Should Throw Exception)
        dataDir = Paths.get("/home/bisq_user/alice");
        storeFilePath = Paths.get("/home/bisq_user/other_user/db/private/key_bundle_store.protobuf");
        Path finalDataDir = dataDir;
        Path finalStoreFilePath = storeFilePath;
        assertThrows(IllegalArgumentException.class, () -> {
            BackupService.getRelativePath(finalDataDir, finalStoreFilePath, false);
        });
    }

    @Test
    void testResolveDirPath() {
        Path dataDir, storeFilePath, dirPath;

        // Windows
        dataDir = Paths.get("C:\\Users\\bisq_user\\alice");
        storeFilePath = Paths.get(dataDir + "\\db\\private\\key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDir, storeFilePath);
        assertEquals(Paths.get(dataDir + "\\backups\\private\\key_bundle"), dirPath);

        // OSX
        dataDir = Paths.get("/Users/bisq_user/Library/Application Support/alice");
        storeFilePath = Paths.get(dataDir + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDir, storeFilePath);
        assertEquals(Paths.get(dataDir + "/backups/private/key_bundle"), dirPath);

        // Unix
        dataDir = Paths.get("/home/bisq_user/alice");
        storeFilePath = Paths.get(dataDir + "/db/private/key_bundle_store.protobuf");
        dirPath = BackupService.resolveDirPath(dataDir, storeFilePath);
        assertEquals(Paths.get(dataDir + "/backups/private/key_bundle"), dirPath);
    }

    @Test
    void testBackup() throws IOException {
        FileUtils.writeToFile("test", storeFile);
        assertTrue(storeFile.exists());
        File backupFile = backupService.getBackupFile();
        assertFalse(backupFile.exists());
        backupService.backup(backupFile);
        assertTrue(backupFile.exists());
        assertFalse(storeFile.exists());
    }

    @Test
    void testPrune() {
        Predicate<BackupFileInfo> isMaxFileSizeReachedFunction = e -> false;
        List<String> fileNames;
        List<BackupFileInfo> list;
        List<BackupFileInfo> outdatedBackupFileInfos;
        List<BackupFileInfo> remaining;
        LocalDateTime now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);
        String fileName = "test_store.protobuf";


        // Empty
        fileNames = List.of();
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());

        // Backup date is in the future. We treat backup as daily backup
        fileNames = List.of(
                "test_store.protobuf_2024-09-15_1755"
        );
        now = LocalDateTime.parse("2024-09-14_1755", BackupService.DATE_FORMAT);
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());
        now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);

        // Last 2 minutes. We keep all
        fileNames = List.of(
                "test_store.protobuf_2024-09-15_1754",
                "test_store.protobuf_2024-09-15_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // After 1 hour we keep only the newest per hour
        fileNames = List.of(
                "test_store.protobuf_2024-09-15_1653",
                "test_store.protobuf_2024-09-15_1654",
                "test_store.protobuf_2024-09-15_1655", // remove as
                "test_store.protobuf_2024-09-15_1656",
                "test_store.protobuf_2024-09-15_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(1))));


        // At 24 hour we keep only the newest per day
        fileNames = List.of(
                "test_store.protobuf_2024-09-14_1754", // remove as 14_1755 is newer
                "test_store.protobuf_2024-09-14_1755", // use day map
                "test_store.protobuf_2024-09-14_1756", // remove as 14_1757 is newer
                "test_store.protobuf_2024-09-14_1757",
                "test_store.protobuf_2024-09-14_1855",
                "test_store.protobuf_2024-09-15_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(2))));


        // Different day, we keep all as in last week
        fileNames = List.of(
                "test_store.protobuf_2024-09-14_1755",
                "test_store.protobuf_2024-09-15_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(list.get(0).getFileNameWithDate(), fileNames.get(1));
        assertEquals(0, outdatedBackupFileInfos.size());


        // Same day, we keep all as in last week
        fileNames = List.of(
                "test_store.protobuf_2024-09-15_1754",
                "test_store.protobuf_2024-09-15_1755",
                "test_store.protobuf_2024-09-15_1752"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // Same day, we keep only newest as it's older than past 7 days
        fileNames = List.of(
                "test_store.protobuf_2024-09-05_1754",
                "test_store.protobuf_2024-09-05_1755",
                "test_store.protobuf_2024-09-05_1752"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(2))));


        // Past 7 days in calendar week
        now = LocalDateTime.parse("2024-09-15_1755", BackupService.DATE_FORMAT);
        fileNames = List.of(
                "test_store.protobuf_2024-09-09_1755",
                "test_store.protobuf_2024-09-10_1755",
                "test_store.protobuf_2024-09-11_1755",
                "test_store.protobuf_2024-09-12_1755",
                "test_store.protobuf_2024-09-13_1755",
                "test_store.protobuf_2024-09-14_1755",
                "test_store.protobuf_2024-09-15_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertTrue(outdatedBackupFileInfos.isEmpty());


        // Past 7 days crossing the calendar week index
        now = LocalDateTime.parse("2024-09-14_1755", BackupService.DATE_FORMAT);
        fileNames = List.of(
                "test_store.protobuf_2024-09-08_1755",
                "test_store.protobuf_2024-09-09_1755",
                "test_store.protobuf_2024-09-10_1755",
                "test_store.protobuf_2024-09-11_1755",
                "test_store.protobuf_2024-09-12_1755",
                "test_store.protobuf_2024-09-13_1755",
                "test_store.protobuf_2024-09-14_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertTrue(outdatedBackupFileInfos.isEmpty());


        // Older than 7 days, we keep only newest per week index
        now = LocalDateTime.parse("2024-09-23_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2024-09-09_1755", // monday
                "test_store.protobuf_2024-09-10_1755",
                "test_store.protobuf_2024-09-11_1755",
                "test_store.protobuf_2024-09-12_1755",
                "test_store.protobuf_2024-09-13_1755",
                "test_store.protobuf_2024-09-14_1755",
                "test_store.protobuf_2024-09-15_1755" // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(6))));


        // Older than 7 days, we keep only newest per week index
        now = LocalDateTime.parse("2024-09-23_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2024-09-08_1755", // sunday
                "test_store.protobuf_2024-09-09_1755", // monday
                "test_store.protobuf_2024-09-10_1755",
                "test_store.protobuf_2024-09-11_1755",
                "test_store.protobuf_2024-09-12_1755",
                "test_store.protobuf_2024-09-13_1755",
                "test_store.protobuf_2024-09-14_1755",
                "test_store.protobuf_2024-09-15_1755" // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(7))));
        assertEquals(2, remaining.size());


        // Past 28 days, we keep only newest of each week
        now = LocalDateTime.parse("2024-09-30_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2024-09-01_1755",
                "test_store.protobuf_2024-09-08_1755",
                "test_store.protobuf_2024-09-15_1755", // sunday
                "test_store.protobuf_2024-09-22_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(0, outdatedBackupFileInfos.size());


        // Past 29 days, we keep only newest of each week and older than 28 days for months
        now = LocalDateTime.parse("2024-09-30_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2024-09-01_1755",// sunday
                "test_store.protobuf_2024-09-02_1755",
                "test_store.protobuf_2024-09-07_1755",
                "test_store.protobuf_2024-09-08_1755",// sunday
                "test_store.protobuf_2024-09-09_1755",
                "test_store.protobuf_2024-09-14_1755",
                "test_store.protobuf_2024-09-15_1755", // sunday
                "test_store.protobuf_2024-09-16_1755",
                "test_store.protobuf_2024-09-21_1755",
                "test_store.protobuf_2024-09-22_1755" // sunday
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(6, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(3))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(6))));
        assertTrue(remaining.contains(createBackupFileInfo(fileName, fileNames.get(9))));
        assertEquals(4, remaining.size());


        // Past 12 months, we keep the newest one per month
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2023-12-31_1755",
                "test_store.protobuf_2024-01-01_1755",
                "test_store.protobuf_2024-01-30_1755",
                "test_store.protobuf_2024-02-30_1755",
                "test_store.protobuf_2024-03-30_1755",
                "test_store.protobuf_2024-04-30_1755",
                "test_store.protobuf_2024-05-30_1755",
                "test_store.protobuf_2024-06-30_1755",
                "test_store.protobuf_2024-07-30_1755",
                "test_store.protobuf_2024-08-30_1755",
                "test_store.protobuf_2024-09-30_1755",
                "test_store.protobuf_2024-10-30_1755",
                "test_store.protobuf_2024-11-29_1755",
                "test_store.protobuf_2024-11-30_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(2, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(1))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(12))));


        // Past years, we keep only the newest per year if older than 1 year
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2018-12-29_1755",
                "test_store.protobuf_2019-12-29_1755",
                "test_store.protobuf_2020-11-29_1755",
                "test_store.protobuf_2020-12-29_1755",
                "test_store.protobuf_2021-12-29_1755",
                "test_store.protobuf_2022-12-29_1755",
                "test_store.protobuf_2023-12-29_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        assertEquals(1, outdatedBackupFileInfos.size());
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(2))));


        // All mixed up
        now = LocalDateTime.parse("2024-12-30_1755", BackupService.DATE_FORMAT); // monday
        fileNames = List.of(
                "test_store.protobuf_2020-10-29_1755", // remove, as newer exist
                "test_store.protobuf_2020-11-28_1755", // remove, as newer exist
                "test_store.protobuf_2020-11-29_1755", // remove, as newer exist
                "test_store.protobuf_2020-12-29_1755", // keep
                "test_store.protobuf_2021-12-29_1755",
                "test_store.protobuf_2022-12-29_1755",
                "test_store.protobuf_2023-12-29_1755",
                "test_store.protobuf_2024-01-01_1755",// remove, as newer exist
                "test_store.protobuf_2024-01-30_1755",// keep
                // past 28 days, keep the newest per week
                "test_store.protobuf_2024-12-01_1755",  // keep

                "test_store.protobuf_2024-12-02_1755",
                "test_store.protobuf_2024-12-03_1755",
                "test_store.protobuf_2024-12-08_1755", // keep

                "test_store.protobuf_2024-12-09_1755",
                "test_store.protobuf_2024-12-15_1755",// keep

                "test_store.protobuf_2024-12-16_1755",
                "test_store.protobuf_2024-12-21_1755",
                "test_store.protobuf_2024-12-22_1755",// keep

                // last 7 days, keep one per day
                "test_store.protobuf_2024-12-23_1750", // we only use day not exact time to check for past 7 days. so we keep it
                "test_store.protobuf_2024-12-24_1755",
                "test_store.protobuf_2024-12-25_1755",
                "test_store.protobuf_2024-12-26_1755",
                "test_store.protobuf_2024-12-28_1755",
                "test_store.protobuf_2024-12-29_1750", //remove as 29_1755 is newer
                "test_store.protobuf_2024-12-29_1755",

                // Last 24 hours, keep one per hour
                "test_store.protobuf_2024-12-29_1756",
                "test_store.protobuf_2024-12-30_1555",
                "test_store.protobuf_2024-12-30_1654", //remove as 30_1655 is newer
                "test_store.protobuf_2024-12-30_1655",

                // Last hour we keep all per minute
                "test_store.protobuf_2024-12-30_1656",
                "test_store.protobuf_2024-12-30_1754",
                "test_store.protobuf_2024-12-30_1755"
        );
        list = BackupService.createBackupFileInfo(fileName, fileNames);
        outdatedBackupFileInfos = BackupService.findOutdatedBackups(new ArrayList<>(list), now, isMaxFileSizeReachedFunction);
        remaining = new ArrayList<>(list);
        remaining.removeAll(outdatedBackupFileInfos);
        assertEquals(11, outdatedBackupFileInfos.size());
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(0))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(1))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(2))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(7))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(10))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(11))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(13))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(15))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(16))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(23))));
        assertTrue(outdatedBackupFileInfos.contains(createBackupFileInfo(fileName, fileNames.get(27))));
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
            File backupFile = backupService.getBackupFile(localDateTime);
            // As we rename the storage file at backup we need to create it before backup.
            FileUtils.writeToFile("test", storeFile);
            backupService.backup(backupFile);
        }
        log.error("createBackups took {} ms", System.currentTimeMillis() - ts); // seconds * minutes * hours -> took 16275 ms
    }


    private static BackupFileInfo createBackupFileInfo(String fileName, String fileNameWithDate) {
        return BackupFileInfo.from(fileName, fileNameWithDate).orElseThrow();
    }
}
