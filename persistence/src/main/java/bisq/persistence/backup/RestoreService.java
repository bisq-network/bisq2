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

import bisq.persistence.PersistableStore;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class RestoreService {

    private final List<BackupFileInfo> restoredBackupFileInfos = new CopyOnWriteArrayList<>();

    public RestoreService() {
    }

    public List<BackupFileInfo> getRestoredBackupFileInfos() {
        return List.copyOf(restoredBackupFileInfos);
    }

    public <T extends PersistableStore<T>> Optional<T> tryToRestoreFromBackup(
            List<BackupFileInfo> backupFileInfoList,
            Function<Path, Optional<T>> storeReaderFunction) {
        if (backupFileInfoList == null || backupFileInfoList.isEmpty()) {
            log.info("No backup files available for restoration.");
            return Optional.empty();
        }

        for (BackupFileInfo backupFileInfo : backupFileInfoList) {
            Path backupPath = backupFileInfo.getPath();
            log.info("Trying to restore from backup file {}", backupPath);
            try {
                Optional<T> optionalStore = storeReaderFunction.apply(backupPath);
                if (optionalStore.isPresent()) {
                    log.info("Successfully restored from backup file {}", backupPath);
                    restoredBackupFileInfos.add(backupFileInfo);
                    return optionalStore;
                }
            } catch (Exception e) {
                log.warn("Failed to read backup {}: {}", backupPath, e.getMessage(), e);
            }
        }

        return Optional.empty();
    }
}
