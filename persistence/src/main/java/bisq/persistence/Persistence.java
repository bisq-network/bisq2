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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class Persistence<T extends Serializable> {
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
        try (FileInputStream fileInputStream = new FileInputStream(storagePath);
             ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream)) {
            Object object;
            synchronized (lock) {
                object = objectInputStream.readObject();
                log.debug("read {}", object);
            }
            //noinspection unchecked
            return Optional.of((T) object);
        } catch (Throwable exception) {
            log.error("Error at read for " + storagePath, exception);
            try {
                FileUtils.backupCorruptedFile(directory, storageFile, fileName, "corruptedFilesAtRead");
            } catch (IOException e) {
                e.printStackTrace();
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

    public boolean persist(T serializable) {
        synchronized (lock) {
            boolean success = false;
            try {
                FileUtils.makeDirs(directory);
                // We use a temp file to not risk data corruption in case the write operation fails.
                // After write is done we rename the tempFile to our storageFile which is an atomic operation.
                File tempFile = File.createTempFile("temp_" + fileName, null, new File(directory));
                FileUtils.deleteOnExit(tempFile);
                File storageFile = new File(storagePath);
                boolean tempSuccess = true;
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
                     ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
                    objectOutputStream.writeObject(serializable);
                    objectOutputStream.flush();
                    fileOutputStream.flush();
                    fileOutputStream.getFD().sync();
                    // can't set tempSuccess to true here, because the close() is coming implicitly after closing brackets.
                } catch(IOException ex) {
                    log.error("Error at read for " + storagePath+ " msg: "+ex.getMessage(), ex);
                    tempSuccess = false;
                }// need to end the catch here to have the temp file autoclose

                try {
                    if (tempSuccess) {
                        // Atomic rename
                        FileUtils.renameFile(tempFile, storageFile);
                        //log.debug("Persisted {}", serializable);
                        success = true;
                    }
                } catch (IOException exception) {
                    log.error("Error at read for " + storagePath+ " msg: "+exception.getMessage(), exception);
                    try {
                        FileUtils.backupCorruptedFile(directory, storageFile, fileName, "corruptedFilesAtWrite");
                    } catch (IOException e) {
                        log.error("FileUtils.backupCorruptedFile failed: "+e.getMessage(),e);
                    }
                } finally {
                    FileUtils.releaseTempFile(tempFile);
                }
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
            return success;
        }
    }
}
