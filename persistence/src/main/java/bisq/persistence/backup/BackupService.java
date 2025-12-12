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

import bisq.common.data.ByteUnit;
import bisq.common.file.FileMutatorUtils;
import bisq.common.file.FileReaderUtils;
import bisq.persistence.Persistence;
import com.google.common.annotations.VisibleForTesting;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
 *
 * The max number of backups is: 60 + 23 + 6 + 3 + 11 + number of years * 11. for 1 year its: 103.
 * Assuming that most data do not get recent updates each minute, we would have about 40-50 backups.
 * If the backup file is 600 bytes (Settings), it would result in 61.8 KB.
 * If it is 1MB (typical size for user_profile_store.protobuf) it would result in 40-50 MB.
 * To avoid too much growth of backups we use the MaxBackupSize and drop old backups once the limit is reached.
 * We check as well for the totalMaxBackupSize (sum of all backups of all storage files) and once reached drop backups.
 */
@Slf4j
@ToString
public class BackupService {
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
    private static final Map<String, Long> accumulatedFileSizeByStore = new ConcurrentHashMap<>();
    @Setter
    private static double totalMaxBackupSize = ByteUnit.MB.toBytes(100);

    private final String fileName;
    @VisibleForTesting
    final Path dirPath;
    private final Path storeFilePath;
    private final MaxBackupSize maxBackupSize;

    private final Map<String, Long> fileSizeByBackupFileInfo = new HashMap<>();
    private long accumulatedFileSize;

    public BackupService(Path dataDir, Path storeFilePath, MaxBackupSize maxBackupSize) {
        this.storeFilePath = storeFilePath;
        this.maxBackupSize = maxBackupSize;

        fileName = storeFilePath.getFileName().toString();
        dirPath = resolveDirPath(dataDir, storeFilePath);
    }

    public void maybeMigrateLegacyBackupFile() {
        if (maxBackupSize == MaxBackupSize.ZERO) {
            return;
        }

        try {
            Path legacyBackupDirPath = storeFilePath.getParent().resolve("backup");
            Path legacyBackupFilePath = legacyBackupDirPath.resolve(fileName);
            if (Files.exists(legacyBackupFilePath)) {
                Path newBackupFilePath = getBackupFilePath();
                FileMutatorUtils.renameFile(legacyBackupFilePath, newBackupFilePath);
                if (FileReaderUtils.listRegularFilesAsPath(legacyBackupDirPath).isEmpty()) {
                    FileMutatorUtils.deleteFileOrDirectory(legacyBackupDirPath);
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

        if (!Files.exists(storeFilePath)) {
            return false;
        }

        // If we get over half of maxBackupSize we prune
        long fileSize = updateAndGetAccumulatedFileSize();
        if (fileSize > maxBackupSize.getSizeInBytes() / 2) {
            prune();
        }

        try {
            return backup(getBackupFilePath());
        } catch (IOException ex) {
            log.error("Backup failed", ex);
            return false;
        }
    }

    @VisibleForTesting
    boolean backup(Path backupFilePath) throws IOException {
        boolean success = FileMutatorUtils.renameFile(storeFilePath, backupFilePath);
        if (!success) {
            log.error("Could not rename {} to {}", storeFilePath, backupFilePath);
        }
        return success;
    }

    public void prune() {
        if (maxBackupSize == MaxBackupSize.ZERO) {
            return;
        }

        // TODO Consider to let that run in a background thread
        accumulatedFileSize = 0;
        List<BackupFileInfo> backupFileInfoList = getBackups();
        LocalDateTime now = LocalDateTime.now();
        List<BackupFileInfo> outdatedBackupFileInfos = findOutdatedBackups(new ArrayList<>(backupFileInfoList), now, this::isMaxFileSizeReached);
        outdatedBackupFileInfos.forEach(backupFileInfo -> {
            try {
                Files.deleteIfExists(backupFileInfo.getPath());
                log.debug("Deleted outdated backup {}", backupFileInfo.getPath().getFileName());
            } catch (Exception e) {
                log.error("Failed to prune backups", e);
            }
        });
    }

    @VisibleForTesting
    static List<BackupFileInfo> findOutdatedBackups(List<BackupFileInfo> backupFileInfoList,
                                                    LocalDateTime now,
                                                    Predicate<BackupFileInfo> isMaxFileSizeReachedPredicate) {
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

            if (isMaxFileSizeReachedPredicate.test(backupFileInfo)) {
                continue;
            }

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

    private long addAndGetAccumulatedFileSize(BackupFileInfo backupFileInfo) {
        accumulatedFileSize += getFileSize(backupFileInfo);
        accumulatedFileSizeByStore.put(fileName, accumulatedFileSize);
        return accumulatedFileSize;
    }

    private long updateAndGetAccumulatedFileSize() {
        accumulatedFileSize = 0;
        getBackups().forEach(this::addAndGetAccumulatedFileSize);
        return accumulatedFileSize;
    }

    private boolean isMaxFileSizeReached(BackupFileInfo backupFileInfo) {
        accumulatedFileSize = addAndGetAccumulatedFileSize(backupFileInfo);
        long totalAccumulatedFileSize = accumulatedFileSizeByStore.values().stream().mapToLong(e -> e).sum();
        return accumulatedFileSize > maxBackupSize.getSizeInBytes() || totalAccumulatedFileSize > totalMaxBackupSize;
    }

    private long getFileSize(BackupFileInfo backupFileInfo) {
        Path path = backupFileInfo.getPath();
        String key = path.toAbsolutePath().toString();
        fileSizeByBackupFileInfo.computeIfAbsent(key, k -> {
            try {
                return Files.size(path);
            } catch (IOException e) {
                log.error("Failed to read file size of {}", path.toAbsolutePath(), e);
                return 0L;
            }
        });
        return fileSizeByBackupFileInfo.get(key);
    }

    public List<BackupFileInfo> getBackups() {
        Set<Path> paths = FileReaderUtils.listRegularFilesAsPath(dirPath);
        return createBackupFileInfo(fileName, paths);
    }

    /* --------------------------------------------------------------------- */
    // Utils
    /* --------------------------------------------------------------------- */

    @VisibleForTesting
    static Path resolveDirPath(Path dataDir, Path storeFilePath) {
        boolean isWindowsPath = dataDir.toString().contains("\\");
        String relativeStoreFilePathString = getRelativePath(dataDir, storeFilePath, isWindowsPath);
        String relativeBackupDirString = relativeStoreFilePathString
                .replaceFirst("db", "backups")
                .replace(Persistence.EXTENSION, "")
                .replace("_store", "");
        // We don't use `resolve` as we use it in unit test which need to be OS independent.
        return Path.of(dataDir.toString() + relativeBackupDirString);
    }

    @VisibleForTesting
    static String getRelativePath(Path dataDir, Path filePath, boolean isWindowsPath) {
        // We don't use File.pathSeparator as we use it in unit test which need to be OS independent.
        String normalizedDataDirString = dataDir.toString().replace("\\", "/");
        String normalizedFilePathString = filePath.toString().replace("\\", "/");

        // Ensure dataDir is a prefix of filePath
        if (!normalizedFilePathString.startsWith(normalizedDataDirString)) {
            throw new IllegalArgumentException("File path is not inside the data directory");
        }

        String normalizedRelativePath = normalizedFilePathString.substring(normalizedDataDirString.length());
        if (isWindowsPath) {
            return normalizedRelativePath.replace("/", "\\");
        } else {
            return normalizedRelativePath;
        }
    }

    @VisibleForTesting
    Path getBackupFilePath() throws IOException {
        return getBackupFilePath(LocalDateTime.now());
    }

    @VisibleForTesting
    Path getBackupFilePath(LocalDateTime localDateTime) throws IOException {
        String formattedDate = DATE_FORMAT.format(localDateTime);
        String fileNamePath = fileName + "_" + formattedDate;
        Path backupFilePath = dirPath.resolve(fileNamePath);
        FileMutatorUtils.createRestrictedDirectories(dirPath);
        return backupFilePath;
    }

    @VisibleForTesting
    static List<BackupFileInfo> createBackupFileInfo(String fileName, Collection<Path> paths) {
        List<BackupFileInfo> result = paths.stream()
                .filter(path -> !path.getFileName().toString().equals(".DS_Store"))
                .map(path -> BackupFileInfo.from(fileName, path))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .collect(Collectors.toList());
        return result;
    }

    private static long getBackupAgeInDays(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.DAYS.between(backupFileInfo.getLocalDate(), now);
    }

    private static long getBackupAgeInMinutes(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.MINUTES.between(backupFileInfo.getLocalDateTime(), now);
    }

    private static long getBackupAgeInHours(BackupFileInfo backupFileInfo, LocalDateTime now) {
        return ChronoUnit.HOURS.between(backupFileInfo.getLocalDateTime(), now);
    }
}
