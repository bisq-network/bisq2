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

package bisq.persistence;

import bisq.common.threading.ExecutorFactory;
import bisq.common.util.FileUtils;
import com.google.protobuf.Any;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class Persistence<T extends PersistableStore<T>> {
    public static final ExecutorService PERSISTENCE_IO_POOL = ExecutorFactory.newFixedThreadPool("Persistence-io-pool");

    private final String directory;
    @Getter
    private final String fileName;
    @Getter
    private final String storagePath;
    private final Object lock = new Object();
    private final AtomicReference<T> candidateToPersist = new AtomicReference<>();

    public Persistence(String directory, String fileName) {
        this.directory = directory;
        this.fileName = fileName;
        storagePath = directory + File.separator + fileName;
    }

    public CompletableFuture<Optional<T>> readAsync(Consumer<T> consumer) {
        return readAsync().whenComplete((result, throwable) -> result.ifPresent(consumer));
    }

    public CompletableFuture<Optional<T>> readAsync() {
        return CompletableFuture.supplyAsync(this::read, PERSISTENCE_IO_POOL);
    }

    public Optional<T> read() {
        File storageFile = new File(storagePath);
        if (!storageFile.exists()) {
            return Optional.empty();
        }
        try (FileInputStream fileInputStream = new FileInputStream(storagePath)) {
            PersistableStore<?> persistableStore;
            synchronized (lock) {
                // The data we get is of type Any
                Any any = Any.parseDelimitedFrom(fileInputStream);
                persistableStore = PersistableStore.fromAny(any);
            }
            //noinspection unchecked,rawtypes
            return (Optional) Optional.of(persistableStore);
        } catch (Throwable exception) {
            log.error("Error at read for " + storagePath, exception);
            try {
                FileUtils.backupCorruptedFile(directory, storageFile, fileName, "corruptedFilesAtRead");
            } catch (IOException e) {
                log.error("Error trying to backup corrupted file " + fileName + ": " + e.getMessage(), e);
            }
            return Optional.empty();
        }
    }


    public CompletableFuture<Boolean> persistAsync(T serializable) {
        synchronized (lock) {
            candidateToPersist.set(serializable);
        }
        return CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("Persistence.persist-" + fileName);
            return persist(candidateToPersist.get());
        }, PERSISTENCE_IO_POOL);
    }

    public boolean persist(T persistableStore) {
        synchronized (lock) {
            boolean success = false;
            File tempFile = null;
            FileOutputStream fileOutputStream = null;
            File storageFile = null;
            try {
                FileUtils.makeDirs(directory);
                // We use a temp file to not risk data corruption in case the write operation fails.
                // After write is done we rename the tempFile to our storageFile which is an atomic operation.
                tempFile = File.createTempFile("temp_" + fileName, null, new File(directory));
                FileUtils.deleteOnExit(tempFile);
                storageFile = new File(storagePath);
                fileOutputStream = new FileOutputStream(tempFile);

                // We use an Any container (byte blob) as we do not have the dependencies to the 
                // external PersistableStore implementations (at deserialization we would have an issue otherwise as
                // it requires static access).
                Any any = persistableStore.toAny();
                any.writeDelimitedTo(fileOutputStream);
                fileOutputStream.flush();
                fileOutputStream.getFD().sync();

                // close is needed on WinOS otherwise renameFile will fail
                fileOutputStream.close();
                fileOutputStream = null;

                // Atomic rename
                boolean renameSucceeded = FileUtils.renameFile(tempFile, storageFile);
                if (!renameSucceeded) {
                    // At shut down we get sometimes renameSucceeded=false. 
                    // As far I observed the temp file was never left and the storage file got updated, so it seems it's
                    // not a critical issue.
                    log.debug("Renaming of tempFile to storageFile failed. tempFile={}, storageFile={}",
                            tempFile, storageFile);
                }
                //log.debug("Persisted {}", persistableStore);
                success = true;
            } catch (IOException ex) {
                log.error("Error at read for " + storagePath + " msg: " + ex.getMessage(), ex);
                try {
                    if (storageFile != null) {
                        FileUtils.backupCorruptedFile(directory, storageFile, fileName, "corruptedFilesAtWrite");
                    }
                } catch (IOException e) {
                    log.error("FileUtils.backupCorruptedFile failed: " + e.getMessage(), e);
                }
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException ioe) {
                    log.error("Error closing stream " + ioe.getMessage(), ioe); // swallow
                }
                if (tempFile != null) {
                    FileUtils.releaseTempFile(tempFile);
                }
            }
            return success;
        }
    }
}
