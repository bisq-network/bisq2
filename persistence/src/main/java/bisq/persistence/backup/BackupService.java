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
import bisq.persistence.Persistence;
import com.google.common.annotations.VisibleForTesting;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * We back up the persisted data at each write operation. We append the date time format with minutes as smallest time unit.
 * Thus, multiple backups during the same minute overwrite the previous one.
 *
 * Retention based backup strategy:
 * - We keep every backup of the last hour with the smallest time unit of 1 minute
 * - If older than 1 hour and not older than 24 hours, we keep the newest backup per hour
 * - If older than 1 day and not older than 7 days, we keep the newest backup per day
 * - If older than 7 days and not older than 28 days, we keep the newest backup per calendar week
 * - If older than 28 days but not older than 1 year, we keep the newest backup per month
 * - If older than 1 year we keep the newest backup per year
 */
@Slf4j
@ToString
public class BackupService {
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");

    private final String fileName;
    final Path dirPath, storeFilePath;
    private final MaxBackupSize maxBackupSize;

    public BackupService(Path dataDir, Path storeFilePath, MaxBackupSize maxBackupSize) {
        this.storeFilePath = storeFilePath;
        this.maxBackupSize = maxBackupSize;

        fileName = storeFilePath.getFileName().toString();
        Path backupDir = Path.of(storeFilePath.toString()
                .replaceFirst("db", "backups")
                .replace(fileName, ""));
        String dirName = fileName.replace(Persistence.EXTENSION, "")
                .replace("_store", "");
        dirPath = backupDir.resolve(dirName);

        if (maxBackupSize != MaxBackupSize.ZERO) {
            prune();
        }
    }

    public void maybeMigrateLegacyBackupFile() {
        if (maxBackupSize == MaxBackupSize.ZERO) {
            return;
        }

        try {
            Path legacyBackupDir = storeFilePath.getParent().resolve("backup");
            File legacyBackupFile = legacyBackupDir.resolve(fileName).toFile();
            if (legacyBackupFile.exists()) {
                File newBackupFile = getBackupFile();
                FileUtils.renameFile(legacyBackupFile, newBackupFile);
                if (FileUtils.listFiles(legacyBackupDir).isEmpty()) {
                    FileUtils.deleteFileOrDirectory(legacyBackupDir.toFile());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean maybeBackup() {
        if (maxBackupSize == MaxBackupSize.ZERO) {
            return false;
        }
        if (!storeFilePath.toFile().exists()) {
            return false;
        }
        try {
            return backup(getBackupFile());
        } catch (IOException ex) {
            log.error("Backup failed", ex);
            return false;
        }
    }

    @VisibleForTesting
    boolean backup(File backupFile) throws IOException {
        boolean success = FileUtils.renameFile(storeFilePath.toFile(), backupFile);
        if (!success) {
            log.error("Could not rename {} to {}", storeFilePath, backupFile);
        }
        return success;
    }


    @VisibleForTesting
    void prune() {
        if (dirPath.toFile().exists()) {
            Set<String> fileNames = FileUtils.listFiles(dirPath);
            List<BackupFileInfo> backupFileInfoList = createBackupFileInfo(fileName, fileNames);
            LocalDateTime now = LocalDateTime.now();
            List<BackupFileInfo> outdatedBackupFileInfos = findOutdatedBackups(new ArrayList<>(backupFileInfoList), now);
            outdatedBackupFileInfos.forEach(backupFileInfo -> {
                try {
                    String fileNameWithDate = backupFileInfo.getFileNameWithDate();
                    FileUtils.deleteFile(dirPath.resolve(fileNameWithDate).toFile());
                    log.info("Deleted outdated backup {}", fileNameWithDate);
                } catch (Exception e) {
                    log.error("Failed to prune backups", e);
                }
            });
        }
    }

    @VisibleForTesting
    static List<BackupFileInfo> findOutdatedBackups(List<BackupFileInfo> backupFileInfoList, LocalDateTime now) {
        Map<Integer, BackupFileInfo> byMinutes = new HashMap<>();
        Map<Integer, BackupFileInfo> byHour = new HashMap<>();
        Map<Integer, BackupFileInfo> byDay = new HashMap<>();
        Map<Integer, BackupFileInfo> byWeek = new HashMap<>();
        Map<Integer, BackupFileInfo> byMonth = new HashMap<>();
        Map<Integer, BackupFileInfo> byYear = new HashMap<>();
        for (BackupFileInfo backupFileInfo : backupFileInfoList) {
            long ageInMinutes = getBackupAgeInMinutes(backupFileInfo, now);
            long ageInHours = getBackupAgeInHours(backupFileInfo, now);
            long ageInDays = getBackupAgeInDays(backupFileInfo, now);
            if (ageInMinutes < 60) {
                byMinutes.putIfAbsent(backupFileInfo.getMinutes(), backupFileInfo);
            } else if (ageInHours < 24) {
                byHour.putIfAbsent(backupFileInfo.getHour(), backupFileInfo);
            } else if (ageInDays <= 7) {
                byDay.putIfAbsent(backupFileInfo.getDay(), backupFileInfo);
            } else if (ageInDays <= 28) {
                byWeek.putIfAbsent(backupFileInfo.getWeek(), backupFileInfo);
            } else if (ageInDays <= 365) {
                byMonth.putIfAbsent(backupFileInfo.getMonth(), backupFileInfo);
            } else {
                byYear.putIfAbsent(backupFileInfo.getYear(), backupFileInfo);
            }
        }

        ArrayList<BackupFileInfo> remaining = new ArrayList<>() {{
            addAll(byMinutes.values());
            addAll(byHour.values());
            addAll(byDay.values());
            addAll(byWeek.values());
            addAll(byMonth.values());
            addAll(byYear.values());
        }};

        ArrayList<BackupFileInfo> outDated = new ArrayList<>(backupFileInfoList);
        outDated.removeAll(remaining);
        return outDated;
    }


    ////////////////////////////////////////////////////////////////////
    // Utils
    ////////////////////////////////////////////////////////////////////

    @VisibleForTesting
    File getBackupFile() throws IOException {
        return getBackupFile(LocalDateTime.now());
    }

    @VisibleForTesting
    File getBackupFile(LocalDateTime localDateTime) throws IOException {
        String formattedDate = DATE_FORMAT.format(localDateTime);
        String fileNamePath = fileName + "_" + formattedDate;
        Path backupFilePath = dirPath.resolve(fileNamePath);
        FileUtils.makeDirs(dirPath);
        return backupFilePath.toFile();
    }

    @VisibleForTesting
    static List<BackupFileInfo> createBackupFileInfo(String fileName, Collection<String> fileNames) {
        return fileNames.stream()
                .filter(fileNameWithDate -> !fileNameWithDate.equals(".DS_Store"))
                .map(fileNameWithDate -> BackupFileInfo.from(fileName, fileNameWithDate))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .toList();
    }

    private static long getBackupAgeInDays(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.DAYS.between(backupFileInfo.getLocalDate(), now);
    }

    private static long getBackupAgeInMinutes(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.MINUTES.between(backupFileInfo.getLocalDateTime(), now);
    }   private static long getBackupAgeInHours(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.HOURS.between(backupFileInfo.getLocalDateTime(), now);
    }
}
