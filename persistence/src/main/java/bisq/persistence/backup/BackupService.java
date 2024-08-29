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
import bisq.persistence.DbSubDirectory;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@ToString
public class BackupService {
    private static final String BACKUP_DIR = "backups";
    private static final String LAST_60_MINUTES_DIR = "last-60-minutes";
    private static final String LAST_24_HOURS_DIR = "last-24-hours";
    private static final String LAST_DAYS_DIR = "last-days";
    private final Path backupDir;
    private final Path last60MinutesDir;
    private final Path last24HoursDir;
    private final Path lastDaysDir;
    private final String storeFileName;

    public enum Priority {
        HIGH, MEDIUM, LOW, NONE
    }

    public static Priority toPriority(DbSubDirectory dbSubDirectory) {
        return switch (dbSubDirectory) {
            case NETWORK_DB -> Priority.NONE;
            case CACHE -> Priority.NONE;
            case SETTINGS -> Priority.MEDIUM;
            case PRIVATE -> Priority.HIGH;
            case WALLETS -> Priority.HIGH;
        };
    }

    private final Path storeFilePath;
    private final Priority backupPriority;

    public BackupService(Path storeFilePath, Priority backupPriority) {
        this.storeFilePath = storeFilePath;
        this.backupPriority = backupPriority;

        storeFileName = storeFilePath.getFileName().toString();
        Path dataDir = storeFilePath.getParent().getParent().getParent();
        backupDir = dataDir.resolve(BACKUP_DIR);
        last60MinutesDir = backupDir.resolve(LAST_60_MINUTES_DIR);
        last24HoursDir = backupDir.resolve(LAST_24_HOURS_DIR);
        lastDaysDir = backupDir.resolve(LAST_DAYS_DIR);

        try {
            FileUtils.makeDirs(last60MinutesDir);
            FileUtils.makeDirs(last24HoursDir);
            FileUtils.makeDirs(lastDaysDir);
        } catch (IOException e) {
            log.error("Cannot make directory at {}", this, e);
        }
    }

    public void backup() {
        if (!storeFilePath.toFile().exists()) {
            return;
        }

        /*File backupFile = backupFilePath.toFile();
        if (backupFile.exists()) {
            Files.delete(backupFilePath);
        }

        boolean isSuccess = storeFilePath.toFile().renameTo(backupFile);
        if (!isSuccess) {
            throw new IOException("Couldn't rename " + storeFilePath + " to " + backupFilePath);
        }*/
    }
}
