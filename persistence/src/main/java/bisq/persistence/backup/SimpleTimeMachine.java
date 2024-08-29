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
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mimics Apple's Time Machine backup strategy:
 *
 * Hourly Backups: For the past 24 hours, Time Machine keeps hourly backups.
 * This ensures that you can restore files or the entire system to a specific hour in the past day.
 *
 * Daily Backups: After the first 24 hours, Time Machine starts to consolidate the hourly
 * backups into daily backups. It keeps one backup per day for the past month.
 *
 * Weekly Backups: Once backups are older than a month, Time Machine further reduces the
 * frequency to weekly backups. These weekly backups are kept until the backup drive is full.
 *
 * Oldest Backups: When the backup disk becomes full, Time Machine automatically deletes
 * the oldest weekly backups to make space for new ones.
 */
@Slf4j
public class SimpleTimeMachine  {
    public static final String BACKUP_DIR = "backup";
    public static final SimpleDateFormat DATE_FORMAT_SEC = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
    public static final SimpleDateFormat DATE_FORMAT_HOUR = new SimpleDateFormat("yyyy-MM-dd-HH");
    public static final SimpleDateFormat DATE_FORMAT_DAY = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATE_FORMAT_MONTH = new SimpleDateFormat("yyyy-MM");

    private long oldestBackupAge = TimeUnit.DAYS.toMillis(30);
    private long maxBackups = 100;
    private long lastBackup;

    private void transferToHourlyBackups(String backupDirAbsPath, List<String> backupDirs) {
        long now = System.currentTimeMillis();
        Date cutOffDate = new Date(now - TimeUnit.HOURS.toMillis(1));
        backupDirs.stream()
                .forEach(dirName -> {
                    Date date = getDate(dirName, now);
                    if (date.before(cutOffDate)) {
                        String timeStamp = DATE_FORMAT_HOUR.format(date);
                        Path timeStampDir = Path.of(backupDirAbsPath, timeStamp);

                       // String fileName = storeFilePath.getFileName().toString();
                      //  Path backupPath = Path.of(timeStampDir.toAbsolutePath().toString(), fileName);
                        //FileUtils.renameFile();
                    }
                });
    }


    public void backup(Path storeFilePath, Path backupFilePath) {
        Path parentDirectoryPath = storeFilePath.getParent();
        String fileName = storeFilePath.getFileName().toString();
        String fileNameDir = fileName.replace(Persistence.EXTENSION, "");
        String timeStamp = DATE_FORMAT_SEC.format(new Date());
        Path backupDir = Path.of(parentDirectoryPath.toAbsolutePath().toString(), BACKUP_DIR, fileNameDir);
        String backupDirAbsPath = backupDir.toAbsolutePath().toString();
        Path timeStampDir = Path.of(backupDirAbsPath, timeStamp);
        Path backupPath = Path.of(timeStampDir.toAbsolutePath().toString(), fileName);
        File backupFile = backupPath.toFile();
        try {
            FileUtils.makeDirs(timeStampDir);
        } catch (IOException e) {
            log.error("Failed to make backup directory {}", backupDir, e);
        }

        try {
            FileUtils.deleteFile(backupFile);
        } catch (IOException e) {
            log.error("Failed to delete backup file {}", backupFilePath, e);
        }
        try {
            boolean isSuccess = FileUtils.renameFile(storeFilePath.toFile(), backupFile);
            if (isSuccess) {
                lastBackup = System.currentTimeMillis();
            } else {
                log.error("Failed to rename backup file {}", backupFile);
            }
        } catch (IOException e) {
            log.error("Failed to rename backup file {}", backupFile, e);
        }

        List<String> backupDirs = FileUtils.listDirectories(backupDirAbsPath).stream()
                .sorted(getAgeComparator()).toList();

        transferToHourlyBackups(backupDirAbsPath, backupDirs);

        Date oldestBackupDate = new Date(System.currentTimeMillis() - oldestBackupAge);
        AtomicInteger numBackups = new AtomicInteger(backupDirs.size());
        backupDirs.forEach(dirName -> {
            findDate(dirName).ifPresent(date -> {
                if (date.before(oldestBackupDate)) {
                    try {
                        FileUtils.deleteFileOrDirectory(Path.of(backupDirAbsPath, dirName).toFile());
                        numBackups.getAndDecrement();
                        log.info("Backup file {} is older than oldest backup date {}",
                                Path.of(backupDirAbsPath, dirName).toFile(), oldestBackupDate);
                    } catch (IOException e) {
                        log.error("Cannot parse date from directory name", e);
                    }

                }
            });
        });

        if (numBackups.get() > maxBackups) {
            List<String> sortedBackups = backupDirs.stream()
                    .sorted(getAgeComparator()).toList();
            sortedBackups.stream()
                    .skip(maxBackups)
                    .forEach(dirName -> {
                        try {
                            FileUtils.deleteFileOrDirectory(Path.of(backupDirAbsPath, dirName).toFile());
                            log.info("Backup files exceed max number of backups. We delete {}",
                                    Path.of(backupDirAbsPath, dirName).toFile());
                        } catch (IOException e) {
                            log.error("Cannot parse date from directory name", e);
                        }
                    });
        }
    }

    private static Comparator<String> getAgeComparator() {
        return (o1, o2) -> getDate(o2).compareTo(getDate(o1));
    }

    private static Date getDate(String dirName) {
        return getDate(dirName, 0);
    }

    private static Date getDate(String dirName, long defaultDate) {
        return findDate(dirName).orElse(new Date(defaultDate));
    }

    private static Optional<Date> findDate(String dirName) {
        try {
            return Optional.of(DATE_FORMAT_SEC.parse(dirName));
        } catch (ParseException e) {
            log.error("Cannot parse date from directory name", e);
            return Optional.empty();
        }
    }

}
